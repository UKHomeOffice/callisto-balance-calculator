package uk.gov.homeoffice.digital.sas.balancecalculator.utils;

import com.google.common.collect.Range;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.LongStream;

public class RangeUtils {

  private RangeUtils() {

  }

  private static final ZoneId UK_TIME_ZONE = ZoneId.of("Europe/London");

  public static SortedMap<LocalDate, Range<ZonedDateTime>> splitOverDays(
                                                                      ZonedDateTime startDateTime,
                                                                      ZonedDateTime endDateTime) {

    SortedMap<LocalDate, Range<ZonedDateTime>> intervals = new TreeMap<>();

    // The split should be based on UK time
    ZonedDateTime ukStartTime = startDateTime.withZoneSameInstant(UK_TIME_ZONE);
    ZonedDateTime ukEndTime = endDateTime.withZoneSameInstant(UK_TIME_ZONE);

    var numDaysCovered = getNumOfDaysCoveredByDateRange(ukStartTime, ukEndTime);
    if (numDaysCovered == 1) {
      intervals.put(ukStartTime.toLocalDate(),
          Range.closed(ukStartTime, ukEndTime));
    } else {

      intervals.put(ukStartTime.toLocalDate(), RangeUtils.startDayRange(ukStartTime));

      if (numDaysCovered > 2) {
        intervals.putAll(RangeUtils.midDayRangesMap(ukStartTime.plusDays(1),
            numDaysCovered - 1));
      }

      if (hasEndDayRange(ukEndTime)) {
        intervals.put(ukEndTime.toLocalDate(), RangeUtils.endDayRange(ukEndTime));
      }

    }

    return intervals;
  }

  private static Range<ZonedDateTime> startDayRange(ZonedDateTime startDateTime) {

    return Range.closed(
        startDateTime,
        ZonedDateTime.of(
            startDateTime.plusDays(1L).toLocalDate().atTime(0, 0),
            startDateTime.getZone()
        ));
  }

  private static Range<ZonedDateTime> endDayRange(ZonedDateTime endDateTime) {

    return Range.closed(
        ZonedDateTime.of(endDateTime.toLocalDate().atTime(0, 0),
            endDateTime.getZone()),
        endDateTime
    );
  }

  /**
   * This method return false if range is shorten than 1 min
   * (we consider ranges of at least 1 min). Otherwise, true.
   *
   * @param endDateTime ZonedDateTime
   * @return boolean
   */
  private static boolean hasEndDayRange(ZonedDateTime endDateTime) {
    return Duration.between(endDayRange(endDateTime).lowerEndpoint(),
        endDayRange(endDateTime).upperEndpoint()).toMinutes() >= 1;
  }

  private static Range<ZonedDateTime> fullDayRange(ZonedDateTime dateTime) {

    return Range.closed(
        ZonedDateTime.of(dateTime.toLocalDate().atTime(0, 0),
            dateTime.getZone()),
        ZonedDateTime.of(dateTime.plusDays(1).toLocalDate().atTime(0, 0),
            dateTime.getZone())
    );
  }

  private static Map<LocalDate, Range<ZonedDateTime>> midDayRangesMap(ZonedDateTime startDateTime,
                                                                      long numDaysCovered) {

    Map<LocalDate, Range<ZonedDateTime>> intervals = new HashMap<>();
    LongStream.range(0, numDaysCovered - 1).forEach(i ->
        intervals.put(startDateTime.plusDays(i).toLocalDate(),
            fullDayRange(startDateTime.plusDays(i)))
    );

    return intervals;
  }

  private static long getNumOfDaysCoveredByDateRange(ZonedDateTime start, ZonedDateTime end) {
    LocalDate startDate = start.toLocalDate();
    LocalDate endDate = end.toLocalDate();
    return ChronoUnit.DAYS.between(startDate, endDate) + 1;
  }

}
