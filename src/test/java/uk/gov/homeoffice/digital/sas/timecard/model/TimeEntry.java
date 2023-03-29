package uk.gov.homeoffice.digital.sas.timecard.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Data
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

  @JsonProperty("id")
  private String id;

  @JsonProperty("tenantId")
  private String tenantId;

  @JsonProperty("ownerId")
  private String ownerId;

  @JsonProperty("timePeriodTypeId")
  private String timePeriodTypeId;

  @JsonProperty("shiftType")
  private String shiftType;

  @JsonProperty("actualStartTime")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private Date actualStartTime;

  @JsonProperty("actualEndTime")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private Date actualEndTime;

}