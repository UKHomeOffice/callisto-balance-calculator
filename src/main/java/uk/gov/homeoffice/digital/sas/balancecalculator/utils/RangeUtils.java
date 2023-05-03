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
        ZonedDateTime.of(
            endDateTime.toLocalDate().atTime(0, 0),
            endDateTime.getZone()),
        endDateTime
        );
    return Duration.between(endDayRange.lowerEndpoint(), endDayRange.upperEndpoint()).toMinutes() >= 1
        ? endDayRange : null;
  }

  public static Map<LocalDate, Range<ZonedDateTime>> midDayRangesMap(ZonedDateTime startDateTime,
                                                                     long numDaysCovered){
    Map<LocalDate, Range<ZonedDateTime>> intervals = new HashMap<>();

    LongStream.range(1L, numDaysCovered - 1).forEach(i -> {
      Range<ZonedDateTime> midRange = Range.closed(
          ZonedDateTime.of(startDateTime.plusDays(i).toLocalDate().atTime(0, 0),
              startDateTime.getZone()),
          ZonedDateTime.of(startDateTime.plusDays(i + 1L).toLocalDate().atTime(0, 0),
              startDateTime.getZone())
      );
      intervals.put(startDateTime.plusDays(i).toLocalDate(), midRange);
    });

    return intervals;
  }

}
