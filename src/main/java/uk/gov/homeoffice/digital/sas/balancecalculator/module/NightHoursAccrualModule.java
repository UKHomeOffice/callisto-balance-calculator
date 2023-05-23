package uk.gov.homeoffice.digital.sas.balancecalculator.module;

import static uk.gov.homeoffice.digital.sas.balancecalculator.utils.RangeUtils.UK_TIME_ZONE;
import static uk.gov.homeoffice.digital.sas.balancecalculator.utils.RangeUtils.splitOverDays;

import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.SortedMap;
import java.util.TreeMap;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.enums.AccrualType;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.timecard.TimeEntry;

public class NightHoursAccrualModule extends AccrualModule {

  private static final int MIDNIGHT = 0;
  private static final int NIGHT_HOURS_END = 6;
  private static final int NIGHT_HOURS_START = 23;

  public NightHoursAccrualModule() {
    super(AccrualType.NIGHT_HOURS);
  }

  /**
   * Counts the night hour contributions for each day (falling within midnight to 6am
   * and 11pm to midnight).
   *
   * @param timeEntry Time Entry
   * @return night hour contributions in minutes, mapped by days covered by time entry
   */
  @Override
  public SortedMap<LocalDate, BigDecimal> getContributions(TimeEntry timeEntry) {
    SortedMap<LocalDate, BigDecimal> result = new TreeMap<>();

    SortedMap<LocalDate, Range<ZonedDateTime>> dateRanges =
        splitOverDays(timeEntry.getActualStartTime(), timeEntry.getActualEndTime());

    dateRanges.forEach((key, value) -> {
      ZonedDateTime startTime = value.lowerEndpoint();
      ZonedDateTime endTime = value.upperEndpoint();

      RangeSet<ZonedDateTime> timeEntryNightHours =
          getIntersectionWithNightHours(startTime, endTime);

      BigDecimal contribution = timeEntryNightHours.asRanges().stream()
          .map(range -> new BigDecimal(
              Duration.between(range.lowerEndpoint(), range.upperEndpoint()).toMinutes()))
          .reduce(BigDecimal.ZERO, BigDecimal::add);

      result.put(key, contribution);
    });

    return result;
  }

  private RangeSet<ZonedDateTime> getIntersectionWithNightHours(ZonedDateTime startTime,
      ZonedDateTime endTime) {

    ZonedDateTime midnightCurrentDay = atTime(startTime, MIDNIGHT);
    ZonedDateTime endOfPreviousDayNightHours = atTime(startTime, NIGHT_HOURS_END);
    ZonedDateTime startOfCurrentDayNightHours = atTime(startTime, NIGHT_HOURS_START);
    ZonedDateTime midnightNextDay = nextDayAtMidnight(startTime);

    Range<ZonedDateTime> morningNightHours =
        Range.closed(midnightCurrentDay, endOfPreviousDayNightHours);
    Range<ZonedDateTime> eveningNightHours =
        Range.closed(startOfCurrentDayNightHours, midnightNextDay);
    Range<ZonedDateTime> timeEntry = Range.closed(startTime, endTime);

    RangeSet<ZonedDateTime> nightHoursFilter = TreeRangeSet.create();
    nightHoursFilter.add(morningNightHours);
    nightHoursFilter.add(eveningNightHours);

    return nightHoursFilter.subRangeSet(timeEntry);
  }

  private ZonedDateTime atTime(ZonedDateTime date, int hour) {
    return date.toLocalDate().atTime(hour, 0).atZone(UK_TIME_ZONE);
  }

  private ZonedDateTime nextDayAtMidnight(ZonedDateTime date) {
    return date.toLocalDate().plusDays(1).atTime(MIDNIGHT, 0).atZone(UK_TIME_ZONE);
  }
}
