package uk.gov.homeoffice.digital.sas.balancecalculator.module;

import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.enums.AccrualType;

public class NightHoursAccrualModule extends AccrualModule {
  private static final ZoneId UK_TIME_ZONE = ZoneId.of("Europe/London");

  public NightHoursAccrualModule() {
    super(AccrualType.NIGHT_HOURS);
  }

  public BigDecimal calculateShiftContribution(ZonedDateTime startTime, ZonedDateTime endTime) {
    ZonedDateTime ukStartTime = startTime.withZoneSameInstant(UK_TIME_ZONE);
    ZonedDateTime ukEndTime = endTime.withZoneSameInstant(UK_TIME_ZONE);

    ZonedDateTime midnightCurrentDay =
        ukStartTime.toLocalDate().atTime(0, 0).atZone(UK_TIME_ZONE);
    ZonedDateTime endOfPreviousDayNightHours =
        ukStartTime.toLocalDate().atTime(6, 0).atZone(UK_TIME_ZONE);
    ZonedDateTime startOfCurrentDayNightHours =
        ukStartTime.toLocalDate().atTime(23, 0).atZone(UK_TIME_ZONE);
    ZonedDateTime midnightNextDay =
        ukStartTime.toLocalDate().plusDays(1).atTime(0, 0).atZone(UK_TIME_ZONE);

    Range<ZonedDateTime> morningNightHours = Range.closed(midnightCurrentDay, endOfPreviousDayNightHours);
    Range<ZonedDateTime> eveningNightHours = Range.closed(startOfCurrentDayNightHours, midnightNextDay);
    Range<ZonedDateTime> timeEntry = Range.closed(ukStartTime, ukEndTime);

    RangeSet<ZonedDateTime> nightHoursFilter = TreeRangeSet.create();
    nightHoursFilter.add(morningNightHours);
    nightHoursFilter.add(eveningNightHours);

    RangeSet<ZonedDateTime> filteredTimeEntry = nightHoursFilter.subRangeSet(timeEntry);

    BigDecimal result = BigDecimal.ZERO;

    for (Range<ZonedDateTime> range : filteredTimeEntry.asRanges()) {
      Duration shiftDuration = Duration.between(range.lowerEndpoint(), range.upperEndpoint());
      result = result.add(new BigDecimal(shiftDuration.toMinutes()));
    }

    return result;
  }
}
