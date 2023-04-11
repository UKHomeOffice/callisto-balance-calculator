package uk.gov.homeoffice.digital.sas.balancecalculator.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

  public static String createKafkaMessage(String schema,
                                          String version,
                                          String id,
                                          String ownerId) throws JsonProcessingException {

    ObjectMapper mapper = new ObjectMapper();

    ObjectNode resource = createResourceJson(mapper, id, ownerId);

    ObjectNode kafkaMessage = mapper.createObjectNode();
    kafkaMessage.put("schema", String.format("%s, %s", schema, version));
    kafkaMessage.set("resource", resource);
    kafkaMessage.put("action", "CREATE");


    return mapper.writeValueAsString(kafkaMessage);
  }

  public static String createKafkaMessage(String schema,
                                          String version,
                                          ObjectNode resource) throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();

    ObjectNode kafkaMessage = mapper.createObjectNode();
    kafkaMessage.put("schema", String.format("%s, %s", schema, version));
    kafkaMessage.set("resource", resource);
    kafkaMessage.put("action", "CREATE");


    return mapper.writeValueAsString(kafkaMessage);
  }


  public static ObjectNode createResourceJson(ObjectMapper mapper, String id,
                                                                   String ownerId) {
    ObjectNode resourceNode = mapper.createObjectNode();
    resourceNode.put("id", id);
    resourceNode.put("tenantId", "3fa85f64-5717-4562-b3fc-2c963f66afa6");
    resourceNode.put("ownerId", ownerId);
    resourceNode.put("timePeriodTypeId", "00000000-0000-0000-0000-000000000001");
    resourceNode.put("shiftType", " ");
    resourceNode.put("actualStartTime", "2022-01-01T15:00:00");
    resourceNode.put("actualEndTime", "2022-01-01T16:00:00");

    return resourceNode;
  }

  public static ObjectNode createResourceJson(ObjectMapper mapper, String id,
                                              String ownerId, String startTime, String endTime) {
    ObjectNode resourceNode = mapper.createObjectNode();
    resourceNode.put("id", id);
    resourceNode.put("tenantId", "3fa85f64-5717-4562-b3fc-2c963f66afa6");
    resourceNode.put("ownerId", ownerId);
    resourceNode.put("timePeriodTypeId", "00000000-0000-0000-0000-000000000001");
    resourceNode.put("shiftType", " ");
    resourceNode.put("actualStartTime", startTime);
    resourceNode.put("actualEndTime", endTime);

    return resourceNode;
  }


}
