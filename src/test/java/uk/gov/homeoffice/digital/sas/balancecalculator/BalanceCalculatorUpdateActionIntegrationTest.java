package uk.gov.homeoffice.digital.sas.balancecalculator;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.enums.AccrualType.ANNUAL_TARGET_HOURS;
import static uk.gov.homeoffice.digital.sas.balancecalculator.testutils.CommonUtils.assertTypeAndDateAndTotals;
import static uk.gov.homeoffice.digital.sas.balancecalculator.testutils.CommonUtils.createTimeEntry;

import java.time.LocalDate;
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
class BalanceCalculatorUpdateActionIntegrationTest {

  private static final String TIME_ENTRY_ID = "a90a037a-a6c5-4fc0-9e72-a533c8c89c68";
  private static final String TENANT_ID = "52a8188b-d41e-6768-19e9-09938016342f";
  private static final String PERSON_ID = "b810a175-9b87-473c-94f3-9bcdf87f3a58";

  @Autowired
  private BalanceCalculator balanceCalculator;

  @Test
  void calculate_updateAthOneDayTimeEntry_contributionsAndCumulativeTotalsAsExpected() {

    TimeEntry timeEntry = createTimeEntry("85cd140e-9eeb-4771-ab6c-6dea17fcfcba",
        TENANT_ID,
        PERSON_ID,
        "2023-10-30T09:00+00:00",
        "2023-10-30T10:00+00:00");

    List<Accrual> accruals = balanceCalculator.calculate(timeEntry, KafkaAction.UPDATE);

    assertThat(accruals).hasSize(8);

    assertTypeAndDateAndTotals(accruals.get(0), ANNUAL_TARGET_HOURS,
        LocalDate.of(2023, 10, 30),
        540, 7020);
    assertTypeAndDateAndTotals(accruals.get(1), ANNUAL_TARGET_HOURS,
        LocalDate.of(2023, 10, 31),
        240, 7260);
    assertTypeAndDateAndTotals(accruals.get(2), ANNUAL_TARGET_HOURS,
        LocalDate.of(2023, 11, 1),
        720, 7980);
    assertTypeAndDateAndTotals(accruals.get(3), ANNUAL_TARGET_HOURS,
        LocalDate.of(2023, 11, 2),
        120, 8100);
  }

  @Test
  void calculate_updateAthTwoDaysSpan_contributionsAndCumulativeTotalsAsExpected() {

    TimeEntry timeEntry = createTimeEntry(TIME_ENTRY_ID,
        TENANT_ID,
        PERSON_ID,
        "2023-04-22T22:00:00+01:00",
        "2023-04-23T03:00:00+01:00");

    List<Accrual> accruals = balanceCalculator.calculate(timeEntry, KafkaAction.UPDATE);

    assertThat(accruals).hasSize(6);

    assertTypeAndDateAndTotals(accruals.get(0), ANNUAL_TARGET_HOURS,
        LocalDate.of(2023, 4, 22),
        120, 8160);
    assertTypeAndDateAndTotals(accruals.get(1), ANNUAL_TARGET_HOURS,
        LocalDate.of(2023, 4, 23),
        300, 8460);
    assertTypeAndDateAndTotals(accruals.get(2), ANNUAL_TARGET_HOURS,
        LocalDate.of(2023, 4, 24),
        300, 8760);
  }

  @Test
  void calculate_updateAthToOneDaySpan_contributionsAndCumulativeTotalsAsExpected() {

    TimeEntry timeEntry = createTimeEntry(TIME_ENTRY_ID,
        TENANT_ID,
        PERSON_ID,
        "2023-04-22T12:00:00+01:00",
        "2023-04-22T15:00:00+01:00");

    List<Accrual> accruals = balanceCalculator.calculate(timeEntry, KafkaAction.UPDATE);

    assertThat(accruals).hasSize(6);

    assertTypeAndDateAndTotals(accruals.get(0), ANNUAL_TARGET_HOURS,
        LocalDate.of(2023, 4, 22),
        180, 8220);
    assertTypeAndDateAndTotals(accruals.get(1), ANNUAL_TARGET_HOURS,
        LocalDate.of(2023, 4, 23),
        120, 8340);
    assertTypeAndDateAndTotals(accruals.get(2), ANNUAL_TARGET_HOURS,
        LocalDate.of(2023, 4, 24),
        300, 8640);
  }

  @Test
  void calculate_updateTimeEntryToPast_contributionsAndCumulativeTotalsAsExpected() {
    TimeEntry timeEntry = createTimeEntry("cc01b98e-12d4-4b06-8941-9919a2db45a9",
        TENANT_ID,
        PERSON_ID,
        "2023-03-22T09:00:00+00:00",
        "2023-03-22T10:00:00+00:00");

    List<Accrual> accruals = balanceCalculator.calculate(timeEntry, KafkaAction.UPDATE);

    assertTypeAndDateAndTotals(accruals.get(0), ANNUAL_TARGET_HOURS,
        LocalDate.of(2023, 3, 22),
        120, 8160);
    assertTypeAndDateAndTotals(accruals.get(1), ANNUAL_TARGET_HOURS,
        LocalDate.of(2023, 3, 23),
        420, 8580);
    assertTypeAndDateAndTotals(accruals.get(2), ANNUAL_TARGET_HOURS,
        LocalDate.of(2023, 3, 24),
        300, 8880);
  }

  @Test
  void calculate_updateTimeEntryToFuture_contributionsAndCumulativeTotalsAsExpected() {
    TimeEntry timeEntry = createTimeEntry("02e78cf4-8467-4d10-9816-c18f646c9061",
        TENANT_ID,
        PERSON_ID,
        "2023-03-26T09:00:00+00:00",
        "2023-03-26T10:00:00+00:00");

    List<Accrual> accruals = balanceCalculator.calculate(timeEntry, KafkaAction.UPDATE);

    assertTypeAndDateAndTotals(accruals.get(0), ANNUAL_TARGET_HOURS,
        LocalDate.of(2023, 3, 23),
        420, 8520);

    assertTypeAndDateAndTotals(accruals.get(1), ANNUAL_TARGET_HOURS,
        LocalDate.of(2023, 3, 24),
        300, 8820);

    assertTypeAndDateAndTotals(accruals.get(2), ANNUAL_TARGET_HOURS,
        LocalDate.of(2023, 3, 25),
        0, 8820);
    assertTypeAndDateAndTotals(accruals.get(3), ANNUAL_TARGET_HOURS,
        LocalDate.of(2023, 3, 26),
        120, 8940);
    assertTypeAndDateAndTotals(accruals.get(4), ANNUAL_TARGET_HOURS,
        LocalDate.of(2023, 3, 27),
        60, 9000);

  }
}
