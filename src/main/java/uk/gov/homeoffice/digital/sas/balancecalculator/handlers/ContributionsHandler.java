package uk.gov.homeoffice.digital.sas.balancecalculator.handlers;

import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.Constants.ACCRUALS_MAP_EMPTY;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.Constants.MISSING_ACCRUAL;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.Constants.NO_ACCRUALS_FOUND_FOR_TYPE;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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

      LocalDate agreementStartDate = applicableAgreement.getStartDate();
      if (accruals == null) {
        log.error(MessageFormat.format(NO_ACCRUALS_FOUND_FOR_TYPE,
            accrualType, agreementStartDate, applicableAgreement.getEndDate()));
        return false;
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

      LocalDate priorDate = timeEntry.getActualStartTime().toLocalDate().minusDays(1);
      this.cascadeCumulativeTotal(accruals, priorDate, agreementStartDate);
    }
    return true;
  }

  void updateAccrualContribution(String timeEntryId,
                                 BigDecimal shiftContribution,
                                 Accrual accrual, KafkaAction action) {

    Contributions contributions = accrual.getContributions();
    Map<UUID, BigDecimal> timeEntries = contributions.getTimeEntries();

    if (!action.equals(KafkaAction.DELETE)) {
      timeEntries.put(UUID.fromString(timeEntryId), shiftContribution);
    }

    updateContributionsTotal(contributions);
  }

  private void updateContributionsTotal(Contributions contributions) {
    Map<UUID, BigDecimal> timeEntries = contributions.getTimeEntries();
    BigDecimal total = timeEntries.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    contributions.setTotal(total);
  }

  void cascadeCumulativeTotal(SortedMap<LocalDate, Accrual> accruals,
                              LocalDate priorDate,
                              LocalDate agreementStartDate) {

    if (!accruals.isEmpty()) {
      BigDecimal baseCumulativeTotal = calculateBaseCumulativeTotal(accruals, priorDate,
          agreementStartDate);

      this.updateSubsequentAccruals(accruals, priorDate, baseCumulativeTotal);
    } else {
      throw new IllegalArgumentException(ACCRUALS_MAP_EMPTY);
    }
  }

  private boolean isPriorAccrualRelatedToTheSameAgreement(LocalDate priorAccrualDate,
                                                          LocalDate agreementStartDate) {
    return !priorAccrualDate.isBefore(agreementStartDate);
  }

  void updateSubsequentAccruals(SortedMap<LocalDate, Accrual> accruals,
                                LocalDate priorDate,
                                BigDecimal priorCumulativeTotal) {

    accruals.remove(priorDate);
    List<Accrual> accrualsToBeUpdated = accruals.values().stream().toList();

    //update the cumulative total for referenceDate
    updateContributionsTotal(accrualsToBeUpdated.get(0).getContributions());
    accrualsToBeUpdated.get(0).setCumulativeTotal(
        priorCumulativeTotal.add(accrualsToBeUpdated.get(0).getContributions().getTotal()));

    //cascade through until end of agreement
    for (int i = 1; i < accrualsToBeUpdated.size(); i++) {
      BigDecimal priorTotal =
          accrualsToBeUpdated.get(i - 1).getCumulativeTotal();
      Accrual currentAccrual = accrualsToBeUpdated.get(i);

      updateContributionsTotal(currentAccrual.getContributions());

      currentAccrual.setCumulativeTotal(
          priorTotal.add(currentAccrual.getContributions().getTotal()));
    }
  }

  BigDecimal calculateBaseCumulativeTotal(SortedMap<LocalDate, Accrual> accruals,
                                          LocalDate priorDate, LocalDate agreementStartDate) {
    BigDecimal baseCumulativeTotal = BigDecimal.ZERO;

    Accrual priorAccrual = accruals.get(priorDate);

    if (priorAccrual != null && isPriorAccrualRelatedToTheSameAgreement(priorDate,
        agreementStartDate)) {
      baseCumulativeTotal = priorAccrual.getCumulativeTotal();
    }
    return baseCumulativeTotal;
  }
}
