package uk.gov.homeoffice.digital.sas.balancecalculator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.homeoffice.digital.sas.balancecalculator.BalanceCalculator.ACCRUALS_NOT_FOUND;
import static uk.gov.homeoffice.digital.sas.balancecalculator.BalanceCalculator.AGREEMENT_NOT_FOUND;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.Constants.MISSING_ACCRUAL;
import static uk.gov.homeoffice.digital.sas.balancecalculator.testutils.CommonUtils.createAccrual;
import static uk.gov.homeoffice.digital.sas.balancecalculator.testutils.CommonUtils.loadAccrualsFromFile;
import static uk.gov.homeoffice.digital.sas.balancecalculator.testutils.CommonUtils.loadObjectFromFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import uk.gov.homeoffice.digital.sas.balancecalculator.client.RestClient;
import uk.gov.homeoffice.digital.sas.balancecalculator.handlers.ContributionsHandler;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Accrual;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Agreement;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.enums.AccrualType;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.timecard.TimeEntry;
import uk.gov.homeoffice.digital.sas.balancecalculator.module.AccrualModule;
import uk.gov.homeoffice.digital.sas.balancecalculator.module.AnnualTargetHoursAccrualModule;
import uk.gov.homeoffice.digital.sas.balancecalculator.testutils.CommonUtils;
import uk.gov.homeoffice.digital.sas.kafka.message.KafkaAction;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class BalanceCalculatorTest {

  private static final ZonedDateTime SHIFT_START_TIME =
      ZonedDateTime.parse("2023-04-18T08:00:00+00:00");
  private static final ZonedDateTime SHIFT_END_TIME =
      ZonedDateTime.parse("2023-04-18T10:00:00+00:00");

  private static final LocalDate ACCRUAL_DATE = SHIFT_START_TIME.toLocalDate();
  private static final String TIME_ENTRY_ID = "7f000001-879e-1b02-8187-9ef1640f0003";
  private static final String PERSON_ID = "0936e7a6-2b2e-1696-2546-5dd25dcae6a0";
  private static final LocalDate AGREEMENT_END_DATE = LocalDate.of(2024, 3, 31);
  private final List<AccrualModule> accrualModules = List.of(new AnnualTargetHoursAccrualModule());

  @Mock
  private RestClient restClient;

  private BalanceCalculator balanceCalculator;

  private ContributionsHandler contributionsHandler;

  private static Stream<Arguments> annualTargetHoursTestData() {
    return Stream.of(
        // creating one day time entry
        Arguments.of(TIME_ENTRY_ID,
            LocalDate.of(2023, 4, 18),
            ZonedDateTime.parse("2023-04-18T08:00:00+00:00"),
            ZonedDateTime.parse("2023-04-18T10:00:00+00:00"),
            BigDecimal.valueOf(6600), BigDecimal.valueOf(7200),
            BigDecimal.valueOf(7440), BigDecimal.valueOf(8160)),
        // updating one day time entry
        Arguments.of("e7d85e42-f0fb-4e2a-8211-874e27d1e888",
            LocalDate.of(2023, 4, 18),
            ZonedDateTime.parse("2023-04-18T14:00:00+00:00"),
            ZonedDateTime.parse("2023-04-18T14:30:00+00:00"),
            BigDecimal.valueOf(6150), BigDecimal.valueOf(6750),
            BigDecimal.valueOf(6990), BigDecimal.valueOf(7710)),
        // creating two day time entry
        Arguments.of("7f000001-879e-1b02-8187-9ef1640f0014",
            LocalDate.of(2023, 4, 19),
            ZonedDateTime.parse("2023-04-18T22:00:00+00:00"),
            ZonedDateTime.parse("2023-04-19T06:00:00+00:00"),
            BigDecimal.valueOf(6540), BigDecimal.valueOf(7560),
            BigDecimal.valueOf(7800), BigDecimal.valueOf(8520)),
        // creating three day time entry
        Arguments.of("7f000001-879e-1b02-8187-9ef1640f0013",
            LocalDate.of(2023, 4, 20),
            ZonedDateTime.parse("2023-04-18T21:00:00+00:00"),
            ZonedDateTime.parse("2023-04-20T06:00:00+00:00"),
            BigDecimal.valueOf(6600), BigDecimal.valueOf(8640),
            BigDecimal.valueOf(9300), BigDecimal.valueOf(10020))
    );
  }

  @BeforeEach
  void setup() {
    contributionsHandler = new ContributionsHandler(accrualModules);
    balanceCalculator = new BalanceCalculator(restClient, contributionsHandler);
  }

  @Test
  void sendToAccruals_withValidAccruals_shouldCallPatchAccruals() {
    String tenantId = "52a8188b-d41e-6768-19e9-09938016342f";

    Accrual accrual1 = createAccrual(UUID.fromString("0936e7a6-2b2e-1696-2546-5dd25dcae6a0"));
    Accrual accrual2 = createAccrual(UUID.fromString("a613dd93-3bdf-d285-c263-84d6866d61c5"));
    List<Accrual> accrualList = List.of(accrual1, accrual2);

    balanceCalculator.sendToAccruals(tenantId, accrualList);

    verify(restClient).patchAccruals(tenantId, accrualList);
  }

  @ParameterizedTest
  @MethodSource("annualTargetHoursTestData")
  void calculate_annualTargetHours_returnUpdateAccruals(String timeEntryId,
                                                        LocalDate referenceDate,
                                                        ZonedDateTime shiftStartTime,
                                                        ZonedDateTime shiftEndTime,
                                                        BigDecimal expectedCumulativeTotal1,
                                                        BigDecimal expectedCumulativeTotal2,
                                                        BigDecimal expectedCumulativeTotal3,
                                                        BigDecimal expectedCumulativeTotal4)
      throws IOException {

    TimeEntry timeEntry = CommonUtils.createTimeEntry(timeEntryId, PERSON_ID, shiftStartTime,
        shiftEndTime);

    String tenantId = timeEntry.getTenantId();

    when(restClient.getApplicableAgreement(tenantId, PERSON_ID, referenceDate))
        .thenReturn(loadObjectFromFile("data/agreement.json", Agreement.class));

    when(restClient.getAccrualsBetweenDates(tenantId, PERSON_ID,
        shiftStartTime.toLocalDate().minusDays(1),
        AGREEMENT_END_DATE))
        .thenReturn(loadAccrualsFromFile("data/accruals_annualTargetHours.json"));

    List<Accrual> accruals = balanceCalculator.calculate(timeEntry, KafkaAction.CREATE);

    assertThat(accruals).hasSize(4);
    assertThat(accruals.get(0).getCumulativeTotal()).usingComparator(
            BigDecimal::compareTo)
        .isEqualTo(expectedCumulativeTotal1);

    assertThat(accruals.get(1).getCumulativeTotal()).usingComparator(
            BigDecimal::compareTo)
        .isEqualTo(expectedCumulativeTotal2);

    assertThat(accruals.get(2).getCumulativeTotal()).usingComparator(
            BigDecimal::compareTo)
        .isEqualTo(expectedCumulativeTotal3);

    assertThat(accruals.get(3).getCumulativeTotal()).usingComparator(
            BigDecimal::compareTo)
        .isEqualTo(expectedCumulativeTotal4);
  }

  @Test
  void calculate_noAgreementFound_logWarningAndReturnEmptyList(CapturedOutput capturedOutput) {

    TimeEntry timeEntry = CommonUtils.createTimeEntry(TIME_ENTRY_ID, PERSON_ID, SHIFT_START_TIME,
        SHIFT_END_TIME);

    when(restClient.getApplicableAgreement(timeEntry.getTenantId(),
        PERSON_ID, ACCRUAL_DATE)).thenReturn(null);

    List<Accrual> result = balanceCalculator.calculate(timeEntry,KafkaAction.CREATE);

    assertThat(result).isEmpty();

    assertThat(capturedOutput.getOut()).contains("WARN");
    assertThat(capturedOutput.getOut()).contains(
        MessageFormat.format(AGREEMENT_NOT_FOUND, timeEntry.getTenantId(), PERSON_ID, ACCRUAL_DATE)
    );
  }

  @Test
  void calculate_noAccrualsFound_logWarningAndReturnEmptyList(CapturedOutput capturedOutput) {

    TimeEntry timeEntry = CommonUtils.createTimeEntry(TIME_ENTRY_ID, PERSON_ID, SHIFT_START_TIME,
        SHIFT_END_TIME);

    Agreement agreement = mock(Agreement.class);
    when(agreement.getEndDate()).thenReturn(AGREEMENT_END_DATE);
    when(restClient.getApplicableAgreement(timeEntry.getTenantId(), PERSON_ID, ACCRUAL_DATE))
        .thenReturn(agreement);

    List<Accrual> noAccruals = List.of();
    when(restClient.getAccrualsBetweenDates(timeEntry.getTenantId(), PERSON_ID,
        ACCRUAL_DATE.minusDays(1), agreement.getEndDate()))
        .thenReturn(noAccruals);

    List<Accrual> result = balanceCalculator.calculate(timeEntry, KafkaAction.CREATE);

    assertThat(result).isEmpty();

    assertThat(capturedOutput.getOut()).contains("WARN");
    assertThat(capturedOutput.getOut()).contains(
        MessageFormat.format(ACCRUALS_NOT_FOUND, timeEntry.getTenantId(), PERSON_ID,
            ACCRUAL_DATE.minusDays(1), AGREEMENT_END_DATE)
    );
  }

  @Test
  void calculate_noAccrualFoundForReferenceDate_logErrorAndReturnEmptyList(
      CapturedOutput capturedOutput) {
    TimeEntry timeEntry = CommonUtils.createTimeEntry(TIME_ENTRY_ID, PERSON_ID, SHIFT_START_TIME,
        SHIFT_END_TIME);

    Agreement agreement = mock(Agreement.class);
    when(agreement.getEndDate()).thenReturn(AGREEMENT_END_DATE);
    when(restClient.getApplicableAgreement(timeEntry.getTenantId(), PERSON_ID, ACCRUAL_DATE))
        .thenReturn(agreement);

    AccrualType accrualType = AccrualType.ANNUAL_TARGET_HOURS;
    Accrual accrual = Accrual.builder()
        .accrualDate(ACCRUAL_DATE.minusDays(1))
        .accrualTypeId(accrualType.getId())
        .build();
    List<Accrual> accruals = List.of(accrual);
    when(restClient.getAccrualsBetweenDates(timeEntry.getTenantId(), PERSON_ID,
        ACCRUAL_DATE.minusDays(1), agreement.getEndDate()))
        .thenReturn(accruals);

    List<Accrual> result = balanceCalculator.calculate(timeEntry, KafkaAction.CREATE);

    assertThat(result).isEmpty();

    assertThat(capturedOutput.getOut()).contains("ERROR");
    assertThat(capturedOutput.getOut()).contains(
        MessageFormat.format(MISSING_ACCRUAL, timeEntry.getTenantId(), PERSON_ID,
            accrualType, ACCRUAL_DATE)
    );
  }

  @Test
  void map_listOfAccruals_mappedByAccrualTypeAndDate() throws IOException {
    List<Accrual> accruals = loadAccrualsFromFile("data/accruals_convertToMap.json");

    Map<AccrualType, SortedMap<LocalDate, Accrual>> map =
        balanceCalculator.map(accruals);

    assertThat(map).hasSize(2);

    SortedMap<LocalDate, Accrual> annualTargetHoursMap = map.get(AccrualType.ANNUAL_TARGET_HOURS);

    assertThat(annualTargetHoursMap).hasSize(2);
    assertThat(annualTargetHoursMap.get(LocalDate.of(2023, 4, 19))
        .getCumulativeTotal()).usingComparator(BigDecimal::compareTo)
        .isEqualTo(BigDecimal.valueOf(7080));
    assertThat(annualTargetHoursMap.get(LocalDate.of(2023, 4, 20))
        .getCumulativeTotal()).usingComparator(BigDecimal::compareTo)
        .isEqualTo(BigDecimal.valueOf(7320));

    SortedMap<LocalDate, Accrual> nightHoursMap = map.get(AccrualType.NIGHT_HOURS);
    assertThat(nightHoursMap).hasSize(1);
    assertThat(nightHoursMap.get(LocalDate.of(2023, 4, 19))
        .getCumulativeTotal()).usingComparator(BigDecimal::compareTo)
        .isEqualTo(BigDecimal.valueOf(8040));
  }


}