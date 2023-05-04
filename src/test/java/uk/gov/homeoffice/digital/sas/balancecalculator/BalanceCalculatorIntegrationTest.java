package uk.gov.homeoffice.digital.sas.balancecalculator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Accrual;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.timecard.TimeEntry;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static uk.gov.homeoffice.digital.sas.balancecalculator.utils.TestUtils.createTimeEntry;

@SpringBootTest
@AutoConfigureWireMock(port = 9999)
class BalanceCalculatorIntegrationTest {

  private static final String TIME_ENTRY_ID = "7f000001-879e-1b02-8187-9ef1640f0003";
  private static final String TENANT_ID = "52a8188b-d41e-6768-19e9-09938016342f";
  private static final String PERSON_ID = "0936e7a6-2b2e-1696-2546-5dd25dcae6a0";
  private static final ZonedDateTime SHIFT_START_TIME =
      ZonedDateTime.parse("2023-04-18T08:00:00+00:00");
  private static final ZonedDateTime SHIFT_END_TIME =
      ZonedDateTime.parse("2023-04-18T10:00:00+00:00");

  @Autowired
  private BalanceCalculator balanceCalculator;

  @BeforeEach
  void setup() {
  }

  @Test
  void calculate_TODO() {
    TimeEntry timeEntry = createTimeEntry(TIME_ENTRY_ID,
        TENANT_ID,
        PERSON_ID,
        SHIFT_START_TIME,
        SHIFT_END_TIME);

    List<Accrual> accruals = balanceCalculator.calculate(timeEntry);

    assertThat(accruals).hasSize(4);
  }

  @Test
  void calculate_accrualsDays_whenTimeEntryHasTwoDaysSpan() {
    // Given
    ZonedDateTime startTime = ZonedDateTime.parse("2023-04-22T22:00:00+00:00");
    ZonedDateTime finishTime = ZonedDateTime.parse("2023-04-23T06:00:00+00:00");

    TimeEntry timeEntry = createTimeEntry(TIME_ENTRY_ID, TENANT_ID, PERSON_ID,
        startTime, finishTime);
    // When
    List<Accrual> accruals = balanceCalculator.calculate(timeEntry);
    // Then
    assertAll(
        () -> assertThat(accruals).hasSize(2),
        () -> assertThat(accruals.get(0).getContributions().getTotal()).isEqualTo(new BigDecimal(6)),
        () -> assertThat(accruals.get(1).getContributions().getTotal()).isEqualTo(new BigDecimal(2))
    );
  }

}