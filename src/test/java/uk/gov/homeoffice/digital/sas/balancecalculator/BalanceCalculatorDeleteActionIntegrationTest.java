package uk.gov.homeoffice.digital.sas.balancecalculator;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.homeoffice.digital.sas.balancecalculator.testutils.CommonUtils.createTimeEntry;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Accrual;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.timecard.TimeEntry;
import uk.gov.homeoffice.digital.sas.kafka.message.KafkaAction;

@SpringBootTest
@AutoConfigureWireMock(port = 9999)
class BalanceCalculatorDeleteActionIntegrationTest {

  private static final String TIME_ENTRY_ID = "a90a037a-a6c5-4fc0-9e72-a533c8c89c68";
  private static final String TENANT_ID = "52a8188b-d41e-6768-19e9-09938016342f";
  private static final String PERSON_ID = "b810a175-9b87-473c-94f3-9bcdf87f3a58";

  @Autowired
  private  BalanceCalculator balanceCalculator;

  @Test
  void calculate_deleteTimeEntryOneDay_contributionsAndCumulativeTotalsAsExpected() {

    ZonedDateTime startTime = ZonedDateTime.parse("2023-10-30T10:00:00+00:00");
    ZonedDateTime finishTime = ZonedDateTime.parse("2023-10-30T12:00+00:00");

    TimeEntry timeEntry = createTimeEntry("85cd140e-9eeb-4771-ab6c-6dea17fcfcba",
        TENANT_ID,
        PERSON_ID,
        startTime,
        finishTime);

    List<Accrual> accruals = balanceCalculator.calculate(timeEntry, KafkaAction.DELETE);

    assertThat(accruals).hasSize(4);

    assertTotals(accruals.get(0), 480, 6960);
    assertTotals(accruals.get(1), 240, 7200);
    assertTotals(accruals.get(2), 720, 7920);
    assertTotals(accruals.get(3), 120, 8040);
  }

  @Test
  void calculate_deleteTimeEntryTwoDaysSpan_contributionsAndCumulativeTotalsAsExpected() {

    ZonedDateTime startTime = ZonedDateTime.parse("2023-04-22T22:00:00+00:00");
    ZonedDateTime finishTime = ZonedDateTime.parse("2023-04-23T06:00:00+00:00");

    TimeEntry timeEntry = createTimeEntry(TIME_ENTRY_ID,
        TENANT_ID,
        PERSON_ID,
        startTime,
        finishTime);

    List<Accrual> accruals = balanceCalculator.calculate(timeEntry, KafkaAction.DELETE);

    assertThat(accruals).hasSize(3);
    assertTotals(accruals.get(0), 0, 8040);
    assertTotals(accruals.get(1), 120, 8160);
    assertTotals(accruals.get(2), 300, 8460);
  }

  private void assertTotals(Accrual accrual,
                                   int expectedContributionTotal,
                                   int expectedCumulativeTotal) {
    assertThat(accrual.getContributions().getTotal())
        .usingComparator(BigDecimal::compareTo)
        .isEqualTo(BigDecimal.valueOf(expectedContributionTotal));
    assertThat(accrual.getCumulativeTotal())
        .usingComparator(BigDecimal::compareTo)
        .isEqualTo(BigDecimal.valueOf(expectedCumulativeTotal));
  }
}
