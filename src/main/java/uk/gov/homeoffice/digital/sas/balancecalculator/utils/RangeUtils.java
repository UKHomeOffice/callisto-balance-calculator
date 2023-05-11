package uk.gov.homeoffice.digital.sas.balancecalculator.utils;

import com.google.common.collect.Range;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.SortedMap;
import java.util.TreeMap;

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

    ZonedDateTime lowerEnd = ukStartTime;
    ZonedDateTime upperEnd =
            lowerEnd.toLocalDate().plusDays(1).atTime(0, 0).atZone(UK_TIME_ZONE);

    while (upperEnd.isBefore(ukEndTime)) {
      intervals.put(lowerEnd.toLocalDate(), Range.closed(lowerEnd, upperEnd));
      lowerEnd = upperEnd;
      upperEnd = lowerEnd.toLocalDate().plusDays(1).atTime(0, 0).atZone(UK_TIME_ZONE);
    }

    intervals.put(lowerEnd.toLocalDate(), Range.closed(lowerEnd, ukEndTime));

    return intervals;
  }
}
