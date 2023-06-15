package uk.gov.homeoffice.digital.sas.balancecalculator;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.enums.AccrualType.ANNUAL_TARGET_HOURS;
import static uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.enums.AccrualType.NIGHT_HOURS;
import static uk.gov.homeoffice.digital.sas.balancecalculator.testutils.CommonUtils.assertTypeAndDateAndTotals;
import static uk.gov.homeoffice.digital.sas.balancecalculator.testutils.CommonUtils.createTimeEntry;

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
  private BalanceCalculator balanceCalculator;

  @Test
  void calculate_deleteAnnualTargetHoursOneDayTimeEntry_contributionsAndCumulativeTotalsAsExpected() {

    TimeEntry timeEntry = createTimeEntry("85cd140e-9eeb-4771-ab6c-6dea17fcfcba",
        TENANT_ID,
        PERSON_ID,
        "2023-10-30T05:00:00+00:00",
        "2023-10-30T07:00+00:00");

    List<Accrual> accruals = balanceCalculator.calculate(timeEntry, KafkaAction.DELETE);

    assertThat(accruals).hasSize(8);

    assertTypeAndDateAndTotals(accruals.get(0), ANNUAL_TARGET_HOURS, "2023-10-30", 480, 6960);
    assertTypeAndDateAndTotals(accruals.get(1), ANNUAL_TARGET_HOURS, "2023-10-31", 240, 7200);
    assertTypeAndDateAndTotals(accruals.get(2), ANNUAL_TARGET_HOURS, "2023-11-01", 720, 7920);
    assertTypeAndDateAndTotals(accruals.get(3), ANNUAL_TARGET_HOURS, "2023-11-02", 120, 8040);
  }

  @Test
  void calculate_deleteNightHoursOneDayTimeEntry_contributionsAndCumulativeTotalsAsExpected() {

    TimeEntry timeEntry = createTimeEntry("85cd140e-9eeb-4771-ab6c-6dea17fcfcba",
        TENANT_ID,
        PERSON_ID,
        "2023-10-30T05:00:00+00:00",
        "2023-10-30T07:00+00:00");

    List<Accrual> accruals = balanceCalculator.calculate(timeEntry, KafkaAction.DELETE);

    assertThat(accruals).hasSize(8);

    assertTypeAndDateAndTotals(accruals.get(4), NIGHT_HOURS, "2023-10-30", 0, 1000);
    assertTypeAndDateAndTotals(accruals.get(5), NIGHT_HOURS, "2023-10-31", 60, 1060);
    assertTypeAndDateAndTotals(accruals.get(6), NIGHT_HOURS, "2023-11-01", 0, 1060);
    assertTypeAndDateAndTotals(accruals.get(7), NIGHT_HOURS, "2023-11-02", 0, 1060);
  }

  @Test
  void calculate_deleteAnnualTargetHoursTwoDaysSpan_contributionsAndCumulativeTotalsAsExpected() {

    TimeEntry timeEntry = createTimeEntry(TIME_ENTRY_ID,
        TENANT_ID,
        PERSON_ID,
        "2023-04-22T23:00:00+01:00",
        "2023-04-23T07:00:00+01:00");

    List<Accrual> accruals = balanceCalculator.calculate(timeEntry, KafkaAction.DELETE);

    assertThat(accruals).hasSize(6);

    assertTypeAndDateAndTotals(accruals.get(0), ANNUAL_TARGET_HOURS, "2023-04-22", 0, 8040);
    assertTypeAndDateAndTotals(accruals.get(1), ANNUAL_TARGET_HOURS, "2023-04-23", 120, 8160);
    assertTypeAndDateAndTotals(accruals.get(2), ANNUAL_TARGET_HOURS, "2023-04-24", 300, 8460);
  }

  @Test
  void calculate_deleteNightHoursTwoDaysSpan_contributionsAndCumulativeTotalsAsExpected() {

    TimeEntry timeEntry = createTimeEntry(TIME_ENTRY_ID,
        TENANT_ID,
        PERSON_ID,
        "2023-04-22T23:00:00+01:00",
        "2023-04-23T07:00:00+01:00");

    List<Accrual> accruals = balanceCalculator.calculate(timeEntry, KafkaAction.DELETE);

    assertThat(accruals).hasSize(6);

    assertTypeAndDateAndTotals(accruals.get(3), NIGHT_HOURS, "2023-04-22", 0, 1000);
    assertTypeAndDateAndTotals(accruals.get(4), NIGHT_HOURS, "2023-04-23", 60, 1060);
    assertTypeAndDateAndTotals(accruals.get(5), NIGHT_HOURS, "2023-04-24", 120, 1180);
  }

}
