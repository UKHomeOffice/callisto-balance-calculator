package uk.gov.homeoffice.digital.sas.balancecalculator.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import java.io.Serializable;
import java.util.Date;
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

  public static TimeEntry createTimeEntry(UUID ownerId, UUID tenantId, String actualStartTime,
                                          String actualEndTime) {
    var timeEntry = new TimeEntry();
    timeEntry.setOwnerId(ownerId.toString());
    timeEntry.setTenantId(tenantId.toString());
    timeEntry.setActualStartTime(actualStartTime);
    timeEntry.setActualEndTime(actualEndTime);
    return timeEntry;
  }

  public static <S extends Serializable> String createKafkaMessage(){
    String resource = createResourceJson();

    return new Gson().toJson(Map.of(
        "schema", "uk/gov/homeoffice/digital/sas/balancecalculator/models/TimeEntry, 0.1.0",
        "resource" , resource,
        "action", "CREATE"));
  }

  private static <S extends Serializable> String createResourceJson() {
    return new Gson().toJson(Map.of(
        "id", "c0a80018-870e-11b0-8187-0ea38cb30001",
        "tenantId", "00000000-0000-0000-0000-000000000000",
        "ownerId", "3343a960-de03-42ba-8769-767404fb2fcf",
        "timePeriodTypeId", "00000000-0000-0000-0000-000000000001",
        "shiftType", " ",
        "actualStartTime", "1679456400000",
        "actualEndTime", "1679457000000"
    ));
  }

}
