package uk.gov.homeoffice.digital.sas.balancecalculator;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.enums.AccrualType.ANNUAL_TARGET_HOURS;
import static uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.enums.AccrualType.NIGHT_HOURS;
import static uk.gov.homeoffice.digital.sas.balancecalculator.testutils.CommonUtils.createTimeEntry;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Accrual;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.enums.AccrualType;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.timecard.TimeEntry;
import uk.gov.homeoffice.digital.sas.kafka.message.KafkaAction;

@SpringBootTest
@AutoConfigureWireMock(port = 9999)
class BalanceCalculatorCreateActionIntegrationTest {

  private static final String TIME_ENTRY_ID = "7f000001-879e-1b02-8187-9ef1640f0003";
  private static final String TENANT_ID = "52a8188b-d41e-6768-19e9-09938016342f";
  private static final String PERSON_ID = "0936e7a6-2b2e-1696-2546-5dd25dcae6a0";

  @Autowired
  private BalanceCalculator balanceCalculator;

  @BeforeEach
  void setup() {
  }

  @Test
  void calculate_annualTargetHoursGmtToBst_contributionsAndCumulativeTotalsAsExpected() {

    TimeEntry timeEntry = createTimeEntry(TIME_ENTRY_ID,
        TENANT_ID,
        PERSON_ID,
        "2023-03-26T00:59:00+00:00",
        "2023-03-26T03:59:00+01:00");

    List<Accrual> accruals = balanceCalculator.calculate(timeEntry, KafkaAction.CREATE);

    assertThat(accruals).hasSize(8);

    assertTypeAndTotals(accruals.get(0), ANNUAL_TARGET_HOURS, 600, 6600);
    assertTypeAndTotals(accruals.get(1), ANNUAL_TARGET_HOURS, 480, 7080);
    assertTypeAndTotals(accruals.get(2), ANNUAL_TARGET_HOURS, 240, 7320);
    assertTypeAndTotals(accruals.get(3), ANNUAL_TARGET_HOURS, 720, 8040);
  }

  @Test
  void calculate_nightHoursGmtToBst_contributionsAndCumulativeTotalsAsExpected() {

    TimeEntry timeEntry = createTimeEntry(TIME_ENTRY_ID,
        TENANT_ID,
        PERSON_ID,
        "2023-03-26T00:59:00+00:00",
        "2023-03-26T03:59:00+01:00");

    List<Accrual> accruals = balanceCalculator.calculate(timeEntry, KafkaAction.CREATE);

    assertThat(accruals).hasSize(8);

    assertTypeAndTotals(accruals.get(4), NIGHT_HOURS, 120, 1120);
    assertTypeAndTotals(accruals.get(5), NIGHT_HOURS, 0, 1120);
    assertTypeAndTotals(accruals.get(6), NIGHT_HOURS, 0, 1120);
    assertTypeAndTotals(accruals.get(7), NIGHT_HOURS, 0, 1120);
  }

  @Test
  void calculate_annualTargetHoursBstToGmt_contributionsAndCumulativeTotalsAsExpected() {

    TimeEntry timeEntry = createTimeEntry(TIME_ENTRY_ID,
        TENANT_ID,
        PERSON_ID,
        "2023-10-29T01:59:00+01:00",
        "2023-10-29T02:59:00+00:00");

    List<Accrual> accruals = balanceCalculator.calculate(timeEntry, KafkaAction.CREATE);

    assertThat(accruals).hasSize(8);

    assertTypeAndTotals(accruals.get(0), ANNUAL_TARGET_HOURS, 600, 6600);
    assertTypeAndTotals(accruals.get(1), ANNUAL_TARGET_HOURS, 480, 7080);
    assertTypeAndTotals(accruals.get(2), ANNUAL_TARGET_HOURS, 240, 7320);
    assertTypeAndTotals(accruals.get(3), ANNUAL_TARGET_HOURS, 720, 8040);
  }

  @Test
  void calculate_nightHoursBstToGmt_contributionsAndCumulativeTotalsAsExpected() {

    TimeEntry timeEntry = createTimeEntry(TIME_ENTRY_ID,
        TENANT_ID,
        PERSON_ID,
        "2023-10-29T01:59:00+01:00",
        "2023-10-29T02:59:00+00:00");

    List<Accrual> accruals = balanceCalculator.calculate(timeEntry, KafkaAction.CREATE);

    assertThat(accruals).hasSize(8);

    assertTypeAndTotals(accruals.get(4), NIGHT_HOURS, 120, 1120);
    assertTypeAndTotals(accruals.get(5), NIGHT_HOURS, 0, 1120);
    assertTypeAndTotals(accruals.get(6), NIGHT_HOURS, 0, 1120);
    assertTypeAndTotals(accruals.get(7), NIGHT_HOURS, 0, 1120);
  }

  @Test
  void calculate_annualTargetHoursTimeEntryTwoDaysSpan_contributionsAndCumulativeTotalsAsExpected() {

    TimeEntry timeEntry = createTimeEntry(TIME_ENTRY_ID,
        TENANT_ID,
        PERSON_ID,
        "2023-04-22T22:00:00+01:00",
        "2023-04-23T07:00:00+01:00");

    List<Accrual> accruals = balanceCalculator.calculate(timeEntry, KafkaAction.CREATE);

    assertThat(accruals).hasSize(6);

    assertTypeAndTotals(accruals.get(0), ANNUAL_TARGET_HOURS, 120, 8160);
    assertTypeAndTotals(accruals.get(1), ANNUAL_TARGET_HOURS, 420, 8580);
    assertTypeAndTotals(accruals.get(2), ANNUAL_TARGET_HOURS, 0, 8580);
  }

  @Test
  void calculate_nightHoursTimeEntryTwoDaysSpan_contributionsAndCumulativeTotalsAsExpected() {

    TimeEntry timeEntry = createTimeEntry(TIME_ENTRY_ID,
        TENANT_ID,
        PERSON_ID,
        "2023-04-22T22:00:00+01:00",
        "2023-04-23T07:00:00+01:00");

    List<Accrual> accruals = balanceCalculator.calculate(timeEntry, KafkaAction.CREATE);

    assertThat(accruals).hasSize(6);

    assertTypeAndTotals(accruals.get(3), NIGHT_HOURS, 60, 1060);
    assertTypeAndTotals(accruals.get(4), NIGHT_HOURS, 360, 1420);
    assertTypeAndTotals(accruals.get(5), NIGHT_HOURS, 120, 1540);
  }

  private void assertTypeAndTotals(Accrual accrual, AccrualType expectedAccrualType,
                                   int expectedContributionTotal,
                                   int expectedCumulativeTotal) {
    assertThat(accrual.getAccrualType()).isEqualTo(expectedAccrualType);
    assertThat(accrual.getContributions().getTotal())
        .usingComparator(BigDecimal::compareTo)
        .isEqualTo(BigDecimal.valueOf(expectedContributionTotal));
    assertThat(accrual.getCumulativeTotal())
        .usingComparator(BigDecimal::compareTo)
        .isEqualTo(BigDecimal.valueOf(expectedCumulativeTotal));
  }
}