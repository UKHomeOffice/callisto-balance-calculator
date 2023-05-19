package uk.gov.homeoffice.digital.sas.balancecalculator.handlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static uk.gov.homeoffice.digital.sas.balancecalculator.handlers.ContributionsHandler.ACCRUALS_MAP_EMPTY;
import static uk.gov.homeoffice.digital.sas.balancecalculator.testutils.CommonUtils.accrualListToAccrualTypeMap;
import static uk.gov.homeoffice.digital.sas.balancecalculator.testutils.CommonUtils.createTimeEntry;
import static uk.gov.homeoffice.digital.sas.balancecalculator.testutils.CommonUtils.loadAccrualsFromFile;
import static uk.gov.homeoffice.digital.sas.balancecalculator.testutils.CommonUtils.loadObjectFromFile;

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
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Accrual;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Agreement;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Contributions;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.enums.AccrualType;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.timecard.TimeEntry;
import uk.gov.homeoffice.digital.sas.balancecalculator.module.AccrualModule;
import uk.gov.homeoffice.digital.sas.balancecalculator.module.AnnualTargetHoursAccrualModule;
import uk.gov.homeoffice.digital.sas.kafka.message.KafkaAction;

@ExtendWith({MockitoExtension.class})
class ContributionsHandlerTest {

  private static final ZonedDateTime SHIFT_START_TIME =
      ZonedDateTime.parse("2023-04-18T08:00:00+00:00");
  private static final BigDecimal SHIFT_DURATION = new BigDecimal(120);
  private static final LocalDate ACCRUAL_DATE = SHIFT_START_TIME.toLocalDate();
  private static final String TIME_ENTRY_ID = "b63d75f8-f62b-11ed-b67e-0242ac120002";
  private static final String TENANT_ID = "52a8188b-d41e-6768-19e9-09938016342f";
  private static final String PERSON_ID = "0936e7a6-2b2e-1696-2546-5dd25dcae6a0";

  Agreement applicableAgreement;
  List<Accrual> accruals;
  Map<AccrualType, SortedMap<LocalDate, Accrual>> allAccruals;
  List<AccrualModule> accrualModules;

  private  ContributionsHandler contributionsHandler;

  @BeforeEach
  void setup() throws IOException {
    contributionsHandler = spy(new ContributionsHandler());
    applicableAgreement = loadObjectFromFile("data/agreement.json", Agreement.class);
    accruals = loadAccrualsFromFile("data/accruals_annualTargetHours.json");
    allAccruals = accrualListToAccrualTypeMap(accruals);
    accrualModules = List.of(new AnnualTargetHoursAccrualModule());
  }

  private static Stream<Arguments> annualTargetHoursTestData() {
    return Stream.of(
        Arguments.of(
            "b63d75f8-f62b-11ed-b67e-0242ac120002", // Action CREATE
            new BigDecimal[] {
                BigDecimal.valueOf(120), BigDecimal.valueOf(6000),
                BigDecimal.valueOf(600), BigDecimal.valueOf(6600),
                BigDecimal.valueOf(600), BigDecimal.valueOf(7200),
                BigDecimal.valueOf(240), BigDecimal.valueOf(7440),
                BigDecimal.valueOf(720), BigDecimal.valueOf(8160)},
                ZonedDateTime.parse("2023-04-18T08:00:00+01:00"),
                ZonedDateTime.parse("2023-04-18T10:00:00+01:00"),
                KafkaAction.CREATE
            ),
        Arguments.of(
            "9caab6a7-31a5-4679-bd14-fbf09b1cec92", // Action DELETE
            new BigDecimal[] {
                BigDecimal.valueOf(120), BigDecimal.valueOf(6000),
                BigDecimal.valueOf(480), BigDecimal.valueOf(6480),
                BigDecimal.valueOf(600), BigDecimal.valueOf(7080),
                BigDecimal.valueOf(0), BigDecimal.valueOf(7080),
                BigDecimal.valueOf(720), BigDecimal.valueOf(7800)},
                ZonedDateTime.parse("2023-04-20T08:00:00+01:00"),
                ZonedDateTime.parse("2023-04-20T12:00:00+01:00"),
                KafkaAction.DELETE
        )
    );
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

  @ParameterizedTest
  @MethodSource("annualTargetHoursTestData")
  void handleMethod_kafkaCorrectAction_updateAccrualsList (
      String timeEntryId, BigDecimal[] totals,
      ZonedDateTime startTime, ZonedDateTime finishTime, KafkaAction action) {

    TimeEntry timeEntry = createTimeEntry(timeEntryId, TENANT_ID, PERSON_ID,
        startTime, finishTime);

    contributionsHandler.handle(timeEntry, applicableAgreement, allAccruals, accrualModules,
        action);

    switch (action){
      case CREATE -> verify(contributionsHandler)
          .handleCreateAction(timeEntry, applicableAgreement, allAccruals, accrualModules);
      case DELETE -> verify(contributionsHandler)
          .handleDeleteAction(timeEntry, applicableAgreement, allAccruals, accrualModules);
    }

    List<Accrual> updatedAccrualsList = allAccrualsMapToAccrualsList(allAccruals);

    for (int i=0 ; i<totals.length / 2; i++) {
      assertThat(updatedAccrualsList.get(i).getContributions().getTotal())
          .usingComparator(BigDecimal::compareTo).isEqualTo(totals[i*2]);
      assertThat(updatedAccrualsList.get(i).getCumulativeTotal())
          .usingComparator(BigDecimal::compareTo).isEqualTo(totals[i*2+1]);
    }
  }

  private List<Accrual> allAccrualsMapToAccrualsList(Map<AccrualType,
      SortedMap<LocalDate, Accrual>> allAccruals) {
    return allAccruals.values().stream()
        .flatMap(m -> m.values().stream())
        .collect(Collectors.toList());
  }

}