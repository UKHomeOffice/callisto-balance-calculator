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
  void calculate_endToEnd_contributionsAndCumulativeTotalsAsExpected() {

    ZonedDateTime startTime = ZonedDateTime.parse("2023-10-29T01:59:00+01:00");
    ZonedDateTime finishTime = ZonedDateTime.parse("2023-10-29T02:59:00+00:00");

    TimeEntry timeEntry = createTimeEntry(TIME_ENTRY_ID,
        TENANT_ID,
        PERSON_ID,
        startTime,
        finishTime);

    List<Accrual> accruals = balanceCalculator.calculate(timeEntry, KafkaAction.DELETE);

    assertThat(accruals).hasSize(4);

    assertThat(accruals.get(0).getContributions().getTotal())
        .usingComparator(BigDecimal::compareTo).isEqualTo(BigDecimal.valueOf(360));
    assertThat(accruals.get(0).getCumulativeTotal())
        .usingComparator(BigDecimal::compareTo).isEqualTo(BigDecimal.valueOf(6360));

    assertThat(accruals.get(1).getContributions().getTotal())
        .usingComparator(BigDecimal::compareTo).isEqualTo(BigDecimal.valueOf(600));
    assertThat(accruals.get(1).getCumulativeTotal())
        .usingComparator(BigDecimal::compareTo).isEqualTo(BigDecimal.valueOf(6960));

    assertThat(accruals.get(2).getContributions().getTotal())
        .usingComparator(BigDecimal::compareTo).isEqualTo(BigDecimal.valueOf(240));
    assertThat(accruals.get(2).getCumulativeTotal())
        .usingComparator(BigDecimal::compareTo).isEqualTo(BigDecimal.valueOf(7200));

    assertThat(accruals.get(3).getContributions().getTotal())
        .usingComparator(BigDecimal::compareTo).isEqualTo(BigDecimal.valueOf(720));
    assertThat(accruals.get(3).getCumulativeTotal())
        .usingComparator(BigDecimal::compareTo).isEqualTo(BigDecimal.valueOf(7920));
  }

  @Test
  void calculate_timeEntryHasTwoDaysSpan_contributionsAndCumulativeTotalsAsExpected() {

    ZonedDateTime startTime = ZonedDateTime.parse("2023-04-22T22:00:00+00:00");
    ZonedDateTime finishTime = ZonedDateTime.parse("2023-04-23T06:00:00+00:00");

    TimeEntry timeEntry = createTimeEntry(TIME_ENTRY_ID,
        TENANT_ID,
        PERSON_ID,
        startTime,
        finishTime);

    List<Accrual> accruals = balanceCalculator.calculate(timeEntry, KafkaAction.DELETE);

    assertThat(accruals).hasSize(3);
    assertThat(accruals.get(0).getContributions().getTotal())
        .usingComparator(BigDecimal::compareTo).isEqualTo(BigDecimal.valueOf(0));
    assertThat(accruals.get(0).getCumulativeTotal())
        .usingComparator(BigDecimal::compareTo).isEqualTo(BigDecimal.valueOf(8040));

    assertThat(accruals.get(1).getContributions().getTotal())
        .usingComparator(BigDecimal::compareTo).isEqualTo(BigDecimal.valueOf(0));
    assertThat(accruals.get(1).getCumulativeTotal())
        .usingComparator(BigDecimal::compareTo).isEqualTo(BigDecimal.valueOf(8040));

    assertThat(accruals.get(2).getContributions().getTotal())
        .usingComparator(BigDecimal::compareTo).isEqualTo(BigDecimal.valueOf(0));
    assertThat(accruals.get(2).getCumulativeTotal())
        .usingComparator(BigDecimal::compareTo).isEqualTo(BigDecimal.valueOf(8040));
  }
}
