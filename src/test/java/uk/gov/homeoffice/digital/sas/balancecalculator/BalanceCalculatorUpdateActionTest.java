package uk.gov.homeoffice.digital.sas.balancecalculator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.homeoffice.digital.sas.balancecalculator.testutils.CommonUtils.loadAccrualsFromFile;
import static uk.gov.homeoffice.digital.sas.balancecalculator.testutils.CommonUtils.loadObjectFromFile;

import java.io.IOException;
import java.math.BigDecimal;
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
            BigDecimal.valueOf(6540), BigDecimal.valueOf(7140),
            BigDecimal.valueOf(7380), BigDecimal.valueOf(8100),
            BigDecimal.valueOf(540), BigDecimal.valueOf(600),
            BigDecimal.valueOf(240), BigDecimal.valueOf(720)),
        // updating one day time entry to become three day time entry
        Arguments.of("85cd140e-9eeb-4771-ab6c-6dea17fcfcbe",
            "2023-04-18T22:00:00+00:00",
            "2023-04-20T02:00:00+00:00",
            BigDecimal.valueOf(6420), BigDecimal.valueOf(8460),
            BigDecimal.valueOf(8880), BigDecimal.valueOf(9600),
            BigDecimal.valueOf(420), BigDecimal.valueOf(2040),
            BigDecimal.valueOf(420), BigDecimal.valueOf(720)),
        // updating two day time entry to one day time entry
        Arguments.of("e7d85e42-f0fb-4e2a-8211-874e27d1e888",
            "2023-04-18T14:00:00+00:00",
            "2023-04-18T15:00:00+00:00",
            BigDecimal.valueOf(6180), BigDecimal.valueOf(6420),
            BigDecimal.valueOf(6660), BigDecimal.valueOf(7380),
            BigDecimal.valueOf(180), BigDecimal.valueOf(240),
            BigDecimal.valueOf(240), BigDecimal.valueOf(720))
    );
  }

  @ParameterizedTest
  @MethodSource("annualTargetHoursTestData")
  void calculate_annualTargetHours_returnUpdateAccruals(String timeEntryId,
                                                        String shiftStartTime,
                                                        String shiftEndTime,
                                                        BigDecimal expectedCumulativeTotal1,
                                                        BigDecimal expectedCumulativeTotal2,
                                                        BigDecimal expectedCumulativeTotal3,
                                                        BigDecimal expectedCumulativeTotal4,
                                                        BigDecimal expectedContributionsTotal1,
                                                        BigDecimal expectedContributionsTotal2,
                                                        BigDecimal expectedContributionsTotal3,
                                                        BigDecimal expectedContributionsTotal4)
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

    assertCumulativeTotal(accruals.get(0), expectedCumulativeTotal1);
    assertCumulativeTotal(accruals.get(1), expectedCumulativeTotal2);
    assertCumulativeTotal(accruals.get(2), expectedCumulativeTotal3);
    assertCumulativeTotal(accruals.get(3), expectedCumulativeTotal4);

    assertContributionsTotal(accruals.get(0), expectedContributionsTotal1);
    assertContributionsTotal(accruals.get(1), expectedContributionsTotal2);
    assertContributionsTotal(accruals.get(2), expectedContributionsTotal3);
    assertContributionsTotal(accruals.get(3), expectedContributionsTotal4);

  }

  private void assertContributionsTotal(Accrual accrual, BigDecimal expectedContributionsTotal) {
    assertThat(accrual.getContributions().getTotal()).usingComparator(
        BigDecimal::compareTo).isEqualTo(expectedContributionsTotal);
  }


  private void assertCumulativeTotal(Accrual accrual, BigDecimal expectedCumulativeTotal) {
    assertThat(accrual.getCumulativeTotal()).usingComparator(
            BigDecimal::compareTo)
        .isEqualTo(expectedCumulativeTotal);
  }


}