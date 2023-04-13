package uk.gov.homeoffice.digital.sas.balancecalculator.utils;

import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.EMPTY_STRING;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.VALID_END_TIME;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.VALID_START_TIME;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.VALID_TENANT_ID;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.VALID_TIME_PERIOD_TYPE_ID;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.TimeEntry;
import uk.gov.homeoffice.digital.sas.kafka.message.KafkaAction;

public class TestUtils {

  public static TimeEntry createTimeEntry(String id, String ownerId, Date startTime,
                                          Date finishTime) {

    var timeEntry = new TimeEntry();
    timeEntry.setId(id);
    timeEntry.setTenantId(VALID_TENANT_ID);
    timeEntry.setOwnerId(ownerId);
    timeEntry.setTimePeriodTypeId(VALID_TIME_PERIOD_TYPE_ID);
    timeEntry.setShiftType(EMPTY_STRING);
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
    kafkaMessage.put("action", KafkaAction.CREATE.toString());


    return mapper.writeValueAsString(kafkaMessage);
  }

  public static String createKafkaMessage(String schema,
                                          String version,
                                          ObjectNode resource) throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();

    ObjectNode kafkaMessage = mapper.createObjectNode();
    kafkaMessage.put("schema", String.format("%s, %s", schema, version));
    kafkaMessage.set("resource", resource);
    kafkaMessage.put("action", KafkaAction.CREATE.toString());


    return mapper.writeValueAsString(kafkaMessage);
  }


  public static ObjectNode createResourceJson(ObjectMapper mapper, String id,
                                                                   String ownerId) {
    ObjectNode resourceNode = mapper.createObjectNode();
    resourceNode.put("id", id);
    resourceNode.put("tenantId", VALID_TENANT_ID);
    resourceNode.put("ownerId", ownerId);
    resourceNode.put("timePeriodTypeId", VALID_TIME_PERIOD_TYPE_ID);
    resourceNode.put("shiftType", EMPTY_STRING);
    resourceNode.put("actualStartTime", VALID_START_TIME);
    resourceNode.put("actualEndTime", VALID_END_TIME);

    return resourceNode;
  }

  public static ObjectNode createResourceJson(ObjectMapper mapper, String id,
                                              String ownerId, String startTime, String endTime) {
    ObjectNode resourceNode = mapper.createObjectNode();
    resourceNode.put("id", id);
    resourceNode.put("tenantId", VALID_TENANT_ID);
    resourceNode.put("ownerId", ownerId);
    resourceNode.put("timePeriodTypeId", VALID_TIME_PERIOD_TYPE_ID);
    resourceNode.put("shiftType", EMPTY_STRING);
    resourceNode.put("actualStartTime", startTime);
    resourceNode.put("actualEndTime", endTime);

    return resourceNode;
  }


}
