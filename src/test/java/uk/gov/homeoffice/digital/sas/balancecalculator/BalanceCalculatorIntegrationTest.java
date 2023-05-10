package uk.gov.homeoffice.digital.sas.balancecalculator;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.homeoffice.digital.sas.balancecalculator.testutils.CommonUtils.createTimeEntry;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Accrual;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.timecard.TimeEntry;

@SpringBootTest
@AutoConfigureWireMock(port = 9999)
class BalanceCalculatorIntegrationTest {

  private static final String TIME_ENTRY_ID = "7f000001-879e-1b02-8187-9ef1640f0003";
  private static final String TENANT_ID = "52a8188b-d41e-6768-19e9-09938016342f";
  private static final String PERSON_ID = "0936e7a6-2b2e-1696-2546-5dd25dcae6a0";

  @Autowired
  private BalanceCalculator balanceCalculator;

  @BeforeEach
  void setup() {
  }

  @Test
  void calculate_endToEnd_contributionsAndCumulativeTotalsAsExpected() {

    ZonedDateTime startTime = ZonedDateTime.parse("2023-04-18T08:00:00+00:00");
    ZonedDateTime finishTime = startTime.plusHours(2);

    TimeEntry timeEntry = createTimeEntry(TIME_ENTRY_ID,
        TENANT_ID,
        PERSON_ID,
        startTime,
        finishTime);

    List<Accrual> accruals = balanceCalculator.calculate(timeEntry);

    assertThat(accruals).hasSize(4);

    assertThat(accruals.get(0).getContributions().getTotal())
        .usingComparator(BigDecimal::compareTo).isEqualTo(BigDecimal.valueOf(600));
    assertThat(accruals.get(0).getCumulativeTotal())
        .usingComparator(BigDecimal::compareTo).isEqualTo(BigDecimal.valueOf(6600));

    assertThat(accruals.get(1).getContributions().getTotal())
        .usingComparator(BigDecimal::compareTo).isEqualTo(BigDecimal.valueOf(600));
    assertThat(accruals.get(1).getCumulativeTotal())
        .usingComparator(BigDecimal::compareTo).isEqualTo(BigDecimal.valueOf(7200));

    assertThat(accruals.get(2).getContributions().getTotal())
        .usingComparator(BigDecimal::compareTo).isEqualTo(BigDecimal.valueOf(240));
    assertThat(accruals.get(2).getCumulativeTotal())
        .usingComparator(BigDecimal::compareTo).isEqualTo(BigDecimal.valueOf(7440));

    assertThat(accruals.get(3).getContributions().getTotal())
        .usingComparator(BigDecimal::compareTo).isEqualTo(BigDecimal.valueOf(720));
    assertThat(accruals.get(3).getCumulativeTotal())
        .usingComparator(BigDecimal::compareTo).isEqualTo(BigDecimal.valueOf(8160));
  }

  @Test
  void calculate_accrualsDays_whenTimeEntryHasTwoDaysSpan() {

    ZonedDateTime startTime = ZonedDateTime.parse("2023-04-22T22:00:00+00:00");
    ZonedDateTime finishTime = ZonedDateTime.parse("2023-04-23T06:00:00+00:00");

    TimeEntry timeEntry = createTimeEntry(TIME_ENTRY_ID,
        TENANT_ID,
        PERSON_ID,
        startTime,
        finishTime);

    List<Accrual> accruals = balanceCalculator.calculate(timeEntry);

    assertThat(accruals).hasSize(3);
    assertThat(accruals.get(0).getContributions().getTotal())
        .usingComparator(BigDecimal::compareTo).isEqualTo(BigDecimal.valueOf(60));
    assertThat(accruals.get(0).getCumulativeTotal())
        .usingComparator(BigDecimal::compareTo).isEqualTo(BigDecimal.valueOf(8100));
    assertThat(accruals.get(1).getContributions().getTotal())
        .usingComparator(BigDecimal::compareTo).isEqualTo(BigDecimal.valueOf(420));
    assertThat(accruals.get(1).getCumulativeTotal())
        .usingComparator(BigDecimal::compareTo).isEqualTo(BigDecimal.valueOf(8520));
    assertThat(accruals.get(2).getContributions().getTotal())
        .usingComparator(BigDecimal::compareTo).isEqualTo(BigDecimal.valueOf(0));
    assertThat(accruals.get(2).getCumulativeTotal())
        .usingComparator(BigDecimal::compareTo).isEqualTo(BigDecimal.valueOf(8520));
  }
}