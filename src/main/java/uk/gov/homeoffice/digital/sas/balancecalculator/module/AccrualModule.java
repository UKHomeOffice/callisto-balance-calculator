package uk.gov.homeoffice.digital.sas.balancecalculator.module;

import lombok.Getter;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.enums.AccrualType;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZonedDateTime;

@Getter
public abstract class AccrualModule {

  protected AccrualType accrualType;

  protected AccrualModule(AccrualType accrualType) {
    this.accrualType = accrualType;
  }

  public BigDecimal calculateShiftContribution(ZonedDateTime startTime, ZonedDateTime endTime) {
    Duration shiftDuration = Duration.between(startTime, endTime);
    return new BigDecimal(shiftDuration.toMinutes());
  }

}
