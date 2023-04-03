package uk.gov.homeoffice.digital.sas.balancecalculator.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "id",
    "tenantId",
    "ownerId",
    "timePeriodTypeId",
    "shiftType",
    "actualStartTime",
    "actualEndTime"
})
public class TimeEntry {

  private String id;

  private String tenantId;

  private String ownerId;

  private String timePeriodTypeId;

  private String shiftType;

  private String actualStartTime;

  private String actualEndTime;

}
