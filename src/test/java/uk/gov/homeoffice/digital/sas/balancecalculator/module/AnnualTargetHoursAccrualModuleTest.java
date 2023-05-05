package uk.gov.homeoffice.digital.sas.balancecalculator.module;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class AnnualTargetHoursAccrualModuleTest {

  private final AnnualTargetHoursAccrualModule module = new AnnualTargetHoursAccrualModule();

  private static Stream<Arguments> testData() {
    return Stream.of(
        //Daylight saving switch
        Arguments.of(ZonedDateTime.parse("2023-03-26T00:00:00+00:00"),
            ZonedDateTime.parse("2023-03-26T05:00:00+01:00"),
            BigDecimal.valueOf(240)),
         Arguments.of(ZonedDateTime.parse("2023-04-18T08:00:00+01:00"),
            ZonedDateTime.parse("2023-04-18T10:00:00+01:00"),
            BigDecimal.valueOf(120))
    );
  }

  @ParameterizedTest
  @MethodSource("testData")
  void calculateShiftContribution_default(ZonedDateTime startTime, ZonedDateTime endTime
      , BigDecimal expectedShiftContribution) {
    BigDecimal minutes = module.calculateShiftContribution(startTime, endTime);

    assertThat(minutes).isEqualTo(expectedShiftContribution);
  }
}