package uk.gov.homeoffice.digital.sas.balancecalculator;

import static org.springframework.util.CollectionUtils.isEmpty;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import uk.gov.homeoffice.digital.sas.balancecalculator.client.RestClient;
import uk.gov.homeoffice.digital.sas.balancecalculator.configuration.AccrualModuleConfig;
import uk.gov.homeoffice.digital.sas.balancecalculator.handlers.ContributionsHandler;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Accrual;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Agreement;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.enums.AccrualType;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.timecard.TimeEntry;
import uk.gov.homeoffice.digital.sas.balancecalculator.module.AccrualModule;
import uk.gov.homeoffice.digital.sas.kafka.message.KafkaAction;

@Component
@Slf4j
@Import(AccrualModuleConfig.class)
public class BalanceCalculator {

  static final String AGREEMENT_NOT_FOUND =
      "Agreement record not found for tenantId {0}, personId {1} and date {2}";
  static final String ACCRUALS_NOT_FOUND =
      "No Accrual records found for tenantId {0} and personId {1} between {2} and {3}";


  private final RestClient restClient;
  private final List<AccrualModule> accrualModules;
  private final ContributionsHandler contributionsHandler;

  @Autowired
  public BalanceCalculator(RestClient restClient, List<AccrualModule> accrualModules,
                           ContributionsHandler contributionsHandler) {
    this.restClient = restClient;
    this.accrualModules = accrualModules;
    this.contributionsHandler = contributionsHandler;
  }

  public List<Accrual> calculate(TimeEntry timeEntry, KafkaAction action) {

    // TODO: what to do if TimeEntry has no end date ?? / exit ??
    String tenantId = timeEntry.getTenantId();
    String personId = timeEntry.getOwnerId();
    ZonedDateTime timeEntryStart = timeEntry.getActualStartTime();
    ZonedDateTime timeEntryEnd = timeEntry.getActualEndTime();
    LocalDate timeEntryStartDate = timeEntryStart.toLocalDate();
    LocalDate timeEntryEndDate = timeEntryEnd.toLocalDate();

    // Get agreement applicable to the end date of the time entry (in case the time entry spans two
    // agreements
    Agreement applicableAgreement =
        getAgreementApplicableToTimeEntryEndDate(tenantId, personId, timeEntryEndDate);
    if (applicableAgreement == null) {
      log.warn(MessageFormat.format(AGREEMENT_NOT_FOUND, tenantId, personId, timeEntryEndDate));
      return List.of();
    }

    // Get accruals of all types between the day just before the time entry and the end date of the
    // latest applicable agreement
    SortedMap<AccrualType, SortedMap<LocalDate, Accrual>> allAccruals =
        getAccrualsBetweenDates(tenantId, personId,
            timeEntryStartDate.minusDays(1), applicableAgreement.getEndDate());

    if (isEmpty(allAccruals)) {
      log.warn(MessageFormat.format(ACCRUALS_NOT_FOUND, tenantId, personId,
          timeEntryStartDate.minusDays(1), applicableAgreement.getEndDate()));
      return List.of();
    }


    if (!contributionsHandler.handle(timeEntry, applicableAgreement,
        allAccruals, accrualModules, action)) {
      return List.of();
    }


    // Each AccrualType within allAccruals map still containing entry for prior day
    // which shouldn't be sent to batch update. Lines below removing that entry from the Map
    for (SortedMap<LocalDate, Accrual> value : allAccruals.values()) {
      value.remove(value.firstKey());
    }

    return allAccruals.values().stream()
        .map(Map::values)
        .flatMap(Collection::stream)
        .toList();
  }

  public void sendToAccruals(String tenantId, List<Accrual> accruals) {
    restClient.patchAccruals(tenantId, accruals);
  }

  Agreement getAgreementApplicableToTimeEntryEndDate(String tenantId,
                                                     String personId,
                                                     LocalDate timeEntryEndDate) {
    return restClient.getApplicableAgreement(tenantId,
        personId, timeEntryEndDate);
  }

  SortedMap<AccrualType, SortedMap<LocalDate, Accrual>> getAccrualsBetweenDates(
      String tenantId, String personId, LocalDate startDate, LocalDate endDate) {

    List<Accrual> accruals = restClient.getAccrualsBetweenDates(tenantId, personId,
        startDate, endDate);

    return map(accruals);
  }

  /**
   * Groups input list of accruals by Accrual Type then by Accrual Date. Note the use of SortedMap
   * in the nested map to ensure accrual records are sorted by date
   *
   * @param accruals list of accruals
   * @return Accruals mapped by Accrual Type and Accrual Date
   */
  SortedMap<AccrualType, SortedMap<LocalDate, Accrual>> map(List<Accrual> accruals) {
    Map<AccrualType, SortedMap<LocalDate, Accrual>> accrualsMap = accruals.stream()
        .collect(Collectors.groupingBy(
                Accrual::getAccrualType,
                Collectors.toMap(
                    Accrual::getAccrualDate,
                    Function.identity(),
                    (c1, c2) -> c1, TreeMap::new)
            )
        );
    return new TreeMap<>(accrualsMap);
  }
}
