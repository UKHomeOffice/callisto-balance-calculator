package uk.gov.homeoffice.digital.sas.balancecalculator;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.homeoffice.digital.sas.balancecalculator.utils.TestUtils.createTimeEntry;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Accrual;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.timecard.TimeEntry;

@SpringBootTest
class BalanceCalculatorIntegrationTest {

  @Autowired
  private BalanceCalculator balanceCalculator;

  @BeforeEach
  void setup() {
  }

  @Disabled("This is only run locally against a local Accruals instance")
  @Test
  void calculate_TODO() {

    ZonedDateTime startTime = ZonedDateTime.now();
    ZonedDateTime finishTime = startTime.plusHours(2);

    TimeEntry timeEntry = createTimeEntry(UUID.randomUUID().toString(),
        UUID.randomUUID().toString(),
        startTime,
        finishTime);

    Set<Accrual> accruals = balanceCalculator.calculate(timeEntry);
    assertThat(accruals).hasSize(1);

    Accrual actual = accruals.iterator().next();
    assertThat(actual.getContributions().getTotal()).isEqualTo(new BigDecimal(2));
  }

}