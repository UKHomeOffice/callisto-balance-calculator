package uk.gov.homeoffice.digital.sas.balancecalculator.module;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZonedDateTime;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.enums.AccrualType;

public class AnnualTargetHoursAccrualModule extends AccrualModule {

  public AnnualTargetHoursAccrualModule() {
    super(AccrualType.ANNUAL_TARGET_HOURS);
  }

  @Override
  public BigDecimal calculateShiftContribution(ZonedDateTime startTime, ZonedDateTime endTime) {
    Duration shiftDuration = Duration.between(startTime, endTime);
    return new BigDecimal(shiftDuration.toMinutes());
  }
}
