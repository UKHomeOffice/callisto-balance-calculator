package uk.gov.homeoffice.digital.sas.balancecalculator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.enums.AccrualType.ANNUAL_TARGET_HOURS;
import static uk.gov.homeoffice.digital.sas.balancecalculator.testutils.CommonUtils.assertTypeAndDateAndTotals;
import static uk.gov.homeoffice.digital.sas.balancecalculator.testutils.CommonUtils.loadAccrualsFromFile;
import static uk.gov.homeoffice.digital.sas.balancecalculator.testutils.CommonUtils.loadObjectFromFile;

import java.io.IOException;
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
import uk.gov.homeoffice.digital.sas.balancecalculator.testutils.CommonUtils;
import uk.gov.homeoffice.digital.sas.kafka.message.KafkaAction;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class BalanceCalculatorUpdateActionTest {

  private static final String PERSON_ID = "0936e7a6-2b2e-1696-2546-5dd25dcae6a0";

  @Mock
  private AccrualsService accrualsService;

  private static Stream<Arguments> annualTargetHoursTestData() {
    return Stream.of(
        // updating one day time entry
        Arguments.of("85cd140e-9eeb-4771-ab6c-6dea17fcfcbe",
            "2023-04-18T09:00:00+00:00",
            "2023-04-18T12:00:00+00:00",
            "2023-04-18", "2023-04-19", "2023-04-20", "2023-04-21",
            6540, 7140, 7380, 8100, 540, 600, 240, 720),
        // updating one day time entry to become three day time entry
        Arguments.of("85cd140e-9eeb-4771-ab6c-6dea17fcfcbe",
            "2023-04-18T22:00:00+00:00",
            "2023-04-20T02:00:00+00:00",
            "2023-04-18", "2023-04-19", "2023-04-20", "2023-04-21",
            6420, 8460, 8880, 9600, 420, 2040, 420, 720,
        // updating two day time entry to one day time entry
        Arguments.of("e7d85e42-f0fb-4e2a-8211-874e27d1e888",
            "2023-04-18T14:00:00+00:00", "2023-04-18T15:00:00+00:00",
            "2023-04-18", "2023-04-19", "2023-04-20", "2023-04-21",
            6180, 6420, 6660, 7380, 180, 240, 240, 72))
    );
  }

  @ParameterizedTest
  @MethodSource("annualTargetHoursTestData")
  void calculate_annualTargetHours_returnUpdateAccruals(String timeEntryId,
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
    BalanceCalculator balanceCalculator = new BalanceCalculator(accrualsService,
        contributionsHandler);

    TimeEntry timeEntry = CommonUtils.createTimeEntry(timeEntryId, PERSON_ID, shiftStartTime,
        shiftEndTime);

    String tenantId = timeEntry.getTenantId();

    when(accrualsService.getApplicableAgreement(tenantId, PERSON_ID,
        timeEntry.getActualEndTime().toLocalDate()))
        .thenReturn(loadObjectFromFile("data/agreement.json", Agreement.class));

    when(accrualsService.getImpactedAccruals(tenantId, PERSON_ID, timeEntryId,
        timeEntry.getActualStartTime().toLocalDate(), timeEntry.getActualEndTime().toLocalDate()))
        .thenReturn(loadAccrualsFromFile("data/accruals_annualTargetHours.json"));

    List<Accrual> accruals = balanceCalculator.calculate(timeEntry, KafkaAction.UPDATE);

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
}