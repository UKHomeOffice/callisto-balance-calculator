package uk.gov.homeoffice.digital.sas.balancecalculator;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Range;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.homeoffice.digital.sas.balancecalculator.client.RestClient;

@ExtendWith(MockitoExtension.class)
class BalanceCalculatorTest {


  private static final ZonedDateTime SHIFT_START_TIME =
      ZonedDateTime.parse("2023-04-18T08:00:00+00:00");
  private static final ZonedDateTime SHIFT_END_TIME =
      ZonedDateTime.parse("2023-04-18T10:00:00+00:00");

  private static final BigDecimal SHIFT_DURATION = new BigDecimal(2);

  @Mock
  private RestClient restClient;

  private BalanceCalculator balanceCalculator;

  @BeforeEach
  void setup() {
    balanceCalculator = new BalanceCalculator(restClient);
  }

  @Test
  void calculateDurationInHours_TODO() {

    Range<ZonedDateTime> dateTimeRange = Range.closed(SHIFT_START_TIME, SHIFT_END_TIME);

    BigDecimal hours = balanceCalculator.calculateDurationInHours(dateTimeRange);
    assertThat(hours).isEqualTo(SHIFT_DURATION);
  }
  @Test
  void splitOverDays_timeEntryWithinCalendarDay_returnOneDateTimeRange() {

    Map<LocalDate, Range<ZonedDateTime>> ranges =
        balanceCalculator.splitOverDays(SHIFT_START_TIME, SHIFT_END_TIME);
    assertThat(ranges).hasSize(1);

    Range<ZonedDateTime> range = ranges.get(SHIFT_START_TIME.toLocalDate());

    assertThat(range.lowerEndpoint()).isEqualTo(SHIFT_START_TIME);
    assertThat(range.upperEndpoint()).isEqualTo(SHIFT_END_TIME);
  }

}