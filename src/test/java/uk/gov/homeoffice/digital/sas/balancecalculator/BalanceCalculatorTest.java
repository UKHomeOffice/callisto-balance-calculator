package uk.gov.homeoffice.digital.sas.balancecalculator;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.Range;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.when;

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
  private static final BigDecimal SHIFT_DURATION = new BigDecimal(2);
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
        Arguments.of(TIME_ENTRY_ID, BigDecimal.valueOf(110), BigDecimal.valueOf(120),
            BigDecimal.valueOf(124), BigDecimal.valueOf(136)),
        Arguments.of("e7d85e42-f0fb-4e2a-8211-874e27d1e888", BigDecimal.valueOf(104),
            BigDecimal.valueOf(114), BigDecimal.valueOf(118), BigDecimal.valueOf(130))
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
        .isEqualTo(BigDecimal.TEN);
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
        .isEqualTo(BigDecimal.valueOf(4));
    assertThat(result.getContributions().getTimeEntries()).hasSize(2);
    assertThat(result.getContributions().getTimeEntries()).containsEntry(
        UUID.fromString(existingTimeEntryId), SHIFT_DURATION);
  }

  @Test
  void updateSubsequentAccruals_timeEntryWithinCalendarDay_updateCumulativeTotal()
      throws IOException {
    List<Accrual> accruals = loadAccrualsFromFile("data/mockSubsequentAccruals.json");

    List<Accrual> result =
        balanceCalculator.updateSubsequentAccruals(accruals, BigDecimal.valueOf(8));

    assertThat(result.get(0).getCumulativeTotal()).usingComparator(BigDecimal::compareTo)
        .isEqualTo(BigDecimal.valueOf(18));

    assertThat(result.get(1).getCumulativeTotal()).usingComparator(BigDecimal::compareTo)
        .isEqualTo(BigDecimal.valueOf(22));

    assertThat(result.get(2).getCumulativeTotal()).usingComparator(BigDecimal::compareTo)
        .isEqualTo(BigDecimal.valueOf(34));
  }

  @Test
  void calculateDurationInHours_TODO() {

    BigDecimal hours = balanceCalculator
        .calculateDurationInHours(DATE_TIME_RANGE, AccrualType.ANNUAL_TARGET_HOURS);
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

  @Test
  void splitOverDays_timeEntryWithinTwoCalendarDays_returnTwoDateTimeRanges() {

    var startTime = ZonedDateTime.parse("2023-04-18T22:00:00+00:00");
    var endTimeNextDay = ZonedDateTime.parse("2023-04-19T01:00:00+00:00");

    Map<LocalDate, Range<ZonedDateTime>> ranges =
        balanceCalculator.splitOverDays(startTime, endTimeNextDay);
    assertThat(ranges).hasSize(2);

    Range<ZonedDateTime> range1 = ranges.get(startTime.toLocalDate());
    Range<ZonedDateTime> range2 = ranges.get(endTimeNextDay.toLocalDate());

    assertAll(
        () -> assertThat(range1.lowerEndpoint()).isEqualTo(startTime),
        () -> assertThat(range1.upperEndpoint()).isEqualTo("2023-04-19T00:00:00+00:00"),
        () -> assertThat(range2.lowerEndpoint()).isEqualTo("2023-04-19T00:00:00+00:00"),
        () -> assertThat(range2.upperEndpoint()).isEqualTo(endTimeNextDay),
        () -> assertRangeHoursCount(range1, new BigDecimal(2)),
        () -> assertRangeHoursCount(range2, new BigDecimal(1))
    );
  }

  @Test
  void splitOverDays_timeEntryWithinFourCalendarDays_returnFourDateTimeRanges() {

    var startTime = ZonedDateTime.parse("2023-04-18T22:00:00+00:00");
    var endTime = ZonedDateTime.parse("2023-04-21T06:00:00+00:00");

    Map<LocalDate, Range<ZonedDateTime>> ranges =
        balanceCalculator.splitOverDays(startTime, endTime);
    assertThat(ranges).hasSize(4);

    Range<ZonedDateTime> range1 = ranges.get(startTime.toLocalDate());
    Range<ZonedDateTime> range2 = ranges.get(startTime.plusDays(1).toLocalDate());
    Range<ZonedDateTime> range3 = ranges.get(startTime.plusDays(2).toLocalDate());
    Range<ZonedDateTime> range4 = ranges.get(endTime.toLocalDate());

    assertAll(
        () -> assertThat(range1.lowerEndpoint()).isEqualTo(startTime),
        () -> assertThat(range1.upperEndpoint()).isEqualTo("2023-04-19T00:00:00+00:00"),
        () -> assertThat(range2.lowerEndpoint()).isEqualTo("2023-04-19T00:00:00+00:00"),
        () -> assertThat(range2.upperEndpoint()).isEqualTo("2023-04-20T00:00:00+00:00"),
        () -> assertThat(range3.lowerEndpoint()).isEqualTo("2023-04-20T00:00:00+00:00"),
        () -> assertThat(range3.upperEndpoint()).isEqualTo("2023-04-21T00:00:00+00:00"),
        () -> assertThat(range4.lowerEndpoint()).isEqualTo("2023-04-21T00:00:00+00:00"),
        () -> assertThat(range4.upperEndpoint()).isEqualTo(endTime),
        () -> assertRangeHoursCount(range1, new BigDecimal(2)),
        () -> assertRangeHoursCount(range2, new BigDecimal(24)),
        () -> assertRangeHoursCount(range3, new BigDecimal(24)),
        () -> assertRangeHoursCount(range4, new BigDecimal(6))
    );
  }

  // ?? Shall we take to account end of shift at midnight as another range with length of 0 min ? Guess not
  // For that reason there is only 1 range assertion
  @Test
  void splitOverDays_timeEntryWithinOneCalendarDaysFinishingAtMidnight_returnOneDateTimeRange() {

    var startTime = ZonedDateTime.parse("2023-04-18T22:00:00+00:00");
    var endTimeNextDay = ZonedDateTime.parse("2023-04-19T00:00:00+00:00");

    Map<LocalDate, Range<ZonedDateTime>> ranges =
        balanceCalculator.splitOverDays(startTime, endTimeNextDay);
    assertThat(ranges).hasSize(1);

    Range<ZonedDateTime> range1 = ranges.get(startTime.toLocalDate());
    //Range<ZonedDateTime> range2 = ranges.get(endTimeNextDay.toLocalDate());

    assertAll(
        () -> assertThat(range1.lowerEndpoint()).isEqualTo(startTime),
        () -> assertThat(range1.upperEndpoint()).isEqualTo("2023-04-19T00:00:00+00:00")
//        () -> assertThat(range2.lowerEndpoint()).isEqualTo("2023-04-19T00:00:00+00:00"),
//        () -> assertThat(range2.upperEndpoint()).isEqualTo(endTimeNextDay),
//        () -> assertRangeHoursCount(range1, new BigDecimal(2)),
//        () -> assertRangeHoursCount(range2, new BigDecimal(0))
    );
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

  private void assertRangeHoursCount(Range<ZonedDateTime> range , BigDecimal expectedHours) {
    BigDecimal rangeHours = balanceCalculator
        .calculateDurationInHours(range, AccrualType.ANNUAL_TARGET_HOURS);
    assertThat(rangeHours).isEqualTo(expectedHours);
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