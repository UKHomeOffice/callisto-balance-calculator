package uk.gov.homeoffice.digital.sas.balancecalculator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.homeoffice.digital.sas.balancecalculator.BalanceCalculator.ACCRUALS_NOT_FOUND;
import static uk.gov.homeoffice.digital.sas.balancecalculator.BalanceCalculator.AGREEMENT_NOT_FOUND;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.Constants.MISSING_ACCRUAL;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.Constants.NO_ACCRUALS_FOUND_FOR_TYPE;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.ERROR_LOG;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.WARNING_LOG;
import static uk.gov.homeoffice.digital.sas.balancecalculator.testutils.CommonUtils.createAccrual;
import static uk.gov.homeoffice.digital.sas.balancecalculator.testutils.CommonUtils.loadAccrualsFromFile;
import static uk.gov.homeoffice.digital.sas.balancecalculator.testutils.CommonUtils.loadObjectFromFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import uk.gov.homeoffice.digital.sas.balancecalculator.client.AccrualsService;
import uk.gov.homeoffice.digital.sas.balancecalculator.handlers.ContributionsHandler;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Accrual;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Agreement;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Contributions;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.enums.AccrualType;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.timecard.TimeEntry;
import uk.gov.homeoffice.digital.sas.balancecalculator.module.AccrualModule;
import uk.gov.homeoffice.digital.sas.balancecalculator.module.AnnualTargetHoursAccrualModule;
import uk.gov.homeoffice.digital.sas.balancecalculator.module.NightHoursAccrualModule;
import uk.gov.homeoffice.digital.sas.balancecalculator.testutils.CommonUtils;
import uk.gov.homeoffice.digital.sas.kafka.message.KafkaAction;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class BalanceCalculatorCreateActionTest {

  private static final String SHIFT_START_TIME = "2023-04-18T08:00:00+00:00";
  private static final String SHIFT_END_TIME = "2023-04-18T10:00:00+00:00";

  private static final LocalDate ACCRUAL_DATE =
      LocalDate.from(ZonedDateTime.parse(SHIFT_START_TIME));
  private static final String TIME_ENTRY_ID = "7f000001-879e-1b02-8187-9ef1640f0003";
  private static final String PERSON_ID = "0936e7a6-2b2e-1696-2546-5dd25dcae6a0";
  private static final LocalDate AGREEMENT_START_DATE = LocalDate.of(2023, 4, 1);
  private static final LocalDate AGREEMENT_END_DATE = LocalDate.of(2024, 3, 31);

  private List<AccrualModule> accrualModules;

  @Mock
  private AccrualsService accrualsService;

  private BalanceCalculator balanceCalculator;

  private static Stream<Arguments> annualTargetHoursTestData() {
    return Stream.of(
        // creating one day time entry
        Arguments.of(TIME_ENTRY_ID,
            LocalDate.of(2023, 4, 18),
            "2023-04-18T08:00:00+00:00",
            "2023-04-18T10:00:00+00:00",
            BigDecimal.valueOf(6600), BigDecimal.valueOf(7200),
            BigDecimal.valueOf(7440), BigDecimal.valueOf(8160)),
        // updating one day time entry
        Arguments.of("85cd140e-9eeb-4771-ab6c-6dea17fcfcbe",
            LocalDate.of(2023, 4, 18),
            "2023-04-18T14:00:00+00:00",
            "2023-04-18T14:30:00+00:00",
            BigDecimal.valueOf(6390), BigDecimal.valueOf(6990),
            BigDecimal.valueOf(7230), BigDecimal.valueOf(7950)),
        // creating two day time entry
        Arguments.of("7f000001-879e-1b02-8187-9ef1640f0014",
            LocalDate.of(2023, 4, 19),
            "2023-04-18T22:00:00+00:00",
            "2023-04-19T06:00:00+00:00",
            BigDecimal.valueOf(6540), BigDecimal.valueOf(7560),
            BigDecimal.valueOf(7800), BigDecimal.valueOf(8520)),
        // creating three day time entry
        Arguments.of("7f000001-879e-1b02-8187-9ef1640f0013",
            LocalDate.of(2023, 4, 20),
            "2023-04-18T21:00:00+00:00",
            "2023-04-20T06:00:00+00:00",
            BigDecimal.valueOf(6600), BigDecimal.valueOf(8640),
            BigDecimal.valueOf(9300), BigDecimal.valueOf(10020))
    );
  }

  private static Stream<Arguments> nightHoursTestData() {
    return Stream.of(
        // outside night hours
        Arguments.of(TIME_ENTRY_ID,
            LocalDate.of(2023, 4, 18),
            "2023-04-18T08:00:00+01:00",
            "2023-04-18T10:00:00+01:00",
            BigDecimal.valueOf(6180), BigDecimal.valueOf(6300),
            BigDecimal.valueOf(6300), BigDecimal.valueOf(6300)),
        // creating one day time entry
        Arguments.of(TIME_ENTRY_ID,
            LocalDate.of(2023, 4, 18),
            "2023-04-18T00:00:00+01:00",
            "2023-04-18T03:00:00+01:00",
            BigDecimal.valueOf(6360), BigDecimal.valueOf(6480),
            BigDecimal.valueOf(6480), BigDecimal.valueOf(6480)),
        // updating one day time entry
        Arguments.of("e7d85e42-f0fb-4e2a-8211-874e27d1e888",
            LocalDate.of(2023, 4, 18),
            "2023-04-18T01:00:00+01:00",
            "2023-04-18T05:00:00+01:00",
            BigDecimal.valueOf(6240), BigDecimal.valueOf(6360),
            BigDecimal.valueOf(6360), BigDecimal.valueOf(6360)),
        // creating two day time entry
        Arguments.of(TIME_ENTRY_ID,
            LocalDate.of(2023, 4, 19),
            "2023-04-18T22:00:00+01:00",
            "2023-04-19T06:00:00+01:00",
            BigDecimal.valueOf(6240), BigDecimal.valueOf(6720),
            BigDecimal.valueOf(6720), BigDecimal.valueOf(6720)),
        // creating three day time entry
        Arguments.of(TIME_ENTRY_ID,
            LocalDate.of(2023, 4, 20),
            "2023-04-18T22:00:00+01:00",
            "2023-04-20T07:00:00+01:00",
            BigDecimal.valueOf(6240), BigDecimal.valueOf(6780),
            BigDecimal.valueOf(7140), BigDecimal.valueOf(7140))
    );
  }

  @Test
  void sendToAccruals_withValidAccruals_shouldCallPatchAccruals() {
    ContributionsHandler contributionsHandler = new ContributionsHandler(accrualModules);
    balanceCalculator = new BalanceCalculator(accrualsService, contributionsHandler);

    String tenantId = "52a8188b-d41e-6768-19e9-09938016342f";

    Accrual accrual1 = createAccrual(UUID.fromString("0936e7a6-2b2e-1696-2546-5dd25dcae6a0"));
    Accrual accrual2 = createAccrual(UUID.fromString("a613dd93-3bdf-d285-c263-84d6866d61c5"));
    List<Accrual> accrualList = List.of(accrual1, accrual2);

    balanceCalculator.sendToAccruals(tenantId, accrualList);

    verify(accrualsService).updateAccruals(tenantId, accrualList);
  }

  @ParameterizedTest
  @MethodSource("annualTargetHoursTestData")
  void calculate_annualTargetHours_returnUpdateAccruals(String timeEntryId,
                                                        LocalDate referenceDate,
                                                        String shiftStartTime,
                                                        String shiftEndTime,
                                                        BigDecimal expectedCumulativeTotal1,
                                                        BigDecimal expectedCumulativeTotal2,
                                                        BigDecimal expectedCumulativeTotal3,
                                                        BigDecimal expectedCumulativeTotal4)
      throws IOException {

    accrualModules = List.of(new AnnualTargetHoursAccrualModule());
    ContributionsHandler contributionsHandler = new ContributionsHandler(accrualModules);
    balanceCalculator = new BalanceCalculator(accrualsService, contributionsHandler);

    TimeEntry timeEntry = CommonUtils.createTimeEntry(timeEntryId, PERSON_ID, shiftStartTime,
        shiftEndTime);

    String tenantId = timeEntry.getTenantId();

    when(accrualsService.getApplicableAgreement(tenantId, PERSON_ID, referenceDate))
        .thenReturn(loadObjectFromFile("data/agreement.json", Agreement.class));

    when(accrualsService.getImpactedAccruals(tenantId, PERSON_ID, timeEntryId,
        timeEntry.getActualStartTime().toLocalDate(), timeEntry.getActualEndTime().toLocalDate()))
        .thenReturn(loadAccrualsFromFile("data/accruals_annualTargetHours.json"));

    List<Accrual> accruals = balanceCalculator.calculate(timeEntry, KafkaAction.CREATE);

    assertThat(accruals).hasSize(4);

    assertCumulativeTotal(accruals.get(0), expectedCumulativeTotal1);
    assertCumulativeTotal(accruals.get(1), expectedCumulativeTotal2);
    assertCumulativeTotal(accruals.get(2), expectedCumulativeTotal3);
    assertCumulativeTotal(accruals.get(3), expectedCumulativeTotal4);
  }

  @Test
  void calculate_noPriorDateAccrual_useZeroAsBaseCumulativeTotal()
      throws IOException {

    accrualModules = List.of(new AnnualTargetHoursAccrualModule());
    ContributionsHandler contributionsHandler = new ContributionsHandler(accrualModules);
    balanceCalculator = new BalanceCalculator(accrualsService, contributionsHandler);

    String shiftStartTime = "2023-04-01T10:00:00+00:00";
    String shiftEndTime = "2023-04-01T12:00:00+00:00";
    TimeEntry timeEntry = CommonUtils.createTimeEntry(shiftStartTime,
        shiftEndTime);

    LocalDate referenceDate = AGREEMENT_START_DATE;

    String tenantId = timeEntry.getTenantId();
    String personId = timeEntry.getOwnerId();

    when(accrualsService.getApplicableAgreement(tenantId, personId, referenceDate))
        .thenReturn(loadObjectFromFile("data/agreement.json", Agreement.class));

    when(accrualsService.getImpactedAccruals(tenantId, personId, timeEntry.getId(),
        timeEntry.getActualStartTime().toLocalDate(), timeEntry.getActualEndTime().toLocalDate()))
        .thenReturn(loadAccrualsFromFile("data/accruals_noPriorDateAccrual.json"));

    List<Accrual> accruals = balanceCalculator.calculate(timeEntry, KafkaAction.CREATE);

    assertThat(accruals).hasSize(4);

    assertAccrualDateAndCumulativeTotal(
        accruals.get(0), "2023-04-01", new BigDecimal(600));
    assertAccrualDateAndCumulativeTotal(accruals.get(1), "2023-04-02", new BigDecimal(1200));
    assertAccrualDateAndCumulativeTotal(accruals.get(2), "2023-04-03", new BigDecimal(1440));
    assertAccrualDateAndCumulativeTotal(accruals.get(3), "2023-04-04", new BigDecimal(2160));
  }

  @ParameterizedTest
  @MethodSource("nightHoursTestData")
  void calculate_nightHours_returnUpdatedAccruals(String timeEntryId,
                                                  LocalDate referenceDate,
                                                  String shiftStartTime,
                                                  String shiftEndTime,
                                                  BigDecimal expectedCumulativeTotal1,
                                                  BigDecimal expectedCumulativeTotal2,
                                                  BigDecimal expectedCumulativeTotal3,
                                                  BigDecimal expectedCumulativeTotal4)
      throws IOException {

    accrualModules = List.of(new NightHoursAccrualModule());
    ContributionsHandler contributionsHandler = new ContributionsHandler(accrualModules);
    balanceCalculator = new BalanceCalculator(accrualsService, contributionsHandler);

    TimeEntry timeEntry = CommonUtils.createTimeEntry(timeEntryId, PERSON_ID, shiftStartTime,
        shiftEndTime);

    String tenantId = timeEntry.getTenantId();

    when(accrualsService.getApplicableAgreement(tenantId, PERSON_ID, referenceDate))
        .thenReturn(loadObjectFromFile("data/agreement.json", Agreement.class));

    when(accrualsService.getImpactedAccruals(tenantId, PERSON_ID, timeEntryId,
        timeEntry.getActualStartTime().toLocalDate(), timeEntry.getActualEndTime().toLocalDate()))
        .thenReturn(loadAccrualsFromFile("data/accruals_nightHours.json"));

    List<Accrual> accruals = balanceCalculator.calculate(timeEntry, KafkaAction.CREATE);

    assertThat(accruals).hasSize(4);

    assertCumulativeTotal(accruals.get(0), expectedCumulativeTotal1);
    assertCumulativeTotal(accruals.get(1), expectedCumulativeTotal2);
    assertCumulativeTotal(accruals.get(2), expectedCumulativeTotal3);
    assertCumulativeTotal(accruals.get(3), expectedCumulativeTotal4);
  }

  @Test
  void calculate_noAgreementFound_logWarningAndReturnEmptyList(CapturedOutput capturedOutput) {

    ContributionsHandler contributionsHandler = new ContributionsHandler(accrualModules);
    balanceCalculator = new BalanceCalculator(accrualsService, contributionsHandler);

    TimeEntry timeEntry = CommonUtils.createTimeEntry(TIME_ENTRY_ID, PERSON_ID, SHIFT_START_TIME,
        SHIFT_END_TIME);

    when(accrualsService.getApplicableAgreement(timeEntry.getTenantId(),
        PERSON_ID, ACCRUAL_DATE)).thenReturn(null);

    List<Accrual> result = balanceCalculator.calculate(timeEntry, KafkaAction.CREATE);

    assertThat(result).isEmpty();

    assertThat(capturedOutput.getOut()).contains(WARNING_LOG);
    assertThat(capturedOutput.getOut()).contains(
        MessageFormat.format(AGREEMENT_NOT_FOUND, timeEntry.getTenantId(), PERSON_ID, ACCRUAL_DATE)
    );
  }

  @Test
  void calculate_missingAccruals_logWarningAndReturnEmptyList(CapturedOutput capturedOutput)
      throws IOException {

    accrualModules = List.of(new AnnualTargetHoursAccrualModule());
    ContributionsHandler contributionsHandler = new ContributionsHandler(accrualModules);
    balanceCalculator = new BalanceCalculator(accrualsService, contributionsHandler);

    TimeEntry timeEntry = CommonUtils.createTimeEntry(TIME_ENTRY_ID, PERSON_ID, SHIFT_START_TIME,
        SHIFT_END_TIME);

    Agreement agreement = mock(Agreement.class);
    when(agreement.getStartDate()).thenReturn(AGREEMENT_START_DATE);
    when(agreement.getEndDate()).thenReturn(AGREEMENT_END_DATE);
    when(accrualsService.getApplicableAgreement(timeEntry.getTenantId(), PERSON_ID, ACCRUAL_DATE))
        .thenReturn(agreement);

    when(accrualsService.getImpactedAccruals(timeEntry.getTenantId(), PERSON_ID, timeEntry.getId(),
        timeEntry.getActualStartTime().toLocalDate(), timeEntry.getActualEndTime().toLocalDate()))
        .thenReturn(loadAccrualsFromFile("data/accruals_nightHours.json"));

    List<Accrual> result = balanceCalculator.calculate(timeEntry, KafkaAction.CREATE);

    assertThat(result).isEmpty();

    assertThat(capturedOutput.getOut()).contains(ERROR_LOG);
    assertThat(capturedOutput.getOut()).contains(
        MessageFormat.format(NO_ACCRUALS_FOUND_FOR_TYPE, AccrualType.ANNUAL_TARGET_HOURS,
            AGREEMENT_START_DATE, AGREEMENT_END_DATE)
    );
  }

  @Test
  void calculate_noAccrualsFound_logWarningAndReturnEmptyList(CapturedOutput capturedOutput) {

    ContributionsHandler contributionsHandler = new ContributionsHandler(accrualModules);
    balanceCalculator = new BalanceCalculator(accrualsService, contributionsHandler);

    TimeEntry timeEntry = CommonUtils.createTimeEntry(TIME_ENTRY_ID, PERSON_ID, SHIFT_START_TIME,
        SHIFT_END_TIME);

    Agreement agreement = mock(Agreement.class);

    when(accrualsService.getApplicableAgreement(timeEntry.getTenantId(), PERSON_ID, ACCRUAL_DATE))
        .thenReturn(agreement);

    List<Accrual> noAccruals = List.of();
    when(accrualsService.getImpactedAccruals(timeEntry.getTenantId(), PERSON_ID, timeEntry.getId(),
        timeEntry.getActualStartTime().toLocalDate(), timeEntry.getActualEndTime().toLocalDate()))
        .thenReturn(noAccruals);

    List<Accrual> result = balanceCalculator.calculate(timeEntry, KafkaAction.CREATE);

    assertThat(result).isEmpty();

    assertThat(capturedOutput.getOut()).contains(WARNING_LOG);
    assertThat(capturedOutput.getOut()).contains(
        MessageFormat.format(ACCRUALS_NOT_FOUND, timeEntry.getTenantId(), timeEntry.getId(),
            LocalDate.from(timeEntry.getActualStartTime()), LocalDate.from(timeEntry.getActualEndTime()))
    );
  }

  @Test
  void calculate_noAccrualFoundForReferenceDate_logErrorAndReturnEmptyList(
      CapturedOutput capturedOutput) {

    accrualModules = List.of(new AnnualTargetHoursAccrualModule());
    ContributionsHandler contributionsHandler = new ContributionsHandler(accrualModules);
    balanceCalculator = new BalanceCalculator(accrualsService, contributionsHandler);

    TimeEntry timeEntry = CommonUtils.createTimeEntry(TIME_ENTRY_ID, PERSON_ID, SHIFT_START_TIME,
        SHIFT_END_TIME);

    Agreement agreement = mock(Agreement.class);

    when(accrualsService.getApplicableAgreement(timeEntry.getTenantId(), PERSON_ID, ACCRUAL_DATE))
        .thenReturn(agreement);

    AccrualType accrualType = AccrualType.ANNUAL_TARGET_HOURS;

    Map<UUID, BigDecimal> emptyMap = new HashMap<>();

    Accrual accrual = Accrual.builder()
        .accrualDate(ACCRUAL_DATE.minusDays(1))
        .accrualTypeId(accrualType.getId())
        .contributions(Contributions.builder().timeEntries(emptyMap).build())
        .build();
    List<Accrual> accruals = List.of(accrual);
    when(accrualsService.getImpactedAccruals(timeEntry.getTenantId(), PERSON_ID, timeEntry.getId(),
        timeEntry.getActualStartTime().toLocalDate(), timeEntry.getActualEndTime().toLocalDate()))
        .thenReturn(accruals);

    List<Accrual> result = balanceCalculator.calculate(timeEntry, KafkaAction.CREATE);

    assertThat(result).isEmpty();

    assertThat(capturedOutput.getOut()).contains(ERROR_LOG);
    assertThat(capturedOutput.getOut()).contains(
        MessageFormat.format(MISSING_ACCRUAL, timeEntry.getTenantId(), PERSON_ID,
            accrualType, ACCRUAL_DATE)
    );
  }

  @Test
  void map_listOfAccruals_mappedByAccrualTypeAndDate() throws IOException {

    ContributionsHandler contributionsHandler = new ContributionsHandler(accrualModules);
    balanceCalculator = new BalanceCalculator(accrualsService, contributionsHandler);

    List<Accrual> accruals = loadAccrualsFromFile("data/accruals_convertToMap.json");

    Map<AccrualType, SortedMap<LocalDate, Accrual>> map =
        balanceCalculator.map(accruals);

    assertThat(map).hasSize(2);

    SortedMap<LocalDate, Accrual> annualTargetHoursMap = map.get(AccrualType.ANNUAL_TARGET_HOURS);

    assertThat(annualTargetHoursMap).hasSize(2);

    assertCumulativeTotal(annualTargetHoursMap.get(LocalDate.of(2023, 4, 19)),
        BigDecimal.valueOf(7080));
    assertCumulativeTotal(annualTargetHoursMap.get(LocalDate.of(2023, 4, 20)),
        BigDecimal.valueOf(7320));

    SortedMap<LocalDate, Accrual> nightHoursMap = map.get(AccrualType.NIGHT_HOURS);
    assertThat(nightHoursMap).hasSize(1);

    assertCumulativeTotal(nightHoursMap.get(LocalDate.of(2023, 4, 19)), BigDecimal.valueOf(8040));
  }

  private void assertCumulativeTotal(Accrual accrual, BigDecimal expectedCumulativeTotal) {
    assertThat(accrual.getCumulativeTotal()).usingComparator(
            BigDecimal::compareTo)
        .isEqualTo(expectedCumulativeTotal);
  }

  private void assertAccrualDateAndCumulativeTotal(Accrual accrual, String expectedAccrualDate,
                                                BigDecimal expectedCumulativeTotal) {
    assertCumulativeTotal(accrual, expectedCumulativeTotal);
    assertThat(accrual.getAccrualDate()).isEqualTo(LocalDate.parse(expectedAccrualDate));
  }
}