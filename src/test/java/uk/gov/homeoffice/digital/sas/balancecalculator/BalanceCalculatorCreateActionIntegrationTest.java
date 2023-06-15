package uk.gov.homeoffice.digital.sas.balancecalculator;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.enums.AccrualType.ANNUAL_TARGET_HOURS;
import static uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.enums.AccrualType.NIGHT_HOURS;
import static uk.gov.homeoffice.digital.sas.balancecalculator.testutils.CommonUtils.assertTypeAndDateAndTotals;
import static uk.gov.homeoffice.digital.sas.balancecalculator.testutils.CommonUtils.createTimeEntry;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Accrual;
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

    assertTypeAndDateAndTotals(accruals.get(0), ANNUAL_TARGET_HOURS,
        LocalDate.of(2023, 3, 26),
        600, 6600);
    assertTypeAndDateAndTotals(accruals.get(1), ANNUAL_TARGET_HOURS,
        LocalDate.of(2023, 3, 27),
        480, 7080);
    assertTypeAndDateAndTotals(accruals.get(2), ANNUAL_TARGET_HOURS,
        LocalDate.of(2023, 3, 28),
        240, 7320);
    assertTypeAndDateAndTotals(accruals.get(3), ANNUAL_TARGET_HOURS,
        LocalDate.of(2023, 3, 29),
        720, 8040);
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

    assertTypeAndDateAndTotals(accruals.get(4), NIGHT_HOURS,
        LocalDate.of(2023, 3, 26),
        120, 1120);
    assertTypeAndDateAndTotals(accruals.get(5), NIGHT_HOURS,
        LocalDate.of(2023, 3, 27),
        0, 1120);
    assertTypeAndDateAndTotals(accruals.get(6), NIGHT_HOURS,
        LocalDate.of(2023, 3, 28),0, 1120);
    assertTypeAndDateAndTotals(accruals.get(7), NIGHT_HOURS,
        LocalDate.of(2023, 3, 29),
        0, 1120);
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

    assertTypeAndDateAndTotals(accruals.get(0), ANNUAL_TARGET_HOURS,
        LocalDate.of(2023, 10, 29),
        600, 6600);
    assertTypeAndDateAndTotals(accruals.get(1), ANNUAL_TARGET_HOURS,
        LocalDate.of(2023, 10, 30),
        480, 7080);
    assertTypeAndDateAndTotals(accruals.get(2), ANNUAL_TARGET_HOURS,
        LocalDate.of(2023, 10, 31),
        240, 7320);
    assertTypeAndDateAndTotals(accruals.get(3), ANNUAL_TARGET_HOURS,
        LocalDate.of(2023, 11, 1),
        720, 8040);
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

    assertTypeAndDateAndTotals(accruals.get(4), NIGHT_HOURS,
        LocalDate.of(2023, 10, 29),120, 1120);
    assertTypeAndDateAndTotals(accruals.get(5), NIGHT_HOURS,
        LocalDate.of(2023, 10, 30),
        0, 1120);
    assertTypeAndDateAndTotals(accruals.get(6), NIGHT_HOURS,
        LocalDate.of(2023, 10, 31),
        0, 1120);
    assertTypeAndDateAndTotals(accruals.get(7), NIGHT_HOURS,
        LocalDate.of(2023, 11, 1),
        0, 1120);
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

    assertTypeAndDateAndTotals(accruals.get(0), ANNUAL_TARGET_HOURS,
        LocalDate.of(2023, 4, 22),120, 8160);
    assertTypeAndDateAndTotals(accruals.get(1), ANNUAL_TARGET_HOURS,
        LocalDate.of(2023, 4, 23),420, 8580);
    assertTypeAndDateAndTotals(accruals.get(2), ANNUAL_TARGET_HOURS,
        LocalDate.of(2023, 4, 24),
        0, 8580);
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

    assertTypeAndDateAndTotals(accruals.get(3), NIGHT_HOURS,
        LocalDate.of(2023, 4, 22),
        60, 1060);
    assertTypeAndDateAndTotals(accruals.get(4), NIGHT_HOURS,
        LocalDate.of(2023, 4, 23),
        360, 1420);
    assertTypeAndDateAndTotals(accruals.get(5), NIGHT_HOURS,
        LocalDate.of(2023, 4, 24),
        120, 1540);
  }

}