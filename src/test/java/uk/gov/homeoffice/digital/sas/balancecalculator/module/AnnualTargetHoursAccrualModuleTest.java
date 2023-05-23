package uk.gov.homeoffice.digital.sas.balancecalculator.module;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.Constants.ACCRUALS_MAP_EMPTY;
import static uk.gov.homeoffice.digital.sas.balancecalculator.testutils.CommonUtils.createTimeEntry;
import static uk.gov.homeoffice.digital.sas.balancecalculator.testutils.CommonUtils.loadAccrualsFromFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Accrual;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.timecard.TimeEntry;

class AnnualTargetHoursAccrualModuleTest {

  private final AnnualTargetHoursAccrualModule module = new AnnualTargetHoursAccrualModule();

  // TODO: ad multi-day time entry
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

  @Test
  void cascadeCumulativeTotal_priorDateWithinSameAgreement_usePriorCumulativeTotalAsBasis()
      throws IOException {
    List<Accrual> accruals = loadAccrualsFromFile("data/accruals_annualTargetHours.json");

    SortedMap<LocalDate, Accrual> map = accruals.stream()
        .collect(Collectors.toMap(
            Accrual::getAccrualDate,
            Function.identity(),
            (k1, k2) -> k2,
            TreeMap::new)
        );

    LocalDate agreementStartDate = LocalDate.of(2023, 4, 1);
    LocalDate referenceDate = LocalDate.of(2023, 4, 18);

    module.cascadeCumulativeTotal(map, agreementStartDate);

    assertThat(map).hasSize(5);
    assertThat(map.get(referenceDate)
        .getCumulativeTotal()).usingComparator(BigDecimal::compareTo)
        .isEqualTo(BigDecimal.valueOf(6480));

    assertThat(map.get(referenceDate.plusDays(1))
        .getCumulativeTotal()).usingComparator(BigDecimal::compareTo)
        .isEqualTo(BigDecimal.valueOf(7080));

    assertThat(map.get(referenceDate.plusDays(2))
        .getCumulativeTotal()).usingComparator(BigDecimal::compareTo)
        .isEqualTo(BigDecimal.valueOf(7320));

    assertThat(map.get(referenceDate.plusDays(3))
        .getCumulativeTotal()).usingComparator(BigDecimal::compareTo)
        .isEqualTo(BigDecimal.valueOf(8040));
  }

  @Test
  void cascadeCumulativeTotal_emptyMapOfAccruals_throwException() {
    SortedMap<LocalDate, Accrual> map = new TreeMap<>();

    LocalDate agreementStartDate = LocalDate.of(2023, 4, 1);

    assertThatThrownBy(() ->
        module.cascadeCumulativeTotal(map, agreementStartDate))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(ACCRUALS_MAP_EMPTY);
  }
}