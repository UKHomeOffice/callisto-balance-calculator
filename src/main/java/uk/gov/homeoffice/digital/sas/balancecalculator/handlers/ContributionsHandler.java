package uk.gov.homeoffice.digital.sas.balancecalculator.handlers;

import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.Constants.ACCRUALS_MAP_EMPTY;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.Constants.MISSING_ACCRUAL;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.Constants.NO_ACCRUALS_FOUND_FOR_TYPE;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Accrual;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Agreement;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Contributions;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.enums.AccrualType;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.timecard.TimeEntry;
import uk.gov.homeoffice.digital.sas.balancecalculator.module.AccrualModule;
import uk.gov.homeoffice.digital.sas.kafka.message.KafkaAction;

@Component
@Slf4j
public class ContributionsHandler {

  private final List<AccrualModule> accrualModules;

  @Autowired
  public ContributionsHandler(List<AccrualModule> accrualModules) {
    this.accrualModules = accrualModules;
  }

  public boolean handle(TimeEntry timeEntry,
                              KafkaAction action,
                              Agreement applicableAgreement,
                              Map<AccrualType, SortedMap<LocalDate, Accrual>> allAccruals) {

    for (AccrualModule module : accrualModules) {
      AccrualType accrualType = module.getAccrualType();
      SortedMap<LocalDate, Accrual> accruals = allAccruals.get(accrualType);

      if (accruals == null) {
        log.warn(MessageFormat.format(NO_ACCRUALS_FOUND_FOR_TYPE,
            accrualType, applicableAgreement.getStartDate(), applicableAgreement.getEndDate()));
        continue;
      }

      accruals.forEach((key, value) -> value.getContributions().getTimeEntries()
          .remove(UUID.fromString(timeEntry.getId())));

      SortedMap<LocalDate, BigDecimal> contributionsMap = module.getContributions(timeEntry);

      for (var entry : contributionsMap.entrySet()) {

        LocalDate accrualDate = entry.getKey();
        BigDecimal contribution = entry.getValue();
        Accrual accrual = accruals.get(accrualDate);

        if (accrual == null) {
          log.error(MessageFormat.format(
              MISSING_ACCRUAL, timeEntry.getTenantId(), timeEntry.getOwnerId(),
              accrualType, accrualDate));
          return false;
        }

        this.updateAccrualContribution(timeEntry.getId(), contribution, accrual, action);
      }

      this.cascadeCumulativeTotal(accruals, applicableAgreement.getStartDate());
    }
    return true;
  }

  void updateAccrualContribution(String timeEntryId, BigDecimal shiftContribution,
      Accrual accrual, KafkaAction action) {

    Contributions contributions = accrual.getContributions();
    Map<UUID, BigDecimal> timeEntries = contributions.getTimeEntries();

    if (!action.equals(KafkaAction.DELETE)) {
      timeEntries.put(UUID.fromString(timeEntryId), shiftContribution);
    }

    BigDecimal total = timeEntries.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    contributions.setTotal(total);
  }

  void cascadeCumulativeTotal(
      SortedMap<LocalDate, Accrual> accruals, LocalDate agreementStartDate) {

    Optional<LocalDate> optional = accruals.keySet().stream().findFirst();
    if (optional.isPresent()) {
      LocalDate priorAccrualDate = optional.get();
      Accrual priorAccrual = accruals.get(priorAccrualDate);

      BigDecimal baseCumulativeTotal = BigDecimal.ZERO;
      // if prior accrual is related to the same agreement then use its cumulative total
      // as starting point; otherwise, start at 0
      if (isPriorAccrualRelatedToTheSameAgreement(priorAccrualDate, agreementStartDate)) {
        baseCumulativeTotal = priorAccrual.getCumulativeTotal();
      }

      // the first element is only used to calculate base cumulative total so shouldn't be included
      List<Accrual> accrualsFromReferenceDate = accruals.values().stream().skip(1).toList();
      this.updateSubsequentAccruals(accrualsFromReferenceDate, baseCumulativeTotal);
    } else {
      throw new IllegalArgumentException(ACCRUALS_MAP_EMPTY);
    }
  }

  private boolean isPriorAccrualRelatedToTheSameAgreement(
      LocalDate priorAccrualDate, LocalDate agreementStatDate) {
    return !priorAccrualDate.isBefore(agreementStatDate);
  }

  void updateSubsequentAccruals(List<Accrual> accruals, BigDecimal priorCumulativeTotal) {

    //update the cumulative total for referenceDate
    accruals.get(0).setCumulativeTotal(
        priorCumulativeTotal.add(accruals.get(0).getContributions().getTotal()));

    //cascade through until end of agreement
    for (int i = 1; i < accruals.size(); i++) {
      BigDecimal priorTotal =
          accruals.get(i - 1).getCumulativeTotal();
      Accrual currentAccrual = accruals.get(i);
      currentAccrual.setCumulativeTotal(
          priorTotal.add(currentAccrual.getContributions().getTotal()));
    }
  }
}
