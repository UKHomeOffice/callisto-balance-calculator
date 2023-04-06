package uk.gov.homeoffice.digital.sas.balancecalculator.utils;

import com.google.gson.Gson;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.TimeEntry;
import uk.gov.homeoffice.digital.sas.kafka.message.KafkaAction;
import uk.gov.homeoffice.digital.sas.kafka.message.KafkaEventMessage;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Map;

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
    String resource = createValidResourceJson(id, ownerId);
    return createKafkaEventJsonString(schema, version, resource);
  }

  public static <S extends Serializable> String createKafkaInvalidMessage(String schema, String version,
                                                                   String id, String ownerId){
    String resource = createInvalidResourceJson(id, ownerId, "123L");
    return createKafkaEventJsonString(schema, version, resource);
  }

  private static String createKafkaEventJsonString(String schema, String version, String resource) {
    return new Gson().toJson(Map.of(
        "schema", String.format("%s, %s",
            schema, version),
        "resource", resource,
        "action", "CREATE"));
  }

  public static <S extends Serializable> String createValidResourceJson(String id, String ownerId) {
    return createResourceJson(id, ownerId, "1679456400000");
  }

  public static <S extends Serializable> String createInvalidResourceJson(String id, String ownerId, String actualStartTime) {
    return createResourceJson(id, ownerId, actualStartTime);
  }

  public static <S extends Serializable> String createResourceJson(String id, String ownerId, String actualStartTime) {
    return new Gson().toJson(Map.of(
        "id", id,
        "tenantId", "3fa85f64-5717-4562-b3fc-2c963f66afa6",
        "ownerId", ownerId,
        "timePeriodTypeId", "00000000-0000-0000-0000-000000000001",
        "shiftType", " ",
        "actualStartTime", actualStartTime,
        "actualEndTime", "1679457000000"
    ));
  }

}
