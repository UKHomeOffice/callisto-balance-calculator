package uk.gov.homeoffice.digital.sas.balancecalculator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.homeoffice.digital.sas.balancecalculator.testutils.CommonUtils.loadAccrualsFromFile;
import static uk.gov.homeoffice.digital.sas.balancecalculator.testutils.CommonUtils.loadObjectFromFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
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

  private static final String SHIFT_START_TIME = "2023-04-18T08:00:00+00:00";
  private static final String SHIFT_END_TIME = "2023-04-18T10:00:00+00:00";

  private static final LocalDate ACCRUAL_DATE =
      LocalDate.from(ZonedDateTime.parse(SHIFT_START_TIME));
  private static final String TIME_ENTRY_ID = "e7d85e42-f0fb-4e2a-8211-874e27d1e888";
  private static final String PERSON_ID = "0936e7a6-2b2e-1696-2546-5dd25dcae6a0";
  private static final LocalDate AGREEMENT_START_DATE = LocalDate.of(2023, 4, 1);
  private static final LocalDate AGREEMENT_END_DATE = LocalDate.of(2024, 3, 31);

  private static final KafkaAction kafkaAction = KafkaAction.UPDATE;

  private List<AccrualModule> accrualModules;

  @Mock
  private AccrualsService accrualsService;

  private BalanceCalculator balanceCalculator;

  private static Stream<Arguments> annualTargetHoursTestData() {
    return Stream.of(
        // updating two day time entry to one day time entry
        Arguments.of("e7d85e42-f0fb-4e2a-8211-874e27d1e888",
            "2023-04-18T14:00:00+00:00",
            "2023-04-18T15:00:00+00:00",
            BigDecimal.valueOf(6180), BigDecimal.valueOf(6420),
            BigDecimal.valueOf(6660), BigDecimal.valueOf(7380))
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
                                                        BigDecimal expectedCumulativeTotal4)
      throws IOException {

    accrualModules = List.of(new AnnualTargetHoursAccrualModule());
    ContributionsHandler contributionsHandler = new ContributionsHandler(accrualModules);
    balanceCalculator = new BalanceCalculator(accrualsService, contributionsHandler);

    TimeEntry timeEntry = CommonUtils.createTimeEntry(timeEntryId, PERSON_ID, shiftStartTime,
        shiftEndTime);

    String tenantId = timeEntry.getTenantId();

    when(accrualsService.getApplicableAgreement(tenantId, PERSON_ID,
        timeEntry.getActualEndTime().toLocalDate()))
        .thenReturn(loadObjectFromFile("data/agreement.json", Agreement.class));

    when(accrualsService.getImpactedAccruals(tenantId, timeEntryId,
        timeEntry.getActualStartTime().toLocalDate(),AGREEMENT_END_DATE))
        .thenReturn(loadAccrualsFromFile("data/accruals_annualTargetHours.json"));

    List<Accrual> accruals = balanceCalculator.calculate(timeEntry, kafkaAction);

    assertThat(accruals).hasSize(4);

    assertCumulativeTotal(accruals.get(0), expectedCumulativeTotal1);
    assertCumulativeTotal(accruals.get(1), expectedCumulativeTotal2);
    assertCumulativeTotal(accruals.get(2), expectedCumulativeTotal3);
    assertCumulativeTotal(accruals.get(3), expectedCumulativeTotal4);
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