package uk.gov.homeoffice.digital.sas.balancecalculator.module;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.homeoffice.digital.sas.balancecalculator.testutils.CommonUtils.createTimeEntry;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.timecard.TimeEntry;

class AnnualTargetHoursAccrualModuleTest {

  private final AnnualTargetHoursAccrualModule module = new AnnualTargetHoursAccrualModule();

  // TODO: add multi-day time entry
  private static Stream<Arguments> testData() {
    return Stream.of(
        // Daylight saving switch
        Arguments.of(createTimeEntry(ZonedDateTime.parse("2023-03-26T00:00:00+00:00"),
                ZonedDateTime.parse("2023-03-26T05:00:00+01:00")),
            new TreeMap<LocalDate, BigDecimal>() {{
              this.put(LocalDate.parse("2023-03-26"), BigDecimal.valueOf(240));
            }}),
        Arguments.of(createTimeEntry(ZonedDateTime.parse("2023-04-18T08:00:00+01:00"),
                ZonedDateTime.parse("2023-04-18T10:00:00+01:00")),
            new TreeMap<LocalDate, BigDecimal>() {{
              this.put(LocalDate.parse("2023-04-18"), BigDecimal.valueOf(120));
            }}),
        Arguments.of(createTimeEntry(ZonedDateTime.parse("2023-10-29T00:00:00+01:00"),
                ZonedDateTime.parse("2023-10-29T02:00:00+00:00")),
            new TreeMap<LocalDate, BigDecimal>() {{
              this.put(LocalDate.parse("2023-10-29"), BigDecimal.valueOf(180));
            }})
    );
  }

  @ParameterizedTest
  @MethodSource("testData")
  void getContributions_default(TimeEntry timeEntry,
                                SortedMap<LocalDate, BigDecimal> expectedContributions) {
    SortedMap<LocalDate, BigDecimal> actualContributions = module.getContributions(timeEntry);

    assertThat(actualContributions).isEqualTo(expectedContributions);
  }
}