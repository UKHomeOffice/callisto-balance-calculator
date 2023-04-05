package uk.gov.homeoffice.digital.sas.balancecalculator.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Date;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TimeEntry {

  private String id;

  private String tenantId;

  private String ownerId;

  private String timePeriodTypeId;

  private String shiftType;

  private Date actualStartTime;

  private Date actualEndTime;

}
