package uk.gov.homeoffice.digital.sas.balancecalculator;

import com.google.common.collect.Range;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.homeoffice.digital.sas.balancecalculator.client.RestClient;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Accrual;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Contributions;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.timecard.TimeEntry;

@Component
public class BalanceCalculator {

  private final RestClient restClient;

  @Autowired
  public BalanceCalculator(RestClient restClient) {
    this.restClient = restClient;
  }

  public Set<Accrual> calculate(TimeEntry timeEntry) {

    Map<LocalDate, Range<ZonedDateTime>> dateRangeMap =
        splitOverDays(timeEntry.getActualStartTime(), timeEntry.getActualEndTime());

    return dateRangeMap.entrySet().stream()
        .map(entry
            -> calculateContributionsAndUpdateAccrual(timeEntry.getId(), timeEntry.getTenantId(),
            timeEntry.getOwnerId(), entry.getKey(), entry.getValue())
    ).collect(Collectors.toSet());

  }

  private Accrual calculateContributionsAndUpdateAccrual(String timeEntryId, String tenantId,
      String personId, LocalDate accrualDate, Range<ZonedDateTime> dateTimeRange) {

    List<Accrual> accruals = restClient.getAccrualByDate(tenantId, personId, accrualDate);

    // TODO what to do if there are no accrual records ?

    BigDecimal hours = calculateDurationInHours(dateTimeRange);

    Accrual accrual = accruals.get(0);

    Contributions contributions = accrual.getContributions();
    contributions.getTimeEntries().put(UUID.fromString(timeEntryId), hours);
    BigDecimal total = contributions.getTotal().add(hours);
    contributions.setTotal(total);
    return accrual;
  }

  BigDecimal calculateDurationInHours(Range<ZonedDateTime> dateTimeRange) {
    Duration shiftDuration =
        Duration.between(dateTimeRange.lowerEndpoint(), dateTimeRange.upperEndpoint());

    long minutes = shiftDuration.toMinutes();
    // TODO: rounding?
    return new BigDecimal(minutes / 60);
  }

  Map<LocalDate, Range<ZonedDateTime>> splitOverDays(ZonedDateTime startDateTime,
      ZonedDateTime endDateTime) {
    Map<LocalDate, Range<ZonedDateTime>> intervals = new HashMap<>();
    if (startDateTime.toLocalDate().isEqual(endDateTime.toLocalDate())) {
      Range<ZonedDateTime> range = Range.closed(startDateTime, endDateTime);
      intervals.put(startDateTime.toLocalDate(), range);
    } else {
      // TODO: cover case when time entry span multiple days
      throw new UnsupportedOperationException(
          "Case when time entry spans multiple days isn't implemented yet");
    }

    return intervals;
  }
}
