package uk.gov.homeoffice.digital.sas.balancecalculator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.enums.AccrualType.ANNUAL_TARGET_HOURS;
import static uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.enums.AccrualType.NIGHT_HOURS;
import static uk.gov.homeoffice.digital.sas.balancecalculator.testutils.CommonUtils.assertTypeAndDateAndTotals;
import static uk.gov.homeoffice.digital.sas.balancecalculator.testutils.CommonUtils.loadAccrualsFromFile;
import static uk.gov.homeoffice.digital.sas.balancecalculator.testutils.CommonUtils.loadObjectFromFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.OutputCaptureExtension;
import uk.gov.homeoffice.digital.sas.balancecalculator.client.AccrualsService;
import uk.gov.homeoffice.digital.sas.balancecalculator.handlers.ContributionsHandler;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Accrual;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Agreement;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.timecard.TimeEntry;
import uk.gov.homeoffice.digital.sas.balancecalculator.module.AccrualModule;
import uk.gov.homeoffice.digital.sas.balancecalculator.module.AnnualTargetHoursAccrualModule;
import uk.gov.homeoffice.digital.sas.balancecalculator.module.NightHoursAccrualModule;
import uk.gov.homeoffice.digital.sas.balancecalculator.testutils.CommonUtils;
import uk.gov.homeoffice.digital.sas.kafka.message.KafkaAction;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class BalanceCalculatorDeleteActionTest {

  private static final String PERSON_ID = "0936e7a6-2b2e-1696-2546-5dd25dcae6a0";

  @Mock
  private AccrualsService accrualsService;

  private BalanceCalculator balanceCalculator;

  private static Stream<Arguments> annualTargetHoursTestData() {
    return Stream.of(
        // deleting one day time entry
        Arguments.of("38e09687-5ae7-40d6-82b4-b022ae456bb1",
            LocalDate.of(2023, 4, 18),
            "2023-04-18T08:00:00+00:00",
            "2023-04-18T10:00:00+00:00",
            "2023-04-18", "2023-04-19", "2023-04-20", "2023-04-21",
            6720, 7320, 7920, 8640, 720, 600, 600, 720),
        // deleting two day time entry
        Arguments.of("e7d85e42-f0fb-4e2a-8211-874e27d1e888",
            LocalDate.of(2023, 4, 19),
            "2023-04-18T18:00:00+00:00",
            "2023-04-19T06:00:00+00:00",
            "2023-04-18", "2023-04-19", "2023-04-20", "2023-04-21",
            6480, 6720, 7320, 8040, 480, 240, 600, 720),
        // deleting three day time entry
        Arguments.of("51a0a8eb-5972-406b-a539-4f4793ec3cb9",
            LocalDate.of(2023, 4, 20),
            "2023-04-18T18:00:00+00:00",
            "2023-04-20T06:00:00+00:00",
            "2023-04-18", "2023-04-19", "2023-04-20", "2023-04-21",
            6480, 6960, 7200, 7920, 480, 480, 240, 720)
    );
  }

  private static Stream<Arguments> nightHoursTestData() {
    return Stream.of(
        // deleting one day time entry
        Arguments.of("aed8cfb5-c82a-4fdf-9534-2170d0af14f8",
            LocalDate.of(2023, 4, 18),
            "2023-04-18T00:00:00+01:00",
            "2023-04-18T06:00:00+01:00",
            "2023-04-18", "2023-04-19", "2023-04-20", "2023-04-21",
            6120, 6660, 7020, 7020, 120, 540, 360, 0),
        // deleting two day time entry
        Arguments.of("e7d85e42-f0fb-4e2a-8211-874e27d1e888",
            LocalDate.of(2023, 4, 19),
            "2023-04-18T22:00:00+01:00",
            "2023-04-19T02:00:00+01:00",
            "2023-04-18", "2023-04-19", "2023-04-20", "2023-04-21",
            6420, 6840, 7200, 7200, 420, 420, 360, 0),
        // deleting three day time entry
        Arguments.of("7ea794b4-d87f-42c9-a534-187291c168ac",
            LocalDate.of(2023, 4, 20),
            "2023-04-18T22:00:00+01:00",
            "2023-04-20T07:00:00+01:00",
            "2023-04-18", "2023-04-19", "2023-04-20", "2023-04-21",
            6420, 6540, 6540, 6540, 420, 120, 0, 0)
    );
  }

  @ParameterizedTest
  @MethodSource("annualTargetHoursTestData")
  void calculate_annualTargetHours_returnUpdateAccruals(String timeEntryId,
                                                        LocalDate referenceDate,
                                                        String shiftStartTime,
                                                        String shiftEndTime,
                                                        String expectedDate1,
                                                        String expectedDate2,
                                                        String expectedDate3,
                                                        String expectedDate4,
                                                        Integer expectedCumulativeTotal1,
                                                        Integer expectedCumulativeTotal2,
                                                        Integer expectedCumulativeTotal3,
                                                        Integer expectedCumulativeTotal4,
                                                        Integer expectedContributionsTotal1,
                                                        Integer expectedContributionsTotal2,
                                                        Integer expectedContributionsTotal3,
                                                        Integer expectedContributionsTotal4)
      throws IOException {

    List<AccrualModule> accrualModules = List.of(new AnnualTargetHoursAccrualModule());
    ContributionsHandler contributionsHandler = new ContributionsHandler(accrualModules);
    balanceCalculator = new BalanceCalculator(accrualsService, contributionsHandler);

    TimeEntry timeEntry = CommonUtils.createTimeEntry(timeEntryId, PERSON_ID, shiftStartTime,
        shiftEndTime);

    String tenantId = timeEntry.getTenantId();

    when(accrualsService.getApplicableAgreement(tenantId, PERSON_ID, referenceDate))
        .thenReturn(loadObjectFromFile("data/agreement.json", Agreement.class));

    when(accrualsService.getImpactedAccruals(tenantId, PERSON_ID, timeEntryId,
        timeEntry.getActualStartTime().toLocalDate(), timeEntry.getActualEndTime().toLocalDate()))
        .thenReturn(loadAccrualsFromFile("data/accruals_annualTargetHoursDeleteAction.json"));

    List<Accrual> accruals = balanceCalculator.calculate(timeEntry, KafkaAction.DELETE);

    assertThat(accruals).hasSize(4);

    assertTypeAndDateAndTotals(accruals.get(0), ANNUAL_TARGET_HOURS, expectedDate1,
        expectedContributionsTotal1, expectedCumulativeTotal1);

    assertTypeAndDateAndTotals(accruals.get(1), ANNUAL_TARGET_HOURS, expectedDate2,
        expectedContributionsTotal2, expectedCumulativeTotal2);

    assertTypeAndDateAndTotals(accruals.get(2), ANNUAL_TARGET_HOURS, expectedDate3,
        expectedContributionsTotal3, expectedCumulativeTotal3);

    assertTypeAndDateAndTotals(accruals.get(3), ANNUAL_TARGET_HOURS, expectedDate4,
        expectedContributionsTotal4, expectedCumulativeTotal4);
  }

  @ParameterizedTest
  @MethodSource("nightHoursTestData")
  void calculate_nightHours_returnUpdatedAccruals(String timeEntryId,
                                                  LocalDate referenceDate,
                                                  String shiftStartTime,
                                                  String shiftEndTime,
                                                  String expectedDate1,
                                                  String expectedDate2,
                                                  String expectedDate3,
                                                  String expectedDate4,
                                                  Integer expectedCumulativeTotal1,
                                                  Integer expectedCumulativeTotal2,
                                                  Integer expectedCumulativeTotal3,
                                                  Integer expectedCumulativeTotal4,
                                                  Integer expectedContributionsTotal1,
                                                  Integer expectedContributionsTotal2,
                                                  Integer expectedContributionsTotal3,
                                                  Integer expectedContributionsTotal4)
      throws IOException {

    List<AccrualModule> accrualModules = List.of(new NightHoursAccrualModule());
    ContributionsHandler contributionsHandler = new ContributionsHandler(accrualModules);
    balanceCalculator = new BalanceCalculator(accrualsService, contributionsHandler);

    TimeEntry timeEntry = CommonUtils.createTimeEntry(timeEntryId, PERSON_ID, shiftStartTime,
        shiftEndTime);

    String tenantId = timeEntry.getTenantId();

    when(accrualsService.getApplicableAgreement(tenantId, PERSON_ID, referenceDate))
        .thenReturn(loadObjectFromFile("data/agreement.json", Agreement.class));

    when(accrualsService.getImpactedAccruals(tenantId, PERSON_ID, timeEntryId,
        timeEntry.getActualStartTime().toLocalDate(), timeEntry.getActualEndTime().toLocalDate()))
        .thenReturn(loadAccrualsFromFile("data/accruals_nightHoursDeleteAction.json"));

    List<Accrual> accruals = balanceCalculator.calculate(timeEntry, KafkaAction.DELETE);

    assertThat(accruals).hasSize(4);

    assertTypeAndDateAndTotals(accruals.get(0), NIGHT_HOURS, expectedDate1,
        expectedContributionsTotal1, expectedCumulativeTotal1);

    assertTypeAndDateAndTotals(accruals.get(1), NIGHT_HOURS, expectedDate2,
        expectedContributionsTotal2, expectedCumulativeTotal2);

    assertTypeAndDateAndTotals(accruals.get(2), NIGHT_HOURS, expectedDate3,
        expectedContributionsTotal3, expectedCumulativeTotal3);

    assertTypeAndDateAndTotals(accruals.get(3), NIGHT_HOURS, expectedDate4,
        expectedContributionsTotal4, expectedCumulativeTotal4);
  }


}