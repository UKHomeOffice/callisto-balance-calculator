package uk.gov.homeoffice.digital.sas.balancecalculator.module;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import lombok.Getter;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.enums.AccrualType;

@Getter
public abstract class AccrualModule {

  protected AccrualType accrualType;

  protected AccrualModule(AccrualType accrualType) {
    this.accrualType = accrualType;
  }

  public abstract BigDecimal calculateShiftContribution(ZonedDateTime startTime,
                                                        ZonedDateTime endTime);

}
