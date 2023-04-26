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
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.homeoffice.digital.sas.balancecalculator.client.RestClient;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Accrual;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Agreement;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.enums.AccrualType;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.timecard.TimeEntry;
import uk.gov.homeoffice.digital.sas.balancecalculator.utils.TestUtils;

@ExtendWith(MockitoExtension.class)
class BalanceCalculatorTest {

  public static final AccrualType ANNUAL_TARGET_HOURS_TYPE =
      AccrualType.ANNUAL_TARGET_HOURS;
  public static final String ANNUAL_TARGET_HOURS_TYPE_ID =
      ANNUAL_TARGET_HOURS_TYPE.getId().toString();
  private static final ZonedDateTime SHIFT_START_TIME =
      ZonedDateTime.parse("2023-04-18T08:00:00+00:00");
  private static final ZonedDateTime SHIFT_END_TIME =
      ZonedDateTime.parse("2023-04-18T10:00:00+00:00");
  private static final LocalDate ACCRUAL_DATE = SHIFT_START_TIME.toLocalDate();
  private static final BigDecimal SHIFT_DURATION = new BigDecimal(120);
  private static final Range<ZonedDateTime> DATE_TIME_RANGE =
      Range.closed(SHIFT_START_TIME, SHIFT_END_TIME);
  private static final String TIME_ENTRY_ID = "7f000001-879e-1b02-8187-9ef1640f0003";
  private static final String TENANT_ID = "52a8188b-d41e-6768-19e9-09938016342f";
  private static final String PERSON_ID = "0936e7a6-2b2e-1696-2546-5dd25dcae6a0";
  private static final String AGREEMENT_ID = "c0a80193-87a3-1ff0-8187-a3bfe2b80004";
  private static final LocalDate AGREEMENT_END_DATE = LocalDate.of(2024, 3, 31);
  @Mock
  private RestClient restClient;

  private BalanceCalculator balanceCalculator;

  private static Stream<Arguments> testData() {
    return Stream.of(
        Arguments.of(TIME_ENTRY_ID, BigDecimal.valueOf(6600), BigDecimal.valueOf(7200),
            BigDecimal.valueOf(7440), BigDecimal.valueOf(8160)),
        Arguments.of("e7d85e42-f0fb-4e2a-8211-874e27d1e888", BigDecimal.valueOf(6240),
            BigDecimal.valueOf(6840), BigDecimal.valueOf(7080), BigDecimal.valueOf(7800))
    );
  }

  @BeforeEach
  void setup() {
    balanceCalculator = new BalanceCalculator(restClient);
  }

  @ParameterizedTest
  @MethodSource("testData")
  void calculateAccruals_withinCalendarDayAndAnnualTargetHours_returnUpdateAccrual(String timeEntryId,
      BigDecimal expectedCumulativeTotal1, BigDecimal expectedCumulativeTotal2,
      BigDecimal expectedCumulativeTotal3, BigDecimal expectedCumulativeTotal4)
      throws IOException {
    ZonedDateTime shiftStartTime = ZonedDateTime.parse("2023-04-18T08:00:00+00:00");
    ZonedDateTime shiftEndTime = ZonedDateTime.parse("2023-04-18T10:00:00+00:00");
    //total of 2 hours worked

    TimeEntry timeEntry = TestUtils.createTimeEntry(timeEntryId, PERSON_ID, shiftStartTime,
        shiftEndTime);

    LocalDate referenceDate = LocalDate.of(2023, 4, 18);

    String tenantId = timeEntry.getTenantId();

    when(restClient.getAccrualByTypeAndDate(tenantId, PERSON_ID,
        ANNUAL_TARGET_HOURS_TYPE_ID, referenceDate))
        .thenReturn(loadObjectFromFile("data/mockAccrual.json", Accrual.class));
    // initial cumulative total is 16.

    when(restClient.getAgreementById(tenantId, AGREEMENT_ID))
        .thenReturn(loadObjectFromFile("data/mockAgreement.json", Agreement.class));

    //ACCRUAL_DATE is plus 1 because call to this method is made using reference date plus 1.
    when(restClient.getAccrualsBetweenDates(tenantId, PERSON_ID, ACCRUAL_DATE.plusDays(1),
        AGREEMENT_END_DATE))
        .thenReturn(loadAccrualsFromFile("data/mockSubsequentAccruals.json"));

    when(
        restClient.getPriorAccrual(tenantId, PERSON_ID, ANNUAL_TARGET_HOURS_TYPE_ID, referenceDate))
        .thenReturn(loadObjectFromFile("data/mockPriorAccrual.json", Accrual.class));
    //cumulative prior is a 8

    List<Accrual> accruals =
        balanceCalculator.calculateAccruals(timeEntry, AccrualType.ANNUAL_TARGET_HOURS);

    //assert Cumulative total of 17th should be 18
    assertThat(accruals.get(0).getCumulativeTotal()).usingComparator(BigDecimal::compareTo)
        .isEqualTo(expectedCumulativeTotal1);

    assertThat(accruals.get(1).getCumulativeTotal()).usingComparator(BigDecimal::compareTo)
        .isEqualTo(expectedCumulativeTotal2);

    assertThat(accruals.get(2).getCumulativeTotal()).usingComparator(BigDecimal::compareTo)
        .isEqualTo(expectedCumulativeTotal3);

    assertThat(accruals.get(3).getCumulativeTotal()).usingComparator(BigDecimal::compareTo)
        .isEqualTo(expectedCumulativeTotal4);
  }

  @Test
  void recalculateContributions_createTimeEntryWithinCalendarDay_returnUpdatedAccrual()
      throws IOException {
    when(restClient.getAccrualByTypeAndDate(TENANT_ID, PERSON_ID,
        ANNUAL_TARGET_HOURS_TYPE_ID, ACCRUAL_DATE))
        .thenReturn(loadObjectFromFile("data/mockAccrual.json", Accrual.class));
    Accrual result =
        balanceCalculator.recalculateContributions(TIME_ENTRY_ID, TENANT_ID,
            PERSON_ID, ANNUAL_TARGET_HOURS_TYPE,
            ACCRUAL_DATE, DATE_TIME_RANGE);

    assertThat(result.getContributions().getTotal()).usingComparator(BigDecimal::compareTo)
        .isEqualTo(BigDecimal.valueOf(600));
    assertThat(result.getContributions().getTimeEntries()).hasSize(3);
    assertThat(result.getContributions().getTimeEntries()).containsEntry(
        UUID.fromString(TIME_ENTRY_ID), SHIFT_DURATION);
  }

  @Test
  void recalculateContributions_updateTimeEntryWithinCalendarDay_returnUpdatedAccrual()
      throws IOException {
    String existingTimeEntryId = "e7d85e42-f0fb-4e2a-8211-874e27d1e888";

    when(restClient.getAccrualByTypeAndDate(TENANT_ID, PERSON_ID, ANNUAL_TARGET_HOURS_TYPE_ID,
        ACCRUAL_DATE)).thenReturn(
        loadObjectFromFile("data/mockAccrual.json", Accrual.class));
    Accrual result =
        balanceCalculator.recalculateContributions(existingTimeEntryId, TENANT_ID,
            PERSON_ID, ANNUAL_TARGET_HOURS_TYPE,
            ACCRUAL_DATE, DATE_TIME_RANGE);

    assertThat(result.getContributions().getTotal()).usingComparator(BigDecimal::compareTo)
        .isEqualTo(BigDecimal.valueOf(240));
    assertThat(result.getContributions().getTimeEntries()).hasSize(2);
    assertThat(result.getContributions().getTimeEntries()).containsEntry(
        UUID.fromString(existingTimeEntryId), SHIFT_DURATION);
  }

  @Test
  void updateSubsequentAccruals_timeEntryWithinCalendarDay_updateCumulativeTotal()
      throws IOException {
    List<Accrual> accruals = loadAccrualsFromFile("data/mockSubsequentAccruals.json");

    List<Accrual> result =
        balanceCalculator.updateSubsequentAccruals(accruals, BigDecimal.valueOf(480));

    assertThat(result.get(0).getCumulativeTotal()).usingComparator(BigDecimal::compareTo)
        .isEqualTo(BigDecimal.valueOf(1080));

    assertThat(result.get(1).getCumulativeTotal()).usingComparator(BigDecimal::compareTo)
        .isEqualTo(BigDecimal.valueOf(1320));

    assertThat(result.get(2).getCumulativeTotal()).usingComparator(BigDecimal::compareTo)
        .isEqualTo(BigDecimal.valueOf(2040));
  }

  @Test
  void calculateDurationInMinutes_annualTargetHours() {
    BigDecimal minutes = balanceCalculator
        .calculateDurationInMinutes(DATE_TIME_RANGE, AccrualType.ANNUAL_TARGET_HOURS);
    assertThat(minutes).isEqualTo(SHIFT_DURATION);
  }

  @Test
  void calculateDurationInMinutes_default() {
    BigDecimal minutes = balanceCalculator
        .calculateDurationInMinutes(DATE_TIME_RANGE, AccrualType.NIGHT_HOURS);
    assertThat(minutes).isEqualTo(BigDecimal.ZERO);
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

  private <T> T loadObjectFromFile(String filePath, Class<T> type) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());

    File file = new File(
        Objects.requireNonNull(this.getClass().getClassLoader().getResource(filePath)).getFile()
    );
    return mapper.readValue(file, type);
  }
}