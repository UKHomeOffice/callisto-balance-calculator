package uk.gov.homeoffice.digital.sas.balancecalculator.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.homeoffice.digital.sas.balancecalculator.utils.RangeUtils.splitOverDays;

import com.google.common.collect.Range;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.SortedMap;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class RangeUtilsTest {

  private static Stream<Arguments> singleDayTestData() {
    return Stream.of(
        // time entry in BST
        Arguments.of("2023-04-18T08:00:00+00:00", "2023-04-18T10:00:00+00:00", 1,
            List.of("2023-04-18T09:00+01:00[Europe/London]",
                "2023-04-18T11:00+01:00[Europe/London]")),
        // time entry in GMT
        Arguments.of("2023-01-18T08:00:00+00:00", "2023-01-18T10:00:00+00:00", 1,
            List.of("2023-01-18T08:00Z[Europe/London]",
                "2023-01-18T10:00Z[Europe/London]")),
        // time entry crossing GMT to BST boundary
        Arguments.of("2023-03-26T00:59:00+00:00", "2023-03-26T02:00:00+01:00", 1,
            List.of("2023-03-26T00:59Z[Europe/London]",
                "2023-03-26T02:00+01:00[Europe/London]")),
        // time entry finishes at midnight
        Arguments.of("2023-04-18T22:00:00+01:00", "2023-04-19T00:00:00+01:00", 1,
            List.of("2023-04-18T22:00+01:00[Europe/London]",
                "2023-04-19T00:00+01:00[Europe/London]"))
    );
  }

  @ParameterizedTest
  @MethodSource("singleDayTestData")
  void splitOverDays_timeEntry_returnDateRange(String start, String end, int expectedRanges,
                                               List<String> result) {
    ZonedDateTime startTime = ZonedDateTime.parse(start);
    ZonedDateTime endTime = ZonedDateTime.parse(end);

    SortedMap<LocalDate, Range<ZonedDateTime>> ranges =
        splitOverDays(startTime, endTime);

    assertThat(ranges).hasSize(expectedRanges);
    Range<ZonedDateTime> range = ranges.get(startTime.toLocalDate());

    for (int i = 0; i < expectedRanges * 2; i += 2) {
      assertThat(range.lowerEndpoint().toString()).isEqualTo(result.get(i));
      assertThat(range.upperEndpoint().toString()).isEqualTo(result.get(i + 1));
    }
  }

  @Test
  void splitOverDays_timeEntryWithinTwoCalendarDays_returnTwoDateTimeRanges() {

    var startTime = ZonedDateTime.parse("2023-04-18T22:00:00+00:00");
    var endTimeNextDay = ZonedDateTime.parse("2023-04-19T01:00:00+00:00");

    SortedMap<LocalDate, Range<ZonedDateTime>> ranges = splitOverDays(startTime, endTimeNextDay);
    assertThat(ranges).hasSize(2);

    Range<ZonedDateTime> range1 = ranges.get(startTime.toLocalDate());
    Range<ZonedDateTime> range2 = ranges.get(endTimeNextDay.toLocalDate());

    assertThat(range1.lowerEndpoint().toString()).isEqualTo(
        "2023-04-18T23:00+01:00[Europe/London]");
    assertThat(range1.upperEndpoint().toString()).isEqualTo(
        "2023-04-19T00:00+01:00[Europe/London]");

    assertThat(range2.lowerEndpoint().toString()).isEqualTo(
        "2023-04-19T00:00+01:00[Europe/London]");
    assertThat(range2.upperEndpoint().toString()).isEqualTo(
        "2023-04-19T02:00+01:00[Europe/London]");
  }

  @Test
  void splitOverDays_timeEntryWithinTwoCalendarDaysCrossingGmtToBst_returnTwoDateTimeRanges() {
    ZonedDateTime startTime = ZonedDateTime.parse("2023-03-25T22:00:00+00:00");
    ZonedDateTime endTime = ZonedDateTime.parse("2023-03-26T10:00:00+00:00");

    SortedMap<LocalDate, Range<ZonedDateTime>> ranges =
        splitOverDays(startTime, endTime);
    assertThat(ranges).hasSize(2);

    Range<ZonedDateTime> range1 = ranges.get(startTime.toLocalDate());
    Range<ZonedDateTime> range2 = ranges.get(endTime.toLocalDate());

    assertThat(range1.lowerEndpoint().toString()).isEqualTo("2023-03-25T22:00Z[Europe/London]");
    assertThat(range1.upperEndpoint().toString()).isEqualTo("2023-03-26T00:00Z[Europe/London]");

    assertThat(range2.lowerEndpoint().toString()).isEqualTo("2023-03-26T00:00Z[Europe/London]");
    assertThat(range2.upperEndpoint().toString()).isEqualTo(
        "2023-03-26T11:00+01:00[Europe/London]");
  }

  @Test
  void splitOverDays_timeEntryWithinCalendarDayCrossingGmtToBst_returnOneDateTimeRange() {
    ZonedDateTime startTime = ZonedDateTime.parse("2023-03-26T01:00:00+00:00");
    ZonedDateTime endTime = ZonedDateTime.parse("2023-03-26T02:00:00+01:00");

    SortedMap<LocalDate, Range<ZonedDateTime>> ranges =
        splitOverDays(startTime, endTime);
    assertThat(ranges).hasSize(0);
  }

  @Test
  void splitOverDays_timeEntryWithinTwoCalendarDaysCrossingBstToGmt_returnTwoDateTimeRanges() {
    ZonedDateTime startTime = ZonedDateTime.parse("2023-10-28T22:00:00+00:00");
    ZonedDateTime endTime = ZonedDateTime.parse("2023-10-29T10:00:00+00:00");

    SortedMap<LocalDate, Range<ZonedDateTime>> ranges =
        splitOverDays(startTime, endTime);
    assertThat(ranges).hasSize(2);

    Range<ZonedDateTime> range1 = ranges.get(startTime.toLocalDate());
    Range<ZonedDateTime> range2 = ranges.get(endTime.toLocalDate());

    assertThat(range1.lowerEndpoint().toString()).isEqualTo(
        "2023-10-28T23:00+01:00[Europe/London]");
    assertThat(range1.upperEndpoint().toString()).isEqualTo(
        "2023-10-29T00:00+01:00[Europe/London]");

    assertThat(range2.lowerEndpoint().toString()).isEqualTo(
        "2023-10-29T00:00+01:00[Europe/London]");
    assertThat(range2.upperEndpoint().toString()).isEqualTo("2023-10-29T10:00Z[Europe/London]");
  }

  @Test
  void splitOverDays_timeEntryWithinTwoCalendarDaysMismatchingTimeZones_returnTwoDateTimeRanges() {
    ZonedDateTime startTime = ZonedDateTime.parse("2023-10-28T22:00:00+03:00");
    ZonedDateTime endTime = ZonedDateTime.parse("2023-10-29T10:00:00+06:00");

    SortedMap<LocalDate, Range<ZonedDateTime>> ranges =
        splitOverDays(startTime, endTime);
    assertThat(ranges).hasSize(2);

    Range<ZonedDateTime> range1 = ranges.get(startTime.toLocalDate());
    Range<ZonedDateTime> range2 = ranges.get(endTime.toLocalDate());

    assertThat(range1.lowerEndpoint().toString()).isEqualTo(
        "2023-10-28T20:00+01:00[Europe/London]");
    assertThat(range1.upperEndpoint().toString()).isEqualTo(
        "2023-10-29T00:00+01:00[Europe/London]");

    assertThat(range2.lowerEndpoint().toString()).isEqualTo(
        "2023-10-29T00:00+01:00[Europe/London]");
    assertThat(range2.upperEndpoint().toString()).isEqualTo("2023-10-29T04:00Z[Europe/London]");
  }

  @Test
  void splitOverDays_timeEntryWithinFourCalendarDays_returnFourDateTimeRanges() {

    var startTime = ZonedDateTime.parse("2023-04-18T22:00:00+00:00");
    var endTime = ZonedDateTime.parse("2023-04-21T06:00:00+00:00");

    SortedMap<LocalDate, Range<ZonedDateTime>> ranges = splitOverDays(startTime, endTime);
    assertThat(ranges).hasSize(4);

    Range<ZonedDateTime> range1 = ranges.get(startTime.toLocalDate());
    Range<ZonedDateTime> range2 = ranges.get(startTime.plusDays(1).toLocalDate());
    Range<ZonedDateTime> range3 = ranges.get(startTime.plusDays(2).toLocalDate());
    Range<ZonedDateTime> range4 = ranges.get(endTime.toLocalDate());

    assertThat(range1.lowerEndpoint().toString()).isEqualTo(
        "2023-04-18T23:00+01:00[Europe/London]");
    assertThat(range1.upperEndpoint().toString()).isEqualTo(
        "2023-04-19T00:00+01:00[Europe/London]");

    assertThat(range2.lowerEndpoint().toString()).isEqualTo(
        "2023-04-19T00:00+01:00[Europe/London]");
    assertThat(range2.upperEndpoint().toString()).isEqualTo(
        "2023-04-20T00:00+01:00[Europe/London]");

    assertThat(range3.lowerEndpoint().toString()).isEqualTo(
        "2023-04-20T00:00+01:00[Europe/London]");
    assertThat(range3.upperEndpoint().toString()).isEqualTo(
        "2023-04-21T00:00+01:00[Europe/London]");

    assertThat(range4.lowerEndpoint().toString()).isEqualTo(
        "2023-04-21T00:00+01:00[Europe/London]");
    assertThat(range4.upperEndpoint().toString()).isEqualTo(
        "2023-04-21T07:00+01:00[Europe/London]");
  }
}