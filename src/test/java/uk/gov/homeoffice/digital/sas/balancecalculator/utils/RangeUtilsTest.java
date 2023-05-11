package uk.gov.homeoffice.digital.sas.balancecalculator.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.homeoffice.digital.sas.balancecalculator.utils.RangeUtils.splitOverDays;

import com.google.common.collect.Range;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.SortedMap;
import org.junit.jupiter.api.Test;

class RangeUtilsTest {

  @Test
  void splitOverDays_timeEntryWithinCalendarDay_returnOneDateTimeRange() {
    ZonedDateTime startTime = ZonedDateTime.parse("2023-04-18T08:00:00+00:00");
    ZonedDateTime endTime = ZonedDateTime.parse("2023-04-18T10:00:00+00:00");

    SortedMap<LocalDate, Range<ZonedDateTime>> ranges =
        splitOverDays(startTime, endTime);
    assertThat(ranges).hasSize(1);

    Range<ZonedDateTime> range = ranges.get(startTime.toLocalDate());

    assertThat(range.lowerEndpoint().toString()).isEqualTo("2023-04-18T09:00+01:00[Europe/London]");
    assertThat(range.upperEndpoint().toString()).isEqualTo("2023-04-18T11:00+01:00[Europe/London]");
  }

  @Test
  void splitOverDays_timeEntryWithinCalendarDayInGmt_returnOneDateTimeRange() {
    ZonedDateTime startTime = ZonedDateTime.parse("2023-01-18T08:00:00+00:00");
    ZonedDateTime endTime = ZonedDateTime.parse("2023-01-18T10:00:00+00:00");

    SortedMap<LocalDate, Range<ZonedDateTime>> ranges =
        splitOverDays(startTime, endTime);
    assertThat(ranges).hasSize(1);

    Range<ZonedDateTime> range = ranges.get(startTime.toLocalDate());

    assertThat(range.lowerEndpoint().toString()).isEqualTo("2023-01-18T08:00Z[Europe/London]");
    assertThat(range.upperEndpoint().toString()).isEqualTo("2023-01-18T10:00Z[Europe/London]");
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

  @Test
  void splitOverDays_timeEntryWithinOneCalendarDaysFinishingAtMidnight_returnOneDateTimeRange() {

    var startTime = ZonedDateTime.parse("2023-04-18T22:00:00+01:00");
    var endTimeNextDay = ZonedDateTime.parse("2023-04-19T00:00:00+01:00");

    SortedMap<LocalDate, Range<ZonedDateTime>> ranges = splitOverDays(startTime, endTimeNextDay);
    assertThat(ranges).hasSize(1);

    Range<ZonedDateTime> range1 = ranges.get(startTime.toLocalDate());

    assertThat(range1.lowerEndpoint().toString()).isEqualTo(
        "2023-04-18T22:00+01:00[Europe/London]");
    assertThat(range1.upperEndpoint().toString()).isEqualTo(
        "2023-04-19T00:00+01:00[Europe/London]");
  }
}