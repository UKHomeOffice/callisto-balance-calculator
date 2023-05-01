package uk.gov.homeoffice.digital.sas.balancecalculator;

import com.google.common.collect.Range;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import uk.gov.homeoffice.digital.sas.balancecalculator.client.RestClient;
import uk.gov.homeoffice.digital.sas.balancecalculator.configuration.AccrualModuleConfig;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Accrual;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Agreement;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.enums.AccrualType;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.timecard.TimeEntry;
import uk.gov.homeoffice.digital.sas.balancecalculator.module.AccrualModule;

@Component
@Import(AccrualModuleConfig.class)
public class BalanceCalculator {

  private final RestClient restClient;
  private final List<AccrualModule> accrualModules;

  @Autowired
  public BalanceCalculator(RestClient restClient, List<AccrualModule> accrualModules) {
    this.restClient = restClient;
    this.accrualModules = accrualModules;
  }

  public List<Accrual> calculate(TimeEntry timeEntry) {

    String timeEntryId = timeEntry.getId();
    String tenantId = timeEntry.getTenantId();
    String personId = timeEntry.getOwnerId();
    ZonedDateTime timeEntryStart = timeEntry.getActualStartTime();
    ZonedDateTime timeEntryEnd = timeEntry.getActualEndTime();
    LocalDate timeEntryStartDate = timeEntryStart.toLocalDate();
    LocalDate timeEntryEndDate = timeEntryEnd.toLocalDate();

    // get agreement applicable to the end date of the time entry (in case the time entry spans two
    // agreements
    Agreement applicableAgreement =
        getAgreementApplicableToTimeEntryEndDate(tenantId, personId, timeEntryEndDate);

    // get accruals of all types between the day just before the time entry and the end date of the
    // latest applicable agreement
    Map<AccrualType, TreeMap<LocalDate, Accrual>> allAccruals =
        getAccrualsBetweenDates(tenantId, personId,
            timeEntryStartDate.minusDays(1), applicableAgreement.getEndDate());

    Map<LocalDate, Range<ZonedDateTime>> dateRangeMap = splitOverDays(timeEntryStart, timeEntryEnd);

    dateRangeMap.entrySet().forEach(entry ->
        accrualModules.forEach(module -> {
          LocalDate referenceDate = entry.getKey();
          ZonedDateTime startTime = entry.getValue().lowerEndpoint();
          ZonedDateTime endTime = entry.getValue().upperEndpoint();

          Accrual accrual = module.extractAccrualByReferenceDate(allAccruals, referenceDate);

          module.updateAccrualContribution(timeEntryId, startTime, endTime, accrual);
        })
    );

    List<Accrual> accrualsToBatchUpdate = accrualModules.stream()
        .map(module -> module.cascadeCumulativeTotal(
            allAccruals, applicableAgreement.getStartDate()))
        .flatMap(List::stream)
        .toList();


    // TODO : send Batch Update request to Accruals API
    // restClient.batchUpdate(accrualsToBatchUpdate)

    return accrualsToBatchUpdate;
  }

  Agreement getAgreementApplicableToTimeEntryEndDate(String tenantId, String personId,
      LocalDate timeEntryEndDate) {
    return restClient.getApplicableAgreement(tenantId,
        personId, timeEntryEndDate);
  }

  Map<AccrualType, TreeMap<LocalDate, Accrual>> getAccrualsBetweenDates(
      String tenantId, String personId, LocalDate startDate, LocalDate endDate) {

    List<Accrual> accruals = restClient.getAccrualsBetweenDates(tenantId, personId,
        startDate, endDate);

    return map(accruals);
  }

  Map<AccrualType, TreeMap<LocalDate, Accrual>> map(List<Accrual> accruals) {
    return accruals.stream()
        .collect(Collectors.groupingBy(
                Accrual::getAccrualType,
                Collectors.toMap(
                    Accrual::getAccrualDate,
                    Function.identity(),
                    (c1, c2) -> c1, TreeMap::new)
            )
        );
  }

  Map<LocalDate, Range<ZonedDateTime>> splitOverDays(ZonedDateTime startDateTime,
      ZonedDateTime endDateTime) {
    Map<LocalDate, Range<ZonedDateTime>> intervals = new HashMap<>();

    if (isOnSameDay(startDateTime, endDateTime)) {
      Range<ZonedDateTime> range = Range.closed(startDateTime, endDateTime);
      intervals.put(startDateTime.toLocalDate(), range);
    } else {
      // TODO: cover case when time entry span multiple days
      throw new UnsupportedOperationException(
          "Case when time entry spans multiple days isn't implemented yet");
    }

    return intervals;
  }

  private boolean isOnSameDay(ZonedDateTime dateTimeOne, ZonedDateTime dateTimeTwo) {
    return dateTimeOne.toLocalDate().isEqual(dateTimeTwo.toLocalDate());
  }
}
