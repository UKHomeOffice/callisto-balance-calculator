package uk.gov.homeoffice.digital.sas.balancecalculator.handlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.gov.homeoffice.digital.sas.balancecalculator.handlers.ContributionsHandler.ACCRUALS_MAP_EMPTY;
import static uk.gov.homeoffice.digital.sas.balancecalculator.testutils.CommonUtils.loadAccrualsFromFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Accrual;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Contributions;

class ContributionsHandlerTest {

  private static final ZonedDateTime SHIFT_START_TIME =
      ZonedDateTime.parse("2023-04-18T08:00:00+00:00");
  private static final BigDecimal SHIFT_DURATION = new BigDecimal(120);

  private static final LocalDate ACCRUAL_DATE = SHIFT_START_TIME.toLocalDate();
  private static final String TIME_ENTRY_ID = "7f000001-879e-1b02-8187-9ef1640f0003";

  private  ContributionsHandler contributionsHandler;


  @BeforeEach
  void setup() {
    contributionsHandler = new ContributionsHandler();
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

    contributionsHandler.cascadeCumulativeTotal(map, agreementStartDate);

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
        contributionsHandler.cascadeCumulativeTotal(map, agreementStartDate))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(ACCRUALS_MAP_EMPTY);
  }

  @Test
  void updateAccrualContribution_hasNoContributions_returnUpdatedAccrual() {

    Contributions contributions = Contributions.builder()
        .timeEntries(new HashMap<>())
        .total(BigDecimal.ZERO)
        .build();
    Accrual accrual = Accrual.builder()
        .accrualDate(ACCRUAL_DATE)
        .contributions(contributions)
        .build();

    contributionsHandler.updateAccrualContribution(TIME_ENTRY_ID, SHIFT_DURATION, accrual);

    assertThat(accrual.getContributions().getTotal()).usingComparator(BigDecimal::compareTo)
        .isEqualTo(BigDecimal.valueOf(120));
    assertThat(accrual.getContributions().getTimeEntries()).hasSize(1);
    assertThat(accrual.getContributions().getTimeEntries()).containsEntry(
        UUID.fromString(TIME_ENTRY_ID), SHIFT_DURATION);
  }

  @Test
  void updateAccrualContribution_hasExistingContribution_returnUpdatedAccrual() {
    UUID existingTimeEntryId = UUID.fromString("e7d85e42-f0fb-4e2a-8211-874e27d1e888");

    Contributions contributions = Contributions.builder()
        .timeEntries(new HashMap<>(Map.of(existingTimeEntryId, BigDecimal.TEN)))
        .total(BigDecimal.TEN)
        .build();
    Accrual accrual = Accrual.builder()
        .accrualDate(ACCRUAL_DATE)
        .contributions(contributions)
        .build();

    contributionsHandler.updateAccrualContribution(TIME_ENTRY_ID, SHIFT_DURATION, accrual);

    assertThat(accrual.getContributions().getTotal()).usingComparator(BigDecimal::compareTo)
        .isEqualTo(BigDecimal.valueOf(130));

    assertThat(accrual.getContributions().getTimeEntries()).hasSize(2);

    assertThat(accrual.getContributions().getTimeEntries()).containsEntry(
        existingTimeEntryId, BigDecimal.TEN);
    assertThat(accrual.getContributions().getTimeEntries()).containsEntry(
        UUID.fromString(TIME_ENTRY_ID), SHIFT_DURATION);
  }

}