package uk.gov.homeoffice.digital.sas.balancecalculator.module;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Accrual;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Contributions;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.enums.AccrualType;

public abstract class AccrualModule {

  protected AccrualType accrualType;

  protected AccrualModule(AccrualType accrualType) {
    this.accrualType = accrualType;
  }

  public abstract Accrual extractAccrualByReferenceDate(
      Map<AccrualType, TreeMap<LocalDate, Accrual>> allAccruals,
      LocalDate referenceDate);

  public BigDecimal calculateShiftContribution(ZonedDateTime startTime, ZonedDateTime endTime) {
    Duration shiftDuration = Duration.between(startTime, endTime);
    return new BigDecimal(shiftDuration.toMinutes());
  }

  public void updateAccrualContribution(String timeEntryId, ZonedDateTime startTime,
      ZonedDateTime endTime,
      Accrual accrual) {

    BigDecimal minutes = calculateShiftContribution(startTime, endTime);

    Contributions contributions = accrual.getContributions();
    contributions.getTimeEntries().put(UUID.fromString(timeEntryId), minutes);
    BigDecimal total =
        contributions.getTimeEntries().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    contributions.setTotal(total);
  }

  public List<Accrual> cascadeCumulativeTotal(
      Map<AccrualType, TreeMap<LocalDate, Accrual>> allAccruals, LocalDate agreementStartDate) {

    TreeMap<LocalDate, Accrual> accruals = allAccruals.get(this.accrualType);

    Optional<LocalDate> optional = accruals.keySet().stream().findFirst();
    if (optional.isPresent()) {
      LocalDate priorAccrualDate = optional.get();
      Accrual priorAccrual = accruals.get(priorAccrualDate);

      BigDecimal baseCumulativeTotal = BigDecimal.ZERO;
      // if prior accrual related to the same agreement use its cumulative total as starting point
      // otherwise start at 0
      if (isPriorAccrualRelatedToTheSameAgreement(priorAccrualDate, agreementStartDate)) {
        baseCumulativeTotal = priorAccrual.getCumulativeTotal();
      }

      // the first element is only used to calculate base cumulative total
      accruals.remove(priorAccrualDate);

      return updateSubsequentAccruals(accruals.values().stream().toList(),
          baseCumulativeTotal);
    } else {
      throw new IllegalArgumentException("Accruals Map must contain at least one entry!");
    }
  }

  private boolean isPriorAccrualRelatedToTheSameAgreement(
      LocalDate priorAccrualDate, LocalDate agreementStatDate) {
    return !priorAccrualDate.isBefore(agreementStatDate);
  }


  List<Accrual> updateSubsequentAccruals(List<Accrual> accruals, BigDecimal priorCumulativeTotal) {

    List<Accrual> updatedAccruals = List.copyOf(accruals);
    //update the cumulative total for referenceDate
    updatedAccruals.get(0).setCumulativeTotal(
        priorCumulativeTotal.add(updatedAccruals.get(0).getContributions().getTotal()));

    //cascade through until end of agreement
    for (int i = 1; i < updatedAccruals.size(); i++) {
      BigDecimal priorTotal =
          updatedAccruals.get(i - 1).getCumulativeTotal();
      Accrual currentAccrual = updatedAccruals.get(i);
      currentAccrual.setCumulativeTotal(
          priorTotal.add(currentAccrual.getContributions().getTotal()));
    }

    return updatedAccruals;
  }
}
