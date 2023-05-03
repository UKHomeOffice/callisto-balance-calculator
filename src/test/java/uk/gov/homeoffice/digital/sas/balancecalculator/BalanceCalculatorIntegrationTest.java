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

  private String timeEntryId = "7f000001-879e-1b02-8187-9ef1640f0003";
  private String tenantId = "52a8188b-d41e-6768-19e9-09938016342f";
  private String personId = "0936e7a6-2b2e-1696-2546-5dd25dcae6a0";

  @Autowired
  private BalanceCalculator balanceCalculator;

  @BeforeEach
  void setup() {
  }

  @Test
  void calculate_TODO() {

    ZonedDateTime startTime = ZonedDateTime.parse("2023-04-18T08:00:00+00:00");
    ZonedDateTime finishTime = startTime.plusHours(2);

    TimeEntry timeEntry = createTimeEntry(timeEntryId,
        tenantId,
        personId,
        startTime,
        finishTime);

    List<Accrual> accruals = balanceCalculator.calculate(timeEntry);

    assertThat(accruals).hasSize(4);
  }

  @Test
  void calculate_accrualsDays_whenTimeEntryHasTwoDaysSpan() {
    // Given
    ZonedDateTime startTime = ZonedDateTime.parse("2023-04-22T22:00:00+00:00");
    ZonedDateTime finishTime = ZonedDateTime.parse("2023-04-23T06:00:00+00:00");

    TimeEntry timeEntry = createTimeEntry(timeEntryId,
        tenantId,
        personId,
        startTime,
        finishTime);

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