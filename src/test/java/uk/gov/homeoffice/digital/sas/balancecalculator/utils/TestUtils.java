package uk.gov.homeoffice.digital.sas.balancecalculator.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.TimeEntry;
import uk.gov.homeoffice.digital.sas.kafka.message.KafkaAction;
import uk.gov.homeoffice.digital.sas.kafka.message.KafkaEventMessage;

public class TestUtils {

  public static KafkaEventMessage generateExpectedKafkaEventMessage(String version, TimeEntry resource,
                                                                    KafkaAction action) {
    return new KafkaEventMessage<>(version, resource, action);
  }

  public static TimeEntry createTimeEntry(String id, String ownerId, Date startTime,
                                          Date finishTime) {
    var timeEntry = new TimeEntry();
    timeEntry.setId(id);
    timeEntry.setTenantId("3fa85f64-5717-4562-b3fc-2c963f66afa6");
    timeEntry.setOwnerId(ownerId);
    timeEntry.setTimePeriodTypeId("00000000-0000-0000-0000-000000000001");
    timeEntry.setShiftType(" ");
    timeEntry.setActualStartTime(startTime);
    timeEntry.setActualEndTime(finishTime);
    return timeEntry;
  }

  public static Date getAsDate(LocalDateTime dateTime) {
    return Date.from(dateTime.toInstant(ZoneOffset.UTC));
  }

  public static <S extends Serializable> String createKafkaMessage(String schema, String version,
                                                                   String id, String ownerId){
    String resource = createResourceJson(id, ownerId);

    return new Gson().toJson(Map.of(
        "schema", String.format("%s, %s",
            schema, version),
        "resource" , resource,
        "action", "CREATE"));
  }

  public static <S extends Serializable> String createResourceJson(String id, String ownerId) {
    return new Gson().toJson(Map.of(
        "id", id,
        "tenantId", "3fa85f64-5717-4562-b3fc-2c963f66afa6",
        "ownerId", ownerId,
        "timePeriodTypeId", "00000000-0000-0000-0000-000000000001",
        "shiftType", " ",
        "actualStartTime", "1679456400000",
        "actualEndTime", "1679457000000"
    ));
  }

}
