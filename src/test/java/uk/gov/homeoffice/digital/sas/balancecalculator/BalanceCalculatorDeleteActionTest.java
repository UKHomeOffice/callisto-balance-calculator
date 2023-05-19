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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.OutputCaptureExtension;
import uk.gov.homeoffice.digital.sas.balancecalculator.client.RestClient;
import uk.gov.homeoffice.digital.sas.balancecalculator.handlers.ContributionsHandler;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Accrual;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Agreement;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.timecard.TimeEntry;
import uk.gov.homeoffice.digital.sas.balancecalculator.module.AccrualModule;
import uk.gov.homeoffice.digital.sas.balancecalculator.module.AnnualTargetHoursAccrualModule;
import uk.gov.homeoffice.digital.sas.balancecalculator.testutils.CommonUtils;
import uk.gov.homeoffice.digital.sas.kafka.message.KafkaAction;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class BalanceCalculatorDeleteActionTest {

  private static final String PERSON_ID = "0936e7a6-2b2e-1696-2546-5dd25dcae6a0";
  private static final LocalDate AGREEMENT_END_DATE = LocalDate.of(2024, 3, 31);
  private final List<AccrualModule> accrualModules = List.of(new AnnualTargetHoursAccrualModule());


  @Mock
  private RestClient restClient;

  private BalanceCalculator balanceCalculator;

  private ContributionsHandler contributionsHandler;

  @BeforeEach
  void setup() {
    contributionsHandler = new ContributionsHandler();
    balanceCalculator = new BalanceCalculator(restClient, accrualModules, contributionsHandler);
  }

  private static Stream<Arguments> annualTargetHoursTestData() {
    return Stream.of(
        // creating one day time entry
        Arguments.of("38e09687-5ae7-40d6-82b4-b022ae456bb1",
            LocalDate.of(2023, 4, 18),
            ZonedDateTime.parse("2023-04-18T08:00:00+00:00"),
            ZonedDateTime.parse("2023-04-18T10:00:00+00:00"),
            BigDecimal.valueOf(6720), BigDecimal.valueOf(7320),
            BigDecimal.valueOf(7920), BigDecimal.valueOf(8640)),
        // updating two day time entry
        Arguments.of("e7d85e42-f0fb-4e2a-8211-874e27d1e888",
            LocalDate.of(2023, 4, 19),
            ZonedDateTime.parse("2023-04-18T18:00:00+00:00"),
            ZonedDateTime.parse("2023-04-19T06:00:00+00:00"),
            BigDecimal.valueOf(6480), BigDecimal.valueOf(6720),
            BigDecimal.valueOf(7320), BigDecimal.valueOf(8040)),
        //updating three day time entry
        Arguments.of("51a0a8eb-5972-406b-a539-4f4793ec3cb9",
            LocalDate.of(2023, 4, 20),
            ZonedDateTime.parse("2023-04-18T18:00:00+00:00"),
            ZonedDateTime.parse("2023-04-20T06:00:00+00:00"),
            BigDecimal.valueOf(6480), BigDecimal.valueOf(6960),
            BigDecimal.valueOf(7200), BigDecimal.valueOf(7920))
    );
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
        .thenReturn(loadAccrualsFromFile("data/accruals_annualTargetHoursDeleteAction.json"));

    List<Accrual> accruals = balanceCalculator.calculate(timeEntry, KafkaAction.DELETE);

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

}