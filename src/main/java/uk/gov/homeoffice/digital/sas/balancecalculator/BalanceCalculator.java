package uk.gov.homeoffice.digital.sas.balancecalculator;

import com.google.common.collect.Range;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.homeoffice.digital.sas.balancecalculator.client.RestClient;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Accrual;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Agreement;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Contributions;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.timecard.TimeEntry;

@Component
public class BalanceCalculator {

  private final RestClient restClient;

  @Autowired
  public BalanceCalculator(RestClient restClient) {
    this.restClient = restClient;
  }

  public List<Accrual> calculate(TimeEntry timeEntry) {

    Map<LocalDate, Range<ZonedDateTime>> dateRangeMap =
        splitOverDays(timeEntry.getActualStartTime(), timeEntry.getActualEndTime());

    List<Accrual> accruals = dateRangeMap.entrySet().stream()
        .map(entry
            -> calculateContributionsAndUpdateAccrual(timeEntry.getId(), timeEntry.getTenantId(),
            timeEntry.getOwnerId(), entry.getKey(), entry.getValue()))
        .collect(Collectors.toList());

//          List<Accrual> accrualsToUpdate = getAccrualRecordToEndOfAgreement(timeEntry,
//          a.getAgreementId());
    return updateSubsequentAccruals(timeEntry, accruals);
  }

  List<Accrual> updateSubsequentAccruals(TimeEntry timeEntry, List<Accrual> accruals) {
    //increment cumlativeTotal
    return accruals.stream().peek(a -> {
      //update new accrual cumulative total

      // check previous record first? then update cumulative total.
      a.setCumulativeTotal(a.getCumulativeTotal().add(a.getContributions().getTotal()));
      //update all other records after this new accrual

    }).collect(Collectors.toList());
  }

  Accrual calculateContributionsAndUpdateAccrual(String timeEntryId, String tenantId,
                                                 String personId, LocalDate accrualDate,
                                                 Range<ZonedDateTime> dateTimeRange) {

    List<Accrual> accruals = restClient.getAccrualsByDate(tenantId, personId, accrualDate);
    // TODO what to do if there are no accrual records ?
    Accrual accrual = accruals.get(0); // only one accrual type right now - turn into loop later

    BigDecimal hours = calculateDurationInHours(dateTimeRange);

    Contributions contributions = accrual.getContributions();
    contributions.getTimeEntries().put(UUID.fromString(timeEntryId), hours);
    BigDecimal total =
        contributions.getTimeEntries().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    contributions.setTotal(total);
    return accrual;
  }

  BigDecimal calculateDurationInHours(Range<ZonedDateTime> dateTimeRange) {
    Duration shiftDuration =
        Duration.between(dateTimeRange.lowerEndpoint(), dateTimeRange.upperEndpoint());

    // TODO: rounding?

    long minutes = shiftDuration.toMinutes();
    return new BigDecimal(minutes / 60);

  }

  Map<LocalDate, Range<ZonedDateTime>> splitOverDays(ZonedDateTime startDateTime,
                                                     ZonedDateTime endDateTime) {
    Map<LocalDate, Range<ZonedDateTime>> intervals = new HashMap<>();

    if (isOnSameDay(startDateTime, endDateTime)) {
      Range<ZonedDateTime> range = Range.closed(startDateTime, endDateTime);
      intervals.put(startDateTime.toLocalDate(), range);
    } else {
      // TODO: cover case when time entry span multiple days
      throw new UnsupportedOperationException(
          "Case when time entry spans multiple days isn't implemented yet");
    }

    return intervals;
  }

  private boolean isOnSameDay(ZonedDateTime dateTimeOne, ZonedDateTime dateTimeTwo) {
    return dateTimeOne.toLocalDate().isEqual(dateTimeTwo.toLocalDate());
  }

  //get accrual records from date to end of agreement
  private List<Accrual> getAccrualRecordToEndOfAgreement(TimeEntry timeEntry, UUID agreementId) {
    Agreement agreement = restClient.getAgreementById(timeEntry.getTenantId(),
        agreementId.toString());

    return restClient.getAllAccrualsAfterDate(agreement.getEndDate(),
        timeEntry.getActualStartTime().toLocalDate(), timeEntry.getTenantId(),
        timeEntry.getOwnerId());

  }
}
