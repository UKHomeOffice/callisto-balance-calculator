package uk.gov.homeoffice.digital.sas.balancecalculator.utils;

import com.google.common.collect.Range;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.LongStream;

public class RangeUtils {

  public static TreeMap<LocalDate, Range<ZonedDateTime>> splitOverDays(ZonedDateTime startDateTime,
                                                                       ZonedDateTime endDateTime) {

    TreeMap<LocalDate, Range<ZonedDateTime>> intervals = new TreeMap<>();

    var numDaysCovered = getNumOfDaysCoveredByDateRange(startDateTime, endDateTime);
    if (numDaysCovered == 1) {
      intervals.put(startDateTime.toLocalDate(),
          Range.closed(startDateTime, endDateTime));
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

  private static Range<ZonedDateTime> startDayRange(ZonedDateTime startDateTime) {

    return Range.closed(
        startDateTime,
        ZonedDateTime.of(
            startDateTime.plusDays(1L).toLocalDate().atTime(0, 0),
            startDateTime.getZone()
        ));
  }

  private static Range<ZonedDateTime> endDayRange(ZonedDateTime endDateTime) {

    Range<ZonedDateTime> endDayRange = Range.closed(
        ZonedDateTime.of(endDateTime.toLocalDate().atTime(0, 0),
            endDateTime.getZone()),
        endDateTime
    );
    return Duration.between(endDayRange.lowerEndpoint(),
        endDayRange.upperEndpoint()).toMinutes() >= 1
        ? endDayRange : null;
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
