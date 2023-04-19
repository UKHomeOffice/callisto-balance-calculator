package uk.gov.homeoffice.digital.sas.balancecalculator;

import static uk.gov.homeoffice.digital.sas.balancecalculator.utils.TestUtils.createTimeEntry;

import java.time.ZonedDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.TimeEntry;

@SpringBootTest
class BalanceCalculatorTest {

  @Autowired
  private BalanceCalculator balanceCalculator;

  @BeforeEach
  void setup() {
  }

  @Test
  void calculate_TODO() {

    ZonedDateTime startTime = ZonedDateTime.now();
    ZonedDateTime finishTime = startTime.plusHours(2);

    TimeEntry timeEntry = createTimeEntry(UUID.randomUUID().toString(),
        UUID.randomUUID().toString(),
        startTime,
        finishTime);

    balanceCalculator.calculate(timeEntry);
  }

}