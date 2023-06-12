package uk.gov.homeoffice.digital.sas.balancecalculator;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.enums.AccrualType.ANNUAL_TARGET_HOURS;
import static uk.gov.homeoffice.digital.sas.balancecalculator.testutils.CommonUtils.createTimeEntry;

import java.math.BigDecimal;
import java.util.List;
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

    assertTotals(accruals.get(0), ANNUAL_TARGET_HOURS, 540, 7020);
    assertTotals(accruals.get(1), ANNUAL_TARGET_HOURS, 240, 7260);
    assertTotals(accruals.get(2), ANNUAL_TARGET_HOURS, 720, 7980);
    assertTotals(accruals.get(3), ANNUAL_TARGET_HOURS, 120, 8100);
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

    assertTotals(accruals.get(0), ANNUAL_TARGET_HOURS, 120, 8160);
    assertTotals(accruals.get(1), ANNUAL_TARGET_HOURS, 300, 8460);
    assertTotals(accruals.get(2), ANNUAL_TARGET_HOURS, 300, 8760);
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

    assertTotals(accruals.get(0), ANNUAL_TARGET_HOURS, 180, 8220);
    assertTotals(accruals.get(1), ANNUAL_TARGET_HOURS, 120, 8340);
    assertTotals(accruals.get(2), ANNUAL_TARGET_HOURS, 300, 8640);
  }

  private void assertTotals(Accrual accrual,
                            AccrualType expectedAccrualType,
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
