package uk.gov.homeoffice.digital.sas.balancecalculator.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Date;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

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

  private String id;

  private String tenantId;

  private String ownerId;

  private String timePeriodTypeId;

  private String shiftType;

  private String actualStartTime;

  private String actualEndTime;

}
