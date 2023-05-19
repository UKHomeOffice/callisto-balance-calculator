package uk.gov.homeoffice.digital.sas.balancecalculator.module;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class NightHoursAccrualModuleTest {

  private final NightHoursAccrualModule module = new NightHoursAccrualModule();

  private static Stream<Arguments> testData() {
    return Stream.of(
        // Outside night hours
        Arguments.of(ZonedDateTime.parse("2023-04-18T08:00:00+01:00"),
            ZonedDateTime.parse("2023-04-18T10:00:00+01:00"),
            BigDecimal.valueOf(0)),
        Arguments.of(ZonedDateTime.parse("2023-03-18T00:00:00+00:00"),
            ZonedDateTime.parse("2023-03-18T06:00:00+00:00"),
            BigDecimal.valueOf(360)),
        Arguments.of(ZonedDateTime.parse("2023-03-18T04:00:00+00:00"),
            ZonedDateTime.parse("2023-03-18T10:00:00+00:00"),
            BigDecimal.valueOf(120)),
        Arguments.of(ZonedDateTime.parse("2023-03-18T14:00:00+00:00"),
            ZonedDateTime.parse("2023-03-18T23:30:00+00:00"),
            BigDecimal.valueOf(30)),
        Arguments.of(ZonedDateTime.parse("2023-03-18T23:30:00+00:00"),
            ZonedDateTime.parse("2023-03-18T23:59:00+00:00"),
            BigDecimal.valueOf(29)),
        Arguments.of(ZonedDateTime.parse("2023-03-18T22:30:00+00:00"),
            ZonedDateTime.parse("2023-03-19T00:00:00+00:00"),
            BigDecimal.valueOf(60)),
        Arguments.of(ZonedDateTime.parse("2023-03-18T00:00:00+00:00"),
            ZonedDateTime.parse("2023-03-19T00:00:00+00:00"),
            BigDecimal.valueOf(420)),
        // GMT to BST
        Arguments.of(ZonedDateTime.parse("2023-03-26T00:00:00+00:00"),
            ZonedDateTime.parse("2023-03-26T05:00:00+01:00"),
            BigDecimal.valueOf(240)),
        // BST to GMT
        Arguments.of(ZonedDateTime.parse("2023-10-29T00:00:00+01:00"),
            ZonedDateTime.parse("2023-10-29T02:00:00+00:00"),
            BigDecimal.valueOf(180)));
  }

  @ParameterizedTest
  @MethodSource("testData")
  void calculateShiftContribution_default(ZonedDateTime startTime, ZonedDateTime endTime,
      BigDecimal expectedShiftContribution) {
    BigDecimal minutes = module.calculateShiftContribution(startTime, endTime);

    assertThat(minutes).isEqualTo(expectedShiftContribution);
  }
}