package uk.gov.homeoffice.digital.sas.balancecalculator;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.homeoffice.digital.sas.balancecalculator.utils.TestUtils.createTimeEntry;

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

  @Autowired
  private BalanceCalculator balanceCalculator;

  @BeforeEach
  void setup() {
  }

  @Test
  void calculate_TODO() {

    String timeEntryId = "7f000001-879e-1b02-8187-9ef1640f0003";
    String tenantId = "52a8188b-d41e-6768-19e9-09938016342f";
    String personId = "0936e7a6-2b2e-1696-2546-5dd25dcae6a0";
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

}