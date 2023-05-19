package uk.gov.homeoffice.digital.sas.balancecalculator.handlers;

import static uk.gov.homeoffice.digital.sas.balancecalculator.utils.RangeUtils.splitOverDays;

import com.google.common.collect.Range;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
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

  public static final String ACCRUALS_MAP_EMPTY = "Accruals Map must contain at least one entry!";
  public static final String MISSING_ACCRUAL =
      "Accrual missing for tenantId {0}, personId {1}, accrual type {2} and date {3}";


  public boolean handleCreateAction(TimeEntry timeEntry,
                                    Agreement applicableAgreement,
                                    Map<AccrualType, SortedMap<LocalDate, Accrual>> allAccruals,
                                    List<AccrualModule> accrualModules) {

    SortedMap<LocalDate, Range<ZonedDateTime>> dateRangeMap =
        splitOverDays(timeEntry.getActualStartTime(), timeEntry.getActualEndTime());

    for (var entry : dateRangeMap.entrySet()) {
      LocalDate referenceDate = entry.getKey();
      ZonedDateTime startTime = entry.getValue().lowerEndpoint();
      ZonedDateTime endTime = entry.getValue().upperEndpoint();

      for (AccrualModule module : accrualModules) {

        AccrualType accrualType = module.getAccrualType();
        SortedMap<LocalDate, Accrual> accruals = allAccruals.get(accrualType);

        Accrual accrual = accruals.get(referenceDate);

        if (accrual == null) {
          log.error(MessageFormat.format(
              MISSING_ACCRUAL, timeEntry.getTenantId(), timeEntry.getOwnerId(),
              accrualType, referenceDate));
          return false;
        }

        BigDecimal shiftContribution = module.calculateShiftContribution(startTime, endTime);

        updateAccrualContribution(timeEntry.getId(), shiftContribution, accrual);

        cascadeCumulativeTotal(accruals, applicableAgreement.getStartDate());

      }
    }
    return true;
  }

  public boolean handleDeleteAction(TimeEntry timeEntry,
                                    Agreement applicableAgreement, Map<AccrualType,
              SortedMap<LocalDate, Accrual>> allAccruals,
                                    List<AccrualModule> accrualModules) {

    LocalDate timeEntryStartDate = timeEntry.getActualStartTime().toLocalDate();
    LocalDate timeEntryEndDate = timeEntry.getActualEndTime().toLocalDate();

    for (AccrualModule module : accrualModules) {
      AccrualType accrualType = module.getAccrualType();

      SortedMap<LocalDate, Accrual> accruals = allAccruals.get(accrualType);

      if (accruals == null) {
        return false;
      }

      accruals.entrySet().stream()
          .filter(key -> key.getKey().compareTo(timeEntryStartDate) >= 0
              && key.getKey().compareTo(timeEntryEndDate) <= 0)
          .forEach(accrualsMap -> {

            //TODO handle if timeEntryID can't be found

            BigDecimal timeEntryContribution =
                accrualsMap.getValue().getContributions().getTimeEntries()
                    .get(UUID.fromString(timeEntry.getId()));

            BigDecimal currentTotal = accrualsMap.getValue().getContributions().getTotal();
            accrualsMap.getValue().getContributions()
                .setTotal(currentTotal.subtract(timeEntryContribution));

            accrualsMap.getValue().getContributions().getTimeEntries()
                .remove(UUID.fromString(timeEntry.getId()));

          });

      cascadeCumulativeTotal(accruals, applicableAgreement.getStartDate());
    }

    return true;
  }

  public void updateAccrualContribution(String timeEntryId, BigDecimal shiftContribution,
                                 Accrual accrual) {

    Contributions contributions = accrual.getContributions();
    contributions.getTimeEntries().put(UUID.fromString(timeEntryId), shiftContribution);
    BigDecimal total =
        contributions.getTimeEntries().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    contributions.setTotal(total);
  }

  public void cascadeCumulativeTotal(
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
      updateSubsequentAccruals(accrualsFromReferenceDate, baseCumulativeTotal);
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

  public boolean handle(TimeEntry timeEntry, Agreement applicableAgreement,
                     Map<AccrualType, SortedMap<LocalDate, Accrual>> allAccruals,
                     List<AccrualModule> accrualModules,
                     KafkaAction action) {
    switch (action) {
      case CREATE -> handleCreateAction(
            timeEntry, applicableAgreement, allAccruals, accrualModules);

      case DELETE -> handleDeleteAction(
              timeEntry, applicableAgreement, allAccruals, accrualModules);
      case UPDATE ->
          throw new UnsupportedOperationException("NOT IMPLEMENTED YET");

      default ->
          throw new UnsupportedOperationException("UNKNOWN KAFKA EVENT ACTION");

    }
    return true;
  }
}
