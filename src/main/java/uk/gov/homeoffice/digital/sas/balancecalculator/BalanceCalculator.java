package uk.gov.homeoffice.digital.sas.balancecalculator;

import com.google.common.collect.Range;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.homeoffice.digital.sas.balancecalculator.client.RestClient;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Accrual;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Agreement;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Contributions;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.enums.AccrualType;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.timecard.TimeEntry;
import uk.gov.homeoffice.digital.sas.balancecalculator.utils.RangeUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@Component
public class BalanceCalculator {

  private final RestClient restClient;

  @Autowired
  public BalanceCalculator(RestClient restClient) {
    this.restClient = restClient;
  }

  public List<Accrual> calculate(TimeEntry timeEntry) {

    List<Accrual> accrualsToBatchUpdate = Arrays.stream(AccrualType.values())
        .filter(AccrualType::isEnabled)
        .map(accrualType -> calculateAccruals(timeEntry, accrualType))
        .flatMap(Collection::stream)
        .toList();

    // TODO : send Batch Update request to Accruals API
    // restClient.batchUpdate(accrualsToBatchUpdate)

    return accrualsToBatchUpdate;
  }

  List<Accrual> calculateAccruals(TimeEntry timeEntry, AccrualType accrualType) {

    Map<LocalDate, Range<ZonedDateTime>> dateRangeMap =
        splitOverDays(timeEntry.getActualStartTime(), timeEntry.getActualEndTime());

    return dateRangeMap.entrySet().stream()
        .map(entry -> recalculateContributions(timeEntry.getId(), timeEntry.getTenantId(),
            timeEntry.getOwnerId(), accrualType, entry.getKey(), entry.getValue()))
        .map(accrual -> cascadeCumulativeTotal(timeEntry, accrual)).flatMap(Collection::stream)
        .toList();
  }

  private List<Accrual> cascadeCumulativeTotal(TimeEntry timeEntry, Accrual accrual) {
    LocalDate referenceDate = accrual.getAccrualDate();

    List<Accrual> accrualsToUpdate =
        getAccrualsFromReferenceDateUntilEndOfAgreement(timeEntry.getTenantId(),
            timeEntry.getOwnerId(), accrual.getAgreementId().toString(), referenceDate.plusDays(1));
    accrualsToUpdate.add(0, accrual);

    BigDecimal priorCumulativeTotal =
        restClient.getPriorAccrual(timeEntry.getTenantId(), timeEntry.getOwnerId(),
            accrual.getAccrualTypeId().toString(), referenceDate).getCumulativeTotal();

    return updateSubsequentAccruals(accrualsToUpdate, priorCumulativeTotal);
  }

  List<Accrual> updateSubsequentAccruals(List<Accrual> accruals, BigDecimal priorCumulativeTotal) {

    List<Accrual> updatedAccruals = List.copyOf(accruals);
    //update the cumulative total for referenceDate
    updatedAccruals.get(0).setCumulativeTotal(
        priorCumulativeTotal.add(updatedAccruals.get(0).getContributions().getTotal()));

    //cascade through until end of agreement
    for (int i = 1; i < updatedAccruals.size(); i++) {
      BigDecimal priorTotal =
          updatedAccruals.get(i - 1).getCumulativeTotal();
      Accrual currentAccrual = updatedAccruals.get(i);
      currentAccrual.setCumulativeTotal(
          priorTotal.add(currentAccrual.getContributions().getTotal()));
    }

    return updatedAccruals;
  }

  Accrual recalculateContributions(String timeEntryId, String tenantId,
      String personId, AccrualType accrualType,
      LocalDate accrualDate,
      Range<ZonedDateTime> dateTimeRange) {

    Accrual accrual =
        restClient.getAccrualByTypeAndDate(tenantId, personId,
            accrualType.getId().toString(), accrualDate);
    // TODO what to do if there are no accrual records ?
    // In other terms, is it possible that a time entry could ever be submitted while there is no
    // corresponding agreement in Accruals database?

    BigDecimal hours = calculateDurationInHours(dateTimeRange, accrualType);

    Contributions contributions = accrual.getContributions();
    contributions.getTimeEntries().put(UUID.fromString(timeEntryId), hours);
    BigDecimal total =
        contributions.getTimeEntries().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    contributions.setTotal(total);
    return accrual;
  }

  BigDecimal calculateDurationInHours(Range<ZonedDateTime> dateTimeRange, AccrualType accrualType) {

    BigDecimal hours;
    switch (accrualType) {
      case ANNUAL_TARGET_HOURS -> {
        Duration shiftDuration =
            Duration.between(dateTimeRange.lowerEndpoint(), dateTimeRange.upperEndpoint());
        // TODO: rounding?

        long minutes = shiftDuration.toMinutes();
        hours = new BigDecimal(minutes / 60);
      }
      default -> hours = BigDecimal.ZERO;
    }

    return hours;
  }

  Map<LocalDate, Range<ZonedDateTime>> splitOverDays(ZonedDateTime startDateTime,
      ZonedDateTime endDateTime) {

    Map<LocalDate, Range<ZonedDateTime>> intervals = new HashMap<>();

    var numDaysCovered = getNumOfDaysCoveredByDateRange(startDateTime, endDateTime);
    if (numDaysCovered == 1) {
      intervals.put(startDateTime.toLocalDate(), RangeUtils.oneDayRange(startDateTime, endDateTime));
    } else {

      intervals.put(startDateTime.toLocalDate(), RangeUtils.startDayRange(startDateTime));

      if (numDaysCovered > 2) {
        intervals.putAll(RangeUtils.midDayRangesMap(startDateTime.plusDays(1), numDaysCovered - 1));
      }

      if(RangeUtils.endDayRange(endDateTime) != null){
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

  //get accrual records from date to end of agreement, sorted by accrualDate
  private List<Accrual> getAccrualsFromReferenceDateUntilEndOfAgreement(String tenantId,
      String personId, String agreementId, LocalDate referenceDate) {
    Agreement agreement = restClient.getAgreementById(tenantId, agreementId);

    List<Accrual> accruals =
        restClient.getAccrualsBetweenDates(tenantId, personId, referenceDate, agreement.getEndDate()
        );

    Comparator<Accrual> accrualComparator = Comparator.comparing(Accrual::getAccrualDate);
    Collections.sort(accruals, accrualComparator);

    return accruals;
  }
}
