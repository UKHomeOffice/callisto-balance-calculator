package uk.gov.homeoffice.digital.sas.balancecalculator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.homeoffice.digital.sas.balancecalculator.utils.TestUtils.loadAccrualsFromFile;
import static uk.gov.homeoffice.digital.sas.balancecalculator.utils.TestUtils.loadObjectFromFile;

import com.google.common.collect.Range;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
import uk.gov.homeoffice.digital.sas.balancecalculator.module.AccrualModule;
import uk.gov.homeoffice.digital.sas.balancecalculator.module.AnnualTargetHoursAccrualModule;
import uk.gov.homeoffice.digital.sas.balancecalculator.utils.TestUtils;

@ExtendWith(MockitoExtension.class)
class BalanceCalculatorTest {

  private static final ZonedDateTime SHIFT_START_TIME =
      ZonedDateTime.parse("2023-04-18T08:00:00+00:00");
  private static final ZonedDateTime SHIFT_END_TIME =
      ZonedDateTime.parse("2023-04-18T10:00:00+00:00");
  private static final LocalDate ACCRUAL_DATE = SHIFT_START_TIME.toLocalDate();
  private static final String TIME_ENTRY_ID = "7f000001-879e-1b02-8187-9ef1640f0003";
  private static final String PERSON_ID = "0936e7a6-2b2e-1696-2546-5dd25dcae6a0";
  private static final LocalDate AGREEMENT_END_DATE = LocalDate.of(2024, 3, 31);

  @Mock
  private RestClient restClient;

  private final List<AccrualModule> accrualModules = List.of(new AnnualTargetHoursAccrualModule());

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
    balanceCalculator = new BalanceCalculator(restClient, accrualModules);
  }

  @ParameterizedTest
  @MethodSource("testData")
  void calculate_withinCalendarDayAndAnnualTargetHours_returnUpdateAccruals(String timeEntryId,
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

    when(restClient.getApplicableAgreement(tenantId, PERSON_ID, referenceDate))
        .thenReturn(loadObjectFromFile("data/agreement.json", Agreement.class));

    //ACCRUAL_DATE minus 1 because call to this method is made using reference date minus 1.
    when(restClient.getAccrualsBetweenDates(tenantId, PERSON_ID, ACCRUAL_DATE.minusDays(1),
        AGREEMENT_END_DATE))
        .thenReturn(loadAccrualsFromFile("data/accruals_annualTargetHours.json"));


    List<Accrual> accruals = balanceCalculator.calculate(timeEntry);

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
  void splitOverDays_timeEntryWithinCalendarDay_returnOneDateTimeRange() {

    Map<LocalDate, Range<ZonedDateTime>> ranges =
        balanceCalculator.splitOverDays(SHIFT_START_TIME, SHIFT_END_TIME);
    assertThat(ranges).hasSize(1);

    Range<ZonedDateTime> range = ranges.get(SHIFT_START_TIME.toLocalDate());

    assertThat(range.lowerEndpoint()).isEqualTo(SHIFT_START_TIME);
    assertThat(range.upperEndpoint()).isEqualTo(SHIFT_END_TIME);
  }

  @Test
  void map_listOfAccruals_mappedByAccrualTypeAndDate() throws IOException {
    List<Accrual> accruals = loadAccrualsFromFile("data/accruals_convertToMap.json");

    Map<AccrualType, TreeMap<LocalDate, Accrual>> map =
        balanceCalculator.map(accruals);

    assertThat(map).hasSize(2);

    TreeMap<LocalDate, Accrual> annualTargetHoursMap = map.get(AccrualType.ANNUAL_TARGET_HOURS);

    assertThat(annualTargetHoursMap).hasSize(2);
    assertThat(annualTargetHoursMap.get(LocalDate.of(2023,4,19))
        .getCumulativeTotal()).usingComparator(BigDecimal::compareTo)
        .isEqualTo(BigDecimal.valueOf(7080));
    assertThat(annualTargetHoursMap.get(LocalDate.of(2023,4,20))
        .getCumulativeTotal()).usingComparator(BigDecimal::compareTo)
        .isEqualTo(BigDecimal.valueOf(7320));

    TreeMap<LocalDate, Accrual> nightHoursMap = map.get(AccrualType.NIGHT_HOURS);
    assertThat(nightHoursMap).hasSize(1);
    assertThat(nightHoursMap.get(LocalDate.of(2023,4,19))
        .getCumulativeTotal()).usingComparator(BigDecimal::compareTo)
        .isEqualTo(BigDecimal.valueOf(8040));
  }
}