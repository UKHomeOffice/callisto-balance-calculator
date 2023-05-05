package uk.gov.homeoffice.digital.sas.balancecalculator;

import static org.springframework.util.CollectionUtils.isEmpty;

import com.google.common.collect.Range;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import uk.gov.homeoffice.digital.sas.balancecalculator.client.RestClient;
import uk.gov.homeoffice.digital.sas.balancecalculator.configuration.AccrualModuleConfig;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Accrual;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Agreement;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Contributions;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.enums.AccrualType;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.timecard.TimeEntry;
import uk.gov.homeoffice.digital.sas.balancecalculator.module.AccrualModule;
import uk.gov.homeoffice.digital.sas.balancecalculator.utils.RangeUtils;

@Component
@Slf4j
@Import(AccrualModuleConfig.class)
public class BalanceCalculator {

  private static final String AGREEMENT_NOT_FOUND =
      "Agreement record not found for tenantId {0}, personId {1} and date {2}";
  private static final String ACCRUALS_NOT_FOUND =
      "No Accrual records found for tenantId {0} and personId {1} between {2} and {3}";
  private static final String MISSING_ACCRUAL =
      "Accrual missing for tenantId {0}, personId {1}, accrual type {2} and date {3}";

  private final RestClient restClient;
  private final List<AccrualModule> accrualModules;

  @Autowired
  public BalanceCalculator(RestClient restClient, List<AccrualModule> accrualModules) {
    this.restClient = restClient;
    this.accrualModules = accrualModules;
  }

  public List<Accrual> calculate(TimeEntry timeEntry) {

    String timeEntryId = timeEntry.getId();
    String tenantId = timeEntry.getTenantId();
    String personId = timeEntry.getOwnerId();
    ZonedDateTime timeEntryStart = timeEntry.getActualStartTime();
    ZonedDateTime timeEntryEnd = timeEntry.getActualEndTime();
    LocalDate timeEntryStartDate = timeEntryStart.toLocalDate();
    LocalDate timeEntryEndDate = timeEntryEnd.toLocalDate();

    // Get agreement applicable to the end date of the time entry (in case the time entry spans two
    // agreements
    Agreement applicableAgreement =
        getAgreementApplicableToTimeEntryEndDate(tenantId, personId, timeEntryEndDate);
    if (applicableAgreement == null) {
      log.warn(MessageFormat.format(AGREEMENT_NOT_FOUND, tenantId, personId, timeEntryEndDate));
      return List.of();
    }

    // Get accruals of all types between the day just before the time entry and the end date of the
    // latest applicable agreement
    Map<AccrualType, TreeMap<LocalDate, Accrual>> allAccruals =
        getAccrualsBetweenDates(tenantId, personId,
            timeEntryStartDate.minusDays(1), applicableAgreement.getEndDate());

    if (isEmpty(allAccruals)) {
      log.warn(MessageFormat.format(ACCRUALS_NOT_FOUND, tenantId, personId,
          timeEntryStartDate.minusDays(1), applicableAgreement.getEndDate()));
      return List.of();
    }

    TreeMap<LocalDate, Range<ZonedDateTime>> dateRangeMap = 
        splitOverDays(timeEntryStart, timeEntryEnd);

    for (var entry : dateRangeMap.entrySet()) {
      for (var module : accrualModules) {
        LocalDate referenceDate = entry.getKey();
        ZonedDateTime startTime = entry.getValue().lowerEndpoint();
        ZonedDateTime endTime = entry.getValue().upperEndpoint();
        AccrualType accrualType = module.getAccrualType();
        TreeMap<LocalDate, Accrual> accruals = allAccruals.get(accrualType);

        Accrual accrual = accruals.get(referenceDate);

        if (accrual == null) {
          log.error(MessageFormat.format(
              MISSING_ACCRUAL, tenantId, personId, accrualType, referenceDate));
          return List.of();
        }

        BigDecimal shiftContribution = module.calculateShiftContribution(startTime, endTime);

        updateAccrualContribution(timeEntryId, shiftContribution, accrual);

        cascadeCumulativeTotal(accruals, applicableAgreement.getStartDate());
      }
    }

    // Each AccrualType within allAccruals map still containing entry for prior day
    // which shouldn't be sent to batch update. Lines below removing that entry from the Map
    for (Map.Entry<AccrualType, TreeMap<LocalDate, Accrual>> entry : allAccruals.entrySet()) {
      TreeMap<LocalDate, Accrual> value = entry.getValue();
      value.remove(value.firstKey());
    }

    List<Accrual> accrualsToBatchUpdate = allAccruals.values().stream()
        .map(Map::values)
        .flatMap(Collection::stream)
        .toList();

    // TODO : send Batch Update request to Accruals API
    // restClient.batchUpdate(accrualsToBatchUpdate)

    return accrualsToBatchUpdate;
  }

  void updateAccrualContribution(String timeEntryId, BigDecimal shiftContribution,
      Accrual accrual) {

    Contributions contributions = accrual.getContributions();
    contributions.getTimeEntries().put(UUID.fromString(timeEntryId), shiftContribution);
    BigDecimal total =
        contributions.getTimeEntries().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    contributions.setTotal(total);
  }

  void cascadeCumulativeTotal(
      TreeMap<LocalDate, Accrual> accruals, LocalDate agreementStartDate) {

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
      // in update
      // Decided to remove it as we are loosing 1 day with multiple day TimeEntry. 
      // Using skip(1) [check below]
      //accruals.remove(priorAccrualDate); 

      // Using skip(1) as accruals contain prior day which shouldn't 
      // be updated by updateSubsequentAccruals method
      updateSubsequentAccruals(accruals.values().stream().skip(1).toList(), baseCumulativeTotal);
    } else {
      throw new IllegalArgumentException("Accruals Map must contain at least one entry!");
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

  Agreement getAgreementApplicableToTimeEntryEndDate(String tenantId, String personId,
      LocalDate timeEntryEndDate) {
    return restClient.getApplicableAgreement(tenantId,
        personId, timeEntryEndDate);
  }

  Map<AccrualType, TreeMap<LocalDate, Accrual>> getAccrualsBetweenDates(
      String tenantId, String personId, LocalDate startDate, LocalDate endDate) {

    List<Accrual> accruals = restClient.getAccrualsBetweenDates(tenantId, personId,
        startDate, endDate);

    return map(accruals);
  }

  /**
   * Groups input list of accruals by Accrual Type then by Accrual Date. Note the use of TreeMap in
   * the nested map to ensure accrual records are sorted by date
   *
   * @param accruals list of accruals
   * @return Accruals mapped by Accrual Type and Accrual Date
   */
  Map<AccrualType, TreeMap<LocalDate, Accrual>> map(List<Accrual> accruals) {
    return accruals.stream()
        .collect(Collectors.groupingBy(
                Accrual::getAccrualType,
                Collectors.toMap(
                    Accrual::getAccrualDate,
                    Function.identity(),
                    (c1, c2) -> c1, TreeMap::new)
            )
        );
  }

  TreeMap<LocalDate, Range<ZonedDateTime>> splitOverDays(ZonedDateTime startDateTime,
      ZonedDateTime endDateTime) {

    TreeMap<LocalDate, Range<ZonedDateTime>> intervals = new TreeMap<>();

    var numDaysCovered = getNumOfDaysCoveredByDateRange(startDateTime, endDateTime);
    if (numDaysCovered == 1) {
      intervals.put(startDateTime.toLocalDate(), 
          RangeUtils.oneDayRange(startDateTime, endDateTime));
    } else {

      intervals.put(startDateTime.toLocalDate(), RangeUtils.startDayRange(startDateTime));

      if (numDaysCovered > 2) {
        intervals.putAll(RangeUtils.midDayRangesMap(startDateTime.plusDays(1), 
            numDaysCovered - 1));
      }

      if (RangeUtils.endDayRange(endDateTime) != null) {
        intervals.put(endDateTime.toLocalDate(), RangeUtils.endDayRange(endDateTime));
      }
    }

    return intervals;
  }

  private long getNumOfDaysCoveredByDateRange(ZonedDateTime start, ZonedDateTime end) {
    LocalDate startDate = start.toLocalDate();
    LocalDate endDate = end.toLocalDate();
    return ChronoUnit.DAYS.between(startDate, endDate) + 1;
  }
}
