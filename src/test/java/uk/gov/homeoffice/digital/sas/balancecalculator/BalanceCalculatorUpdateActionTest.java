package uk.gov.homeoffice.digital.sas.balancecalculator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.enums.AccrualType.ANNUAL_TARGET_HOURS;
import static uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.enums.AccrualType.NIGHT_HOURS;
import static uk.gov.homeoffice.digital.sas.balancecalculator.testutils.CommonUtils.assertTypeAndDateAndTotalsForMultipleAccruals;
import static uk.gov.homeoffice.digital.sas.balancecalculator.testutils.CommonUtils.loadAccrualsFromFile;
import static uk.gov.homeoffice.digital.sas.balancecalculator.testutils.CommonUtils.loadObjectFromFile;

import java.io.IOException;
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
        // updating one day time entry
        Arguments.of("85cd140e-9eeb-4771-ab6c-6dea17fcfcbe",
            "2023-04-18T09:00:00+00:00",
            "2023-04-18T12:00:00+00:00",
            new String[]{"2023-04-18", "2023-04-19", "2023-04-20", "2023-04-21"},
            new int[]{6540, 7140, 7380, 8100},
            new int[]{540, 600, 240, 720}
        ),
        // updating one day time entry to become three day time entry
        Arguments.of("85cd140e-9eeb-4771-ab6c-6dea17fcfcbe",
            "2023-04-18T22:00:00+00:00",
            "2023-04-20T02:00:00+00:00",
            new String[]{"2023-04-18", "2023-04-19", "2023-04-20", "2023-04-21"},
            new int[]{6420, 8460, 8880, 9600},
            new int[]{420, 2040, 420, 720}
        ),
        // updating two day time entry to one day time entry
        Arguments.of("e7d85e42-f0fb-4e2a-8211-874e27d1e888",
            "2023-04-18T14:00:00+00:00", "2023-04-18T15:00:00+00:00",
            new String[]{"2023-04-18", "2023-04-19", "2023-04-20", "2023-04-21"},
            new int[]{6180, 6420, 6660, 7380},
            new int[]{180, 240, 240, 720}
        )
    );
  }

  private static Stream<Arguments> nightHoursTestData() {
    return Stream.of(
        // updating one day time entry
        Arguments.of("008ca0f2-ab26-42a0-ba1d-f9eb49287f5b",
            "2023-04-01T01:00:00+01:00",
            "2023-04-01T02:00:00+01:00",
            new String[]{"2023-04-01", "2023-04-02", "2023-04-03", "2023-04-04"},
            new int[]{ 60, 120, 360, 360},
            new int[]{ 60, 60, 240, 0}
        ),
        // update two day time entry to one day entry
        Arguments.of("67c77e84-5bdf-44de-ae9a-9028db97a797",
            "2023-04-02T22:00:00+01:00",
            "2023-04-03T00:00:00+01:00",
            new String[]{"2023-04-01", "2023-04-02", "2023-04-03", "2023-04-04"},
            new int[]{ 120, 180, 180, 180},
            new int[]{ 120, 60, 0, 0}
        ),
        // update one day time entry to two day entry
        Arguments.of("008ca0f2-ab26-42a0-ba1d-f9eb49287f5b",
            "2023-04-01T21:00:00+01:00",
            "2023-04-02T06:00:00+01:00",
            new String[]{"2023-04-01", "2023-04-02", "2023-04-03", "2023-04-04"},
            new int[]{ 60, 480, 720, 720},
            new int[]{ 60, 420, 240, 0}
        ),
        // update time entry to start day before
        Arguments.of("67c77e84-5bdf-44de-ae9a-9028db97a797",
            "2023-04-01T21:00:00+01:00",
            "2023-04-02T06:00:00+01:00",
            new String[]{"2023-04-01", "2023-04-02", "2023-04-03", "2023-04-04"},
            new int[]{ 180, 540, 540, 540},
            new int[]{ 180, 360, 0, 0}
        )
    );
  }

  @ParameterizedTest
  @MethodSource("annualTargetHoursTestData")
  void calculate_annualTargetHours_returnUpdateAccruals(String timeEntryId,
                                                        String shiftStartTime,
                                                        String shiftEndTime,
                                                        String[] expectedDates,
                                                        int[] expectedCumulativeTotals,
                                                        int[] expectedContributionsTotals)
      throws IOException {

    List<AccrualModule> accrualModules = List.of(new AnnualTargetHoursAccrualModule());
    ContributionsHandler contributionsHandler = new ContributionsHandler(accrualModules);
    BalanceCalculator balanceCalculator = new BalanceCalculator(accrualsService,
        contributionsHandler);

    TimeEntry timeEntry = CommonUtils.createTimeEntry(timeEntryId, PERSON_ID, shiftStartTime,
        shiftEndTime);

    String tenantId = timeEntry.getTenantId();

    when(accrualsService.getApplicableAgreement(tenantId, PERSON_ID,
        timeEntry.getActualEndTime().toLocalDate()))
        .thenReturn(loadObjectFromFile("data/agreement.json", Agreement.class));

    when(accrualsService.getImpactedAccruals(tenantId, PERSON_ID, timeEntryId,
        timeEntry.getActualStartTime().toLocalDate(), timeEntry.getActualEndTime().toLocalDate()))
        .thenReturn(loadAccrualsFromFile("data/accruals_annualTargetHours.json"));

    List<Accrual> accruals = balanceCalculator.calculate(timeEntry, KafkaAction.UPDATE);

    assertThat(accruals).hasSize(4);

    assertTypeAndDateAndTotalsForMultipleAccruals(accruals, ANNUAL_TARGET_HOURS, expectedDates,
        expectedCumulativeTotals, expectedContributionsTotals);
  }

  @ParameterizedTest
  @MethodSource("nightHoursTestData")
  void calculate_nightHours_returnUpdateAccruals(String timeEntryId,
                                                        String shiftStartTime,
                                                        String shiftEndTime,
                                                        String[] expectedDates,
                                                        int[] expectedCumulativeTotals,
                                                        int[] expectedContributionsTotals)
      throws IOException {

    List<AccrualModule> accrualModules = List.of(new NightHoursAccrualModule());
    ContributionsHandler contributionsHandler = new ContributionsHandler(accrualModules);
    BalanceCalculator balanceCalculator = new BalanceCalculator(accrualsService,
        contributionsHandler);

    TimeEntry timeEntry = CommonUtils.createTimeEntry(timeEntryId, PERSON_ID, shiftStartTime,
        shiftEndTime);

    String tenantId = timeEntry.getTenantId();

    when(accrualsService.getApplicableAgreement(tenantId, PERSON_ID,
        timeEntry.getActualEndTime().toLocalDate()))
        .thenReturn(loadObjectFromFile("data/agreement.json", Agreement.class));

    when(accrualsService.getImpactedAccruals(tenantId, PERSON_ID, timeEntryId,
        timeEntry.getActualStartTime().toLocalDate(), timeEntry.getActualEndTime().toLocalDate()))
        .thenReturn(loadAccrualsFromFile("data/accruals_nightHoursUpdateAction.json"));

    List<Accrual> accruals = balanceCalculator.calculate(timeEntry, KafkaAction.UPDATE);

    assertThat(accruals).hasSize(4);

    assertTypeAndDateAndTotalsForMultipleAccruals(accruals, NIGHT_HOURS, expectedDates,
        expectedCumulativeTotals, expectedContributionsTotals);
  }

}