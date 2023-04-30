package uk.gov.homeoffice.digital.sas.balancecalculator.module;

import java.time.LocalDate;
import java.util.Map;
import java.util.TreeMap;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Accrual;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.enums.AccrualType;

public class AnnualTargetHoursAccrualModule extends AccrualModule {

  public AnnualTargetHoursAccrualModule() {
    super(AccrualType.ANNUAL_TARGET_HOURS);
  }

  @Override
  public Accrual extractAccrualByReferenceDate(
      Map<AccrualType, TreeMap<LocalDate, Accrual>> allAccruals,
      LocalDate referenceDate) {

    return allAccruals.get(AccrualType.ANNUAL_TARGET_HOURS).get(referenceDate);
  }
}
