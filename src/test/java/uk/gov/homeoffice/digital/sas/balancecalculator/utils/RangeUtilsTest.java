package uk.gov.homeoffice.digital.sas.balancecalculator.utils;

import com.google.common.collect.Range;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class RangeUtilsTest {

  private final static ZonedDateTime DATE_TIME_MIDNIGHT_START = ZonedDateTime.parse("2023-05-03T00:00:00+00:00");
  private final static ZonedDateTime DATE_TIME_MIDNIGHT_END = ZonedDateTime.parse("2023-05-04T00:00:00+00:00");

  @Test
  void should_returnCorrectRange_when_datesWithinOneDayRange() {
    // given
    ZonedDateTime startTime = ZonedDateTime.parse("2023-05-03T08:00:00+00:00");
    ZonedDateTime finishTime = ZonedDateTime.parse("2023-05-03T10:00:00+00:00");
    // When
    Range<ZonedDateTime> range = RangeUtils.oneDayRange(startTime, finishTime);
    // Then
    assertAll(
        () -> assertThat(range.upperEndpoint()).isEqualTo(finishTime),
        () -> assertThat(range.lowerEndpoint()).isEqualTo(startTime)
    );
  }

  @Test
  void should_returnCorrectRangeStartingFromMidnight_when_onlyEndDateTimeGiven() {
    // given
    ZonedDateTime finishTime = ZonedDateTime.parse("2023-05-03T10:00:00+00:00");
    // When
    Range<ZonedDateTime> range = RangeUtils.endDayRange(finishTime);
    // Then
    assertAll(
        () -> assertThat(range.upperEndpoint()).isEqualTo(finishTime),
        () -> assertThat(range.lowerEndpoint()).isEqualTo(ZonedDateTime.parse("2023-05-03T00:00:00+00:00"))
    );
  }

  @Test
  void should_returnFullDayRange_when_dateTimeGiven() {
    // given
    ZonedDateTime date = ZonedDateTime.parse("2023-05-03T00:00:00+00:00");
    // When
    Range<ZonedDateTime> range = RangeUtils.fullDayRange(date);
    // Then
    assertAll(
        () -> assertThat(range.upperEndpoint()).isEqualTo(DATE_TIME_MIDNIGHT_END),
        () -> assertThat(range.lowerEndpoint()).isEqualTo(DATE_TIME_MIDNIGHT_START)
    );
  }

  @Test
  void should_returnCorrectFullRangesMap_when_dateTimeGiven() {
    // given
    ZonedDateTime date = ZonedDateTime.parse("2023-05-03T00:00:00+00:00");
    // When
    Map<LocalDate, Range<ZonedDateTime>> rangesMap = RangeUtils.midDayRangesMap(date, 3);

    // Then
    assertThat(rangesMap.size()).isEqualTo(2);

    Range<ZonedDateTime> range1 = rangesMap.get(LocalDate.parse("2023-05-03"));
    Range<ZonedDateTime> range2 = rangesMap.get(LocalDate.parse("2023-05-04"));

    assertAll(
        () -> assertThat(range1.upperEndpoint()).isEqualTo(ZonedDateTime.parse("2023-05-04T00:00:00+00:00")),
        () -> assertThat(range1.lowerEndpoint()).isEqualTo(ZonedDateTime.parse("2023-05-03T00:00:00+00:00")),
        () -> assertThat(range2.upperEndpoint()).isEqualTo(ZonedDateTime.parse("2023-05-05T00:00:00+00:00")),
        () -> assertThat(range2.lowerEndpoint()).isEqualTo(ZonedDateTime.parse("2023-05-04T00:00:00+00:00"))
    );
  }

}