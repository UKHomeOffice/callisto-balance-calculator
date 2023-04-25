package uk.gov.homeoffice.digital.sas.balancecalculator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.Range;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.homeoffice.digital.sas.balancecalculator.client.RestClient;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Accrual;

@ExtendWith(MockitoExtension.class)
class BalanceCalculatorTest {


  private static final ZonedDateTime SHIFT_START_TIME =
      ZonedDateTime.parse("2023-04-18T08:00:00+00:00");
  private static final ZonedDateTime SHIFT_END_TIME =
      ZonedDateTime.parse("2023-04-18T10:00:00+00:00");
  private static final LocalDate ACCRUAL_DATE = SHIFT_START_TIME.toLocalDate();
  private static final BigDecimal SHIFT_DURATION = new BigDecimal(2);
  private static final Range<ZonedDateTime> DATE_TIME_RANGE =
      Range.closed(SHIFT_START_TIME, SHIFT_END_TIME);

  private static final String TIME_ENTRY_ID = "7f000001-879e-1b02-8187-9ef1640f0003";
  private static final String TENANT_ID = "52a8188b-d41e-6768-19e9-09938016342f";
  private static final String PERSON_ID = "0936e7a6-2b2e-1696-2546-5dd25dcae6a0";

  @Mock
  private RestClient restClient;

  private BalanceCalculator balanceCalculator;

  @BeforeEach
  void setup() {
    balanceCalculator = new BalanceCalculator(restClient);
  }

  @Test
  void calculateContributionsAndUpdateAccrual_createTimeEntryWithinCalendarDay_returnUpdatedAccrual()
      throws IOException {
    when(restClient.getAccrualsByDate(TENANT_ID, PERSON_ID, ACCRUAL_DATE)).thenReturn(
        loadAccrualsFromFile("data/mockAccruals.json"));
    Accrual result =
        balanceCalculator.calculateContributionsAndUpdateAccrual(TIME_ENTRY_ID, TENANT_ID,
            PERSON_ID,
            ACCRUAL_DATE, DATE_TIME_RANGE);

    assertThat(result.getContributions().getTotal()).usingComparator(BigDecimal::compareTo)
        .isEqualTo(BigDecimal.TEN);
    assertThat(result.getContributions().getTimeEntries()).hasSize(3);
    assertThat(result.getContributions().getTimeEntries()).containsEntry(
        UUID.fromString(TIME_ENTRY_ID), SHIFT_DURATION);
  }

  @Test
  void calculateContributionsAndUpdateAccrual_updateTimeEntryWithinCalendarDay_returnUpdatedAccrual()
      throws IOException {
    String existingTimeEntryId = "e7d85e42-f0fb-4e2a-8211-874e27d1e888";

    when(restClient.getAccrualsByDate(TENANT_ID, PERSON_ID, ACCRUAL_DATE)).thenReturn(
        loadAccrualsFromFile("data/mockAccruals.json"));
    Accrual result =
        balanceCalculator.calculateContributionsAndUpdateAccrual(existingTimeEntryId, TENANT_ID,
            PERSON_ID,
            ACCRUAL_DATE, DATE_TIME_RANGE);

    assertThat(result.getContributions().getTotal()).usingComparator(BigDecimal::compareTo)
        .isEqualTo(BigDecimal.valueOf(4));
    assertThat(result.getContributions().getTimeEntries()).hasSize(2);
    assertThat(result.getContributions().getTimeEntries()).containsEntry(
        UUID.fromString(existingTimeEntryId), SHIFT_DURATION);
  }

  @Test
  void updateSubsequentAccruals_timeEntryWithinCalendarDay_updateCumulativeTotal()
      throws IOException {
    List<Accrual> accruals = loadAccrualsFromFile("data/mockSubsequentAccruals.json");

    List<Accrual> result = balanceCalculator.updateSubsequentAccruals(accruals, BigDecimal.valueOf(8));

    assertThat(result.get(0).getCumulativeTotal()).usingComparator(BigDecimal::compareTo)
        .isEqualTo(BigDecimal.valueOf(18));

    assertThat(result.get(1).getCumulativeTotal()).usingComparator(BigDecimal::compareTo)
        .isEqualTo(BigDecimal.valueOf(22));

    assertThat(result.get(2).getCumulativeTotal()).usingComparator(BigDecimal::compareTo)
        .isEqualTo(BigDecimal.valueOf(34));
  }

  @Test
  void calculateDurationInHours_TODO() {

    BigDecimal hours = balanceCalculator.calculateDurationInHours(DATE_TIME_RANGE);
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

  private List<Accrual> loadAccrualsFromFile(String filePath) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());

    File file = new File(
        Objects.requireNonNull(this.getClass().getClassLoader().getResource(filePath)).getFile()
    );
    return mapper.readValue(file, new TypeReference<>() {
    });
  }
}