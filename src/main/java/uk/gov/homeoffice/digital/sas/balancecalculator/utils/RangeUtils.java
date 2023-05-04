package uk.gov.homeoffice.digital.sas.balancecalculator.utils;

import com.google.common.collect.Range;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.LongStream;

public class RangeUtils {

  public static Range<ZonedDateTime> oneDayRange(ZonedDateTime startDateTime, ZonedDateTime endDateTime) {
    return Range.closed(startDateTime, endDateTime);
  }

  public static Range<ZonedDateTime> startDayRange(ZonedDateTime startDateTime) {

    return Range.closed(
        startDateTime,
        ZonedDateTime.of(
            startDateTime.plusDays(1L).toLocalDate().atTime(0, 0),
            startDateTime.getZone()
        ));
  }

  public static Range<ZonedDateTime> endDayRange(ZonedDateTime endDateTime) {

    Range<ZonedDateTime> endDayRange = Range.closed(
        ZonedDateTime.of(endDateTime.toLocalDate().atTime(0, 0), endDateTime.getZone()),
        endDateTime
        );
    return Duration.between(endDayRange.lowerEndpoint(), endDayRange.upperEndpoint()).toMinutes() >= 1
        ? endDayRange : null;
  }

  public static Range<ZonedDateTime> fullDayRange(ZonedDateTime dateTime) {

    return Range.closed(
        ZonedDateTime.of(dateTime.toLocalDate().atTime(0, 0),
            dateTime.getZone()),
        ZonedDateTime.of(dateTime.plusDays(1).toLocalDate().atTime(0, 0),
            dateTime.getZone())
    );
  }

  public static Map<LocalDate, Range<ZonedDateTime>> midDayRangesMap(ZonedDateTime startDateTime,
                                                                     long numDaysCovered){

    Map<LocalDate, Range<ZonedDateTime>> intervals = new HashMap<>();
    LongStream.range(0, numDaysCovered - 1).forEach(i -> {
      intervals.put(startDateTime.plusDays(i).toLocalDate(), fullDayRange(startDateTime.plusDays(i)));
    });

    return intervals;
  }

}
