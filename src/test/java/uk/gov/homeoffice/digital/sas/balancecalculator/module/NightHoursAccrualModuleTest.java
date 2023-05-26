package uk.gov.homeoffice.digital.sas.balancecalculator.module;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.homeoffice.digital.sas.balancecalculator.testutils.CommonUtils.createTimeEntry;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.timecard.TimeEntry;

class NightHoursAccrualModuleTest {

  private final NightHoursAccrualModule module = new NightHoursAccrualModule();

  private static Stream<Arguments> testData() {
    return Stream.of(
        // Outside night hours
        Arguments.of(createTimeEntry("2023-04-18T08:00:00+01:00",
                "2023-04-18T10:00:00+01:00"),
            new TreeMap<LocalDate, BigDecimal>() {{
              this.put(LocalDate.parse("2023-04-18"), BigDecimal.valueOf(0));
            }}
        ),
        Arguments.of(createTimeEntry("2023-03-18T00:00:00+00:00",
                "2023-03-18T06:00:00+00:00"),
            new TreeMap<LocalDate, BigDecimal>() {{
              this.put(LocalDate.parse("2023-03-18"), BigDecimal.valueOf(360));
            }}
        ),
        Arguments.of(createTimeEntry("2023-03-18T04:00:00+00:00",
                "2023-03-18T10:00:00+00:00"),
            new TreeMap<LocalDate, BigDecimal>() {{
              this.put(LocalDate.parse("2023-03-18"), BigDecimal.valueOf(120));
            }}
        ),
        Arguments.of(createTimeEntry("2023-03-18T14:00:00+00:00",
                "2023-03-18T23:30:00+00:00"),
            new TreeMap<LocalDate, BigDecimal>() {{
              this.put(LocalDate.parse("2023-03-18"), BigDecimal.valueOf(30));
            }}
        ),
        Arguments.of(createTimeEntry("2023-03-18T23:30:00+00:00",
                "2023-03-18T23:59:00+00:00"),
            new TreeMap<LocalDate, BigDecimal>() {{
              this.put(LocalDate.parse("2023-03-18"), BigDecimal.valueOf(29));
            }}
        ),
        Arguments.of(createTimeEntry("2023-03-18T22:30:00+00:00",
                "2023-03-19T00:00:00+00:00"),
            new TreeMap<LocalDate, BigDecimal>() {{
              this.put(LocalDate.parse("2023-03-18"), BigDecimal.valueOf(60));
            }}
        ),
        Arguments.of(createTimeEntry("2023-03-18T00:00:00+00:00",
                "2023-03-19T00:00:00+00:00"),
            new TreeMap<LocalDate, BigDecimal>() {{
              this.put(LocalDate.parse("2023-03-18"), BigDecimal.valueOf(420));
            }}
        ),
        // GMT to BST
        Arguments.of(createTimeEntry("2023-03-26T00:00:00+00:00",
                "2023-03-26T05:00:00+01:00"),
            new TreeMap<LocalDate, BigDecimal>() {{
              this.put(LocalDate.parse("2023-03-26"), BigDecimal.valueOf(240));
            }}
        ),
        // BST to GMT
        Arguments.of(createTimeEntry("2023-10-29T00:00:00+01:00",
                "2023-10-29T02:00:00+00:00"),
            new TreeMap<LocalDate, BigDecimal>() {{
              this.put(LocalDate.parse("2023-10-29"), BigDecimal.valueOf(180));
            }}
        ),
        // two day time entry
        Arguments.of(createTimeEntry("2023-11-01T20:00:00+00:00",
                "2023-11-02T10:00:00+00:00"),
            new TreeMap<LocalDate, BigDecimal>() {{
              this.put(LocalDate.parse("2023-11-01"), BigDecimal.valueOf(60));
              this.put(LocalDate.parse("2023-11-02"), BigDecimal.valueOf(360));
            }}),
        // three day time entry
        Arguments.of(createTimeEntry("2023-11-01T20:00:00+00:00",
                "2023-11-03T10:00:00+00:00"),
            new TreeMap<LocalDate, BigDecimal>() {{
              this.put(LocalDate.parse("2023-11-01"), BigDecimal.valueOf(60));
              this.put(LocalDate.parse("2023-11-02"), BigDecimal.valueOf(420));
              this.put(LocalDate.parse("2023-11-03"), BigDecimal.valueOf(360));
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