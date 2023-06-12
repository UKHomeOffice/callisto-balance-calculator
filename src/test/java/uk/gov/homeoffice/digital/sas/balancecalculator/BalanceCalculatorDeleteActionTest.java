package uk.gov.homeoffice.digital.sas.balancecalculator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.homeoffice.digital.sas.balancecalculator.testutils.CommonUtils.loadAccrualsFromFile;
import static uk.gov.homeoffice.digital.sas.balancecalculator.testutils.CommonUtils.loadObjectFromFile;

import java.io.IOException;
import java.math.BigDecimal;
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
            BigDecimal.valueOf(6720), BigDecimal.valueOf(7320),
            BigDecimal.valueOf(7920), BigDecimal.valueOf(8640)),
        // deleting two day time entry
        Arguments.of("e7d85e42-f0fb-4e2a-8211-874e27d1e888",
            LocalDate.of(2023, 4, 19),
            "2023-04-18T18:00:00+00:00",
            "2023-04-19T06:00:00+00:00",
            BigDecimal.valueOf(6480), BigDecimal.valueOf(6720),
            BigDecimal.valueOf(7320), BigDecimal.valueOf(8040)),
        // deleting three day time entry
        Arguments.of("51a0a8eb-5972-406b-a539-4f4793ec3cb9",
            LocalDate.of(2023, 4, 20),
            "2023-04-18T18:00:00+00:00",
            "2023-04-20T06:00:00+00:00",
            BigDecimal.valueOf(6480), BigDecimal.valueOf(6960),
            BigDecimal.valueOf(7200), BigDecimal.valueOf(7920))
    );
  }

  private static Stream<Arguments> nightHoursTestData() {
    return Stream.of(
        // deleting one day time entry
        Arguments.of("aed8cfb5-c82a-4fdf-9534-2170d0af14f8",
            LocalDate.of(2023, 4, 18),
            "2023-04-18T00:00:00+01:00",
            "2023-04-18T06:00:00+01:00",
            BigDecimal.valueOf(6120), BigDecimal.valueOf(6660),
            BigDecimal.valueOf(7020), BigDecimal.valueOf(7020)),
        // deleting two day time entry
        Arguments.of("e7d85e42-f0fb-4e2a-8211-874e27d1e888",
            LocalDate.of(2023, 4, 19),
            "2023-04-18T22:00:00+01:00",
            "2023-04-19T02:00:00+01:00",
            BigDecimal.valueOf(6420), BigDecimal.valueOf(6840),
            BigDecimal.valueOf(7200), BigDecimal.valueOf(7200)),
        // deleting three day time entry
        Arguments.of("7ea794b4-d87f-42c9-a534-187291c168ac",
            LocalDate.of(2023, 4, 20),
            "2023-04-18T22:00:00+01:00",
            "2023-04-20T07:00:00+01:00",
            BigDecimal.valueOf(6420), BigDecimal.valueOf(6540),
            BigDecimal.valueOf(6540), BigDecimal.valueOf(6540))
    );
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

    assertCumulativeTotal(accruals.get(0), expectedCumulativeTotal1);
    assertCumulativeTotal(accruals.get(1), expectedCumulativeTotal2);
    assertCumulativeTotal(accruals.get(2), expectedCumulativeTotal3);
    assertCumulativeTotal(accruals.get(3), expectedCumulativeTotal4);
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

}