package uk.gov.homeoffice.digital.sas.balancecalculator.module;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.SortedMap;
import lombok.Getter;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.enums.AccrualType;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.timecard.TimeEntry;

@Getter
public abstract class AccrualModule {

  protected AccrualType accrualType;

  protected AccrualModule(AccrualType accrualType) {
    this.accrualType = accrualType;
  }

  public abstract SortedMap<LocalDate, BigDecimal> getContributions(TimeEntry timeEntry);

}
