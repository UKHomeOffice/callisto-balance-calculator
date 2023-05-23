package uk.gov.homeoffice.digital.sas.balancecalculator.module;

import static uk.gov.homeoffice.digital.sas.balancecalculator.utils.RangeUtils.splitOverDays;

import com.google.common.collect.Range;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.SortedMap;
import java.util.TreeMap;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.enums.AccrualType;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.timecard.TimeEntry;

public class AnnualTargetHoursAccrualModule extends AccrualModule {

  public AnnualTargetHoursAccrualModule() {
    super(AccrualType.ANNUAL_TARGET_HOURS);
  }

  @Override
  public SortedMap<LocalDate, BigDecimal> getContributions(TimeEntry timeEntry) {

    SortedMap<LocalDate, Range<ZonedDateTime>> dateRanges =
        splitOverDays(timeEntry.getActualStartTime(), timeEntry.getActualEndTime());

    SortedMap<LocalDate, BigDecimal> result = new TreeMap<>();

    dateRanges.forEach((key, value) -> {
      ZonedDateTime startTime = value.lowerEndpoint();
      ZonedDateTime endTime = value.upperEndpoint();
      Duration shiftDuration = Duration.between(startTime, endTime);
      BigDecimal contribution = new BigDecimal(shiftDuration.toMinutes());
      result.put(key, contribution);
    });

    return result;
  }
}
