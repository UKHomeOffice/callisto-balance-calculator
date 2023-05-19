package uk.gov.homeoffice.digital.sas.balancecalculator.module;

import static uk.gov.homeoffice.digital.sas.balancecalculator.utils.RangeUtils.splitOverDays;

import com.google.common.collect.Range;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.UUID;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Accrual;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Agreement;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Contributions;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.enums.AccrualType;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.timecard.TimeEntry;
import uk.gov.homeoffice.digital.sas.kafka.message.KafkaAction;

@Getter
@Slf4j
public abstract class AccrualModule {

  public static final String ACCRUALS_MAP_EMPTY = "Accruals Map must contain at least one entry!";
  public static final String MISSING_ACCRUAL =
      "Accrual missing for tenantId {0}, personId {1}, accrual type {2} and date {3}";

  protected AccrualType accrualType;

  protected AccrualModule(AccrualType accrualType) {
    this.accrualType = accrualType;
  }

  public abstract BigDecimal calculateShiftContribution(ZonedDateTime startTime,
                                                        ZonedDateTime endTime);

  public boolean applyTimeEntryToAccruals(TimeEntry timeEntry,
                                      KafkaAction action,
                                      SortedMap<LocalDate, Accrual> accruals,
                                      Agreement agreement) {

    // split in days
    SortedMap<LocalDate, Range<ZonedDateTime>> dateRanges =
        splitOverDays(timeEntry.getActualStartTime(), timeEntry.getActualEndTime());

    for (var entry : dateRanges.entrySet()) {
      LocalDate accrualDate = entry.getKey();
      ZonedDateTime startTime = entry.getValue().lowerEndpoint();
      ZonedDateTime endTime = entry.getValue().upperEndpoint();

      Accrual accrual = accruals.get(accrualDate);

      if (accrual == null) {
        log.error(MessageFormat.format(
            MISSING_ACCRUAL, timeEntry.getTenantId(), timeEntry.getOwnerId(),
            accrualType, accrualDate));
        return false;
      }

      this.updateAccrualContribution(timeEntry.getId(), startTime, endTime, accrual, action);
    }

    this.cascadeCumulativeTotal(accruals, agreement.getStartDate());
    return true;
  }

  void updateAccrualContribution(String timeEntryId, ZonedDateTime startTime, ZonedDateTime endTime,
      Accrual accrual, KafkaAction action) {

    Contributions contributions = accrual.getContributions();
    Map<UUID, BigDecimal> timeEntries = contributions.getTimeEntries();

    switch (action) {
      case CREATE -> {
        BigDecimal shiftContribution = this.calculateShiftContribution(startTime, endTime);
        timeEntries.put(UUID.fromString(timeEntryId), shiftContribution);
      }

      case DELETE -> timeEntries.remove(UUID.fromString(timeEntryId));

      case UPDATE -> throw new UnsupportedOperationException("NOT IMPLEMENTED YET");

      default -> throw new UnsupportedOperationException("UNKNOWN KAFKA EVENT ACTION");
    }

    BigDecimal total = timeEntries.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    contributions.setTotal(total);
  }

  void cascadeCumulativeTotal(
      SortedMap<LocalDate, Accrual> accruals, LocalDate agreementStartDate) {

    Optional<LocalDate> optional = accruals.keySet().stream().findFirst();
    if (optional.isPresent()) {
      LocalDate priorAccrualDate = optional.get();
      Accrual priorAccrual = accruals.get(priorAccrualDate);

      BigDecimal baseCumulativeTotal = BigDecimal.ZERO;
      // if prior accrual is related to the same agreement then use its cumulative total
      // as starting point; otherwise, start at 0
      if (isPriorAccrualRelatedToTheSameAgreement(priorAccrualDate, agreementStartDate)) {
        baseCumulativeTotal = priorAccrual.getCumulativeTotal();
      }

      // the first element is only used to calculate base cumulative total so shouldn't be included
      List<Accrual> accrualsFromReferenceDate = accruals.values().stream().skip(1).toList();
      this.updateSubsequentAccruals(accrualsFromReferenceDate, baseCumulativeTotal);
    } else {
      throw new IllegalArgumentException(ACCRUALS_MAP_EMPTY);
    }
  }

  private boolean isPriorAccrualRelatedToTheSameAgreement(
      LocalDate priorAccrualDate, LocalDate agreementStatDate) {
    return !priorAccrualDate.isBefore(agreementStatDate);
  }

  void updateSubsequentAccruals(List<Accrual> accruals, BigDecimal priorCumulativeTotal) {

    //update the cumulative total for referenceDate
    accruals.get(0).setCumulativeTotal(
        priorCumulativeTotal.add(accruals.get(0).getContributions().getTotal()));

    //cascade through until end of agreement
    for (int i = 1; i < accruals.size(); i++) {
      BigDecimal priorTotal =
          accruals.get(i - 1).getCumulativeTotal();
      Accrual currentAccrual = accruals.get(i);
      currentAccrual.setCumulativeTotal(
          priorTotal.add(currentAccrual.getContributions().getTotal()));
    }
  }
}
