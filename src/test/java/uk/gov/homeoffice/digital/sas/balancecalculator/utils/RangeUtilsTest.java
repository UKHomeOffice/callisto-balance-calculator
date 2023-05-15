package uk.gov.homeoffice.digital.sas.balancecalculator.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.homeoffice.digital.sas.balancecalculator.utils.RangeUtils.splitOverDays;

import com.google.common.collect.Range;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class RangeUtilsTest {

  private static Stream<Arguments> testData() {
    return Stream.of(
        // time entry in BST
        Arguments.of("2023-04-18T08:00:00+00:00", "2023-04-18T10:00:00+00:00",
            List.of("2023-04-18T09:00+01:00[Europe/London]",
                "2023-04-18T11:00+01:00[Europe/London]")),
        // time entry in GMT
        Arguments.of("2023-01-18T08:00:00+00:00", "2023-01-18T10:00:00+00:00",
            List.of("2023-01-18T08:00Z[Europe/London]",
                "2023-01-18T10:00Z[Europe/London]")),
        // time entry crossing GMT to BST boundary
        Arguments.of("2023-03-26T00:59:00+00:00", "2023-03-26T02:00:00+01:00",
            List.of("2023-03-26T00:59Z[Europe/London]",
                "2023-03-26T02:00+01:00[Europe/London]")),
        // time entry finishes at midnight
        Arguments.of("2023-04-18T22:00:00+01:00", "2023-04-19T00:00:00+01:00",
            List.of("2023-04-18T22:00+01:00[Europe/London]",
                "2023-04-19T00:00+01:00[Europe/London]")),
        // two day time entry
        Arguments.of("2023-04-18T22:00:00+00:00", "2023-04-19T01:00:00+00:00",
            List.of("2023-04-18T23:00+01:00[Europe/London]",
                "2023-04-19T00:00+01:00[Europe/London]",
                "2023-04-19T00:00+01:00[Europe/London]",
                "2023-04-19T02:00+01:00[Europe/London]")),
        // two day time entry crossing GMT to BST
        Arguments.of("2023-03-25T22:00:00+00:00", "2023-03-26T10:00:00+00:00",
            List.of("2023-03-25T22:00Z[Europe/London]",
                "2023-03-26T00:00Z[Europe/London]",
                "2023-03-26T00:00Z[Europe/London]",
                "2023-03-26T11:00+01:00[Europe/London]")),
        // two day time entry crossing BST to GMT
        Arguments.of("2023-10-28T22:00:00+00:00", "2023-10-29T10:00:00+00:00",
            List.of("2023-10-28T23:00+01:00[Europe/London]",
                "2023-10-29T00:00+01:00[Europe/London]",
                "2023-10-29T00:00+01:00[Europe/London]",
                "2023-10-29T10:00Z[Europe/London]")),
        // two day time entry with mismatching timezones
        Arguments.of("2023-10-28T22:00:00+03:00", "2023-10-29T10:00:00+06:00",
            List.of("2023-10-28T20:00+01:00[Europe/London]",
                "2023-10-29T00:00+01:00[Europe/London]",
                "2023-10-29T00:00+01:00[Europe/London]",
                "2023-10-29T04:00Z[Europe/London]")),
        // four day time entry
        Arguments.of("2023-04-18T22:00:00+00:00", "2023-04-21T06:00:00+00:00",
            List.of("2023-04-18T23:00+01:00[Europe/London]",
                "2023-04-19T00:00+01:00[Europe/London]",
                "2023-04-19T00:00+01:00[Europe/London]",
                "2023-04-20T00:00+01:00[Europe/London]",
                "2023-04-20T00:00+01:00[Europe/London]",
                "2023-04-21T00:00+01:00[Europe/London]",
                "2023-04-21T00:00+01:00[Europe/London]",
                "2023-04-21T07:00+01:00[Europe/London]"))
    );
  }

  @ParameterizedTest
  @MethodSource("testData")
  void splitOverDays_timeEntry_returnExpectedDateRange(String timeEntryStart, String timeEntryEnd,
                                                       List<String> expectedEndpoints) {
    ZonedDateTime startTime = ZonedDateTime.parse(timeEntryStart);
    ZonedDateTime endTime = ZonedDateTime.parse(timeEntryEnd);

    SortedMap<LocalDate, Range<ZonedDateTime>> ranges =
        splitOverDays(startTime, endTime);

    // upper and lower endpoints for each range
    int numberOfEndpoints = ranges.size() * 2;
    assertThat(numberOfEndpoints).isEqualTo(expectedEndpoints.size());

    List<LocalDate> keys = new ArrayList<>(ranges.keySet());

    for (int i = 0; i < ranges.size(); i++) {
      LocalDate date = keys.get(i);
      assertThat(ranges.get(date).lowerEndpoint().toString()).isEqualTo(
          expectedEndpoints.get(i * 2));
      assertThat(ranges.get(date).upperEndpoint().toString()).isEqualTo(
          expectedEndpoints.get((i * 2) + 1));
    }
  }

  @Test
  void splitOverDays_timeEntryWithinCalendarDayCrossingGmtToBst_returnOneDateTimeRange() {
    ZonedDateTime startTime = ZonedDateTime.parse("2023-03-26T01:00:00+00:00");
    ZonedDateTime endTime = ZonedDateTime.parse("2023-03-26T02:00:00+01:00");

    SortedMap<LocalDate, Range<ZonedDateTime>> ranges =
        splitOverDays(startTime, endTime);
    assertThat(ranges).hasSize(0);
  }
}