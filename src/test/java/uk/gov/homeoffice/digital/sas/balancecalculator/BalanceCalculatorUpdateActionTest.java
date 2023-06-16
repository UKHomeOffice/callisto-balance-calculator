package uk.gov.homeoffice.digital.sas.balancecalculator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.homeoffice.digital.sas.balancecalculator.testutils.CommonUtils.loadAccrualsFromFile;
import static uk.gov.homeoffice.digital.sas.balancecalculator.testutils.CommonUtils.loadObjectFromFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.OutputCaptureExtension;
import uk.gov.homeoffice.digital.sas.balancecalculator.client.AccrualsService;
import uk.gov.homeoffice.digital.sas.balancecalculator.handlers.ContributionsHandler;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Accrual;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Agreement;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.timecard.TimeEntry;
import uk.gov.homeoffice.digital.sas.balancecalculator.module.AccrualModule;
import uk.gov.homeoffice.digital.sas.balancecalculator.module.AnnualTargetHoursAccrualModule;
import uk.gov.homeoffice.digital.sas.balancecalculator.module.NightHoursAccrualModule;
import uk.gov.homeoffice.digital.sas.balancecalculator.testutils.CommonUtils;
import uk.gov.homeoffice.digital.sas.kafka.message.KafkaAction;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class BalanceCalculatorUpdateActionTest {

  private static final String PERSON_ID = "0936e7a6-2b2e-1696-2546-5dd25dcae6a0";

  @Mock
  private AccrualsService accrualsService;

  private static Stream<Arguments> annualTargetHoursTestData() {
    return Stream.of(
        // update one day time entry
        Arguments.of("85cd140e-9eeb-4771-ab6c-6dea17fcfcbe",
            new String[]{"2023-04-18T09:00:00+00:00", "2023-04-18T12:00:00+00:00"},
            new LinkedHashMap<String, List<BigDecimal>>(){{
              put("CUMULATIVE_TOTAL", Arrays.asList(BigDecimal.valueOf(6540), BigDecimal.valueOf(7140),
            BigDecimal.valueOf(7380), BigDecimal.valueOf(8100)));
              put("CONTRIBUTIONS_TOTAL", List.of(BigDecimal.valueOf(540), BigDecimal.valueOf(600),
            BigDecimal.valueOf(240), BigDecimal.valueOf(720))); }}),
        // updating one day time entry to become three day time entry
        Arguments.of("85cd140e-9eeb-4771-ab6c-6dea17fcfcbe",
            new String[]{"2023-04-18T22:00:00+00:00", "2023-04-20T02:00:00+00:00"},
            new LinkedHashMap<String, List<BigDecimal>>(){{
              put("CUMULATIVE_TOTAL", Arrays.asList( BigDecimal.valueOf(6420), BigDecimal.valueOf(8460),
                  BigDecimal.valueOf(8880), BigDecimal.valueOf(9600)));
              put("CONTRIBUTIONS_TOTAL", List.of(BigDecimal.valueOf(420), BigDecimal.valueOf(2040),
                  BigDecimal.valueOf(420), BigDecimal.valueOf(720))); }}),
        // updating two day time entry to one day time entry
        Arguments.of("e7d85e42-f0fb-4e2a-8211-874e27d1e888",
            new String[]{"2023-04-18T14:00:00+00:00", "2023-04-18T15:00:00+00:00"},
            new LinkedHashMap<String, List<BigDecimal>>(){{
              put("CUMULATIVE_TOTAL", Arrays.asList(BigDecimal.valueOf(6180), BigDecimal.valueOf(6420),
                  BigDecimal.valueOf(6660), BigDecimal.valueOf(7380)));
              put("CONTRIBUTIONS_TOTAL", List.of(BigDecimal.valueOf(180), BigDecimal.valueOf(240),
                  BigDecimal.valueOf(240), BigDecimal.valueOf(720))); }})
    );
  };

  private static Stream<Arguments> nightHoursTestData() {
    return Stream.of(
        // update one day time entry
        Arguments.of("008ca0f2-ab26-42a0-ba1d-f9eb49287f5b",
            new String[]{"2023-04-01T01:00:00+01:00", "2023-04-01T02:00:00+01:00"},
            new LinkedHashMap<String, List<BigDecimal>>(){{
              put("CUMULATIVE_TOTAL", Arrays.asList(BigDecimal.valueOf(60), BigDecimal.valueOf(120),
                  BigDecimal.valueOf(360), BigDecimal.valueOf(360)));
              put("CONTRIBUTIONS_TOTAL", List.of(BigDecimal.valueOf(60), BigDecimal.valueOf(60),
                  BigDecimal.valueOf(240), BigDecimal.valueOf(0)));
            }}),
        // update two day time entry to one day entry
        Arguments.of("67c77e84-5bdf-44de-ae9a-9028db97a797",
            new String[]{"2023-04-02T22:00:00+01:00", "2023-04-03T00:00:00+01:00"},
            new LinkedHashMap<String, List<BigDecimal>>(){{
              put("CUMULATIVE_TOTAL", Arrays.asList(BigDecimal.valueOf(120), BigDecimal.valueOf(180),
                  BigDecimal.valueOf(180), BigDecimal.valueOf(180)));
              put("CONTRIBUTIONS_TOTAL", List.of(BigDecimal.valueOf(0), BigDecimal.valueOf(60),
                  BigDecimal.valueOf(0), BigDecimal.valueOf(0)));
            }}),
        // update one day time entry to two day entry
        Arguments.of("008ca0f2-ab26-42a0-ba1d-f9eb49287f5b",
            new String[]{"2023-04-01T21:00:00+01:00", "2023-04-02T06:00:00+01:00"},
            new LinkedHashMap<String, List<BigDecimal>>(){{
              put("CUMULATIVE_TOTAL", Arrays.asList(BigDecimal.valueOf(60), BigDecimal.valueOf(480),
                  BigDecimal.valueOf(720), BigDecimal.valueOf(720)));
              put("CONTRIBUTIONS_TOTAL", List.of(BigDecimal.valueOf(60), BigDecimal.valueOf(420),
                  BigDecimal.valueOf(240), BigDecimal.valueOf(0)));
            }}),
        // update time entry to start day before
        Arguments.of("67c77e84-5bdf-44de-ae9a-9028db97a797",
            new String[]{"2023-04-01T22:00:00+01:00", "2023-04-02T00:00:00+01:00"},
            new LinkedHashMap<String, List<BigDecimal>>(){{
              put("CUMULATIVE_TOTAL", Arrays.asList(BigDecimal.valueOf(180), BigDecimal.valueOf(180),
                  BigDecimal.valueOf(180), BigDecimal.valueOf(180)));
              put("CONTRIBUTIONS_TOTAL", List.of(BigDecimal.valueOf(180), BigDecimal.valueOf(0),
                  BigDecimal.valueOf(0), BigDecimal.valueOf(0)));
            }}),
        // update time entry to start at the start of agreement
        Arguments.of("008ca0f2-ab26-42a0-ba1d-f9eb49287f5b",
            new String[]{"2023-04-01T00:00:00+01:00", "2023-04-01T01:00:00+01:00"},
            new LinkedHashMap<String, List<BigDecimal>>(){{
              put("CUMULATIVE_TOTAL", Arrays.asList(BigDecimal.valueOf(60), BigDecimal.valueOf(120),
                  BigDecimal.valueOf(360), BigDecimal.valueOf(360)));
              put("CONTRIBUTIONS_TOTAL", List.of( BigDecimal.valueOf(60), BigDecimal.valueOf(60),
                  BigDecimal.valueOf(240), BigDecimal.valueOf(0)));
            }})
    );
  }

  @ParameterizedTest
  @MethodSource("annualTargetHoursTestData")
  void calculate_annualTargetHours_returnUpdateAccruals(String timeEntryId,
          String[] timeEntryDates, LinkedHashMap<String, List<BigDecimal>> totals)
          throws IOException {
    assertions(timeEntryId, timeEntryDates, totals,new AnnualTargetHoursAccrualModule(),
        "accruals_annualTargetHours.json");
  }


  @ParameterizedTest
  @MethodSource("nightHoursTestData")
  void calculate_nightHours_returnUpdatedAccruals(String timeEntryId,
          String[] timeEntryDates, LinkedHashMap<String, List<BigDecimal>> totals)
          throws IOException {
    assertions(timeEntryId, timeEntryDates, totals, new NightHoursAccrualModule(),
        "accruals_nightHoursUpdateAction.json");

  }


  void assertions (String timeEntryId, String[] timeEntryDates,
          LinkedHashMap<String, List<BigDecimal>> totals, AccrualModule module,
          String accrualsResultsFilePath ) throws IOException {

    List<AccrualModule> accrualModules = List.of(module);
    ContributionsHandler contributionsHandler = new ContributionsHandler(accrualModules);
    BalanceCalculator balanceCalculator = new BalanceCalculator(accrualsService, contributionsHandler);

    TimeEntry timeEntry = CommonUtils.createTimeEntry(timeEntryId, PERSON_ID, timeEntryDates[0],
        timeEntryDates[1]);

    String tenantId = timeEntry.getTenantId();

    when(accrualsService.getApplicableAgreement(tenantId, PERSON_ID,
        timeEntry.getActualEndTime().toLocalDate()))
        .thenReturn(loadObjectFromFile("data/agreement2023.json", Agreement.class));

    when(accrualsService.getImpactedAccruals(tenantId, PERSON_ID, timeEntryId,
        timeEntry.getActualStartTime().toLocalDate(), timeEntry.getActualEndTime().toLocalDate()))
        .thenReturn(loadAccrualsFromFile("data/" + accrualsResultsFilePath));

    List<Accrual> accruals = balanceCalculator.calculate(timeEntry, KafkaAction.UPDATE);

    assertThat(accruals).hasSize(totals.get("CUMULATIVE_TOTAL").size());

    for (int i = 0; i < accruals.size(); i++) {
      assertTotals(accruals.get(i), totals.get("CUMULATIVE_TOTAL").get(i),
          totals.get("CONTRIBUTIONS_TOTAL").get(i));
    }

  }

  private void assertTotals(Accrual accrual, BigDecimal expectedCumulativeTotal,
        BigDecimal expectedContributionsTotal) {
    assertThat(accrual.getCumulativeTotal()).usingComparator(
            BigDecimal::compareTo)
        .isEqualTo(expectedCumulativeTotal);

    assertThat(accrual.getContributions().getTotal()).usingComparator(
        BigDecimal::compareTo).isEqualTo(expectedContributionsTotal);
  }


}