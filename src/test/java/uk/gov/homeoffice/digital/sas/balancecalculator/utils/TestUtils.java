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

  public static TimeEntry createTimeEntry() {
    var timeEntry = new TimeEntry();
    timeEntry.setId("c0a80018-870e-11b0-8187-0ea38cb30001");
    timeEntry.setTenantId("3fa85f64-5717-4562-b3fc-2c963f66afa6");
    timeEntry.setOwnerId("3343a960-de03-42ba-8769-767404fb2fcf");
    timeEntry.setTimePeriodTypeId("00000000-0000-0000-0000-000000000001");
    timeEntry.setShiftType(" ");
    timeEntry.setActualStartTime("1679456400000");
    timeEntry.setActualEndTime("1679457000000");
    return timeEntry;
  }

  public static <S extends Serializable> String createKafkaMessage(String version){
    String resource = createResourceJson();

    return new Gson().toJson(Map.of(
        "schema", String.format("uk.gov.homeoffice.digital.sas.timecard.model.TimeEntry, %s",
            version),
        "resource" , resource,
        "action", "CREATE"));
  }

  private static <S extends Serializable> String createResourceJson() {
    return new Gson().toJson(Map.of(
        "id", "c0a80018-870e-11b0-8187-0ea38cb30001",
        "tenantId", "3fa85f64-5717-4562-b3fc-2c963f66afa6",
        "ownerId", "3343a960-de03-42ba-8769-767404fb2fcf",
        "timePeriodTypeId", "00000000-0000-0000-0000-000000000001",
        "shiftType", " ",
        "actualStartTime", "1679456400000",
        "actualEndTime", "1679457000000"
    ));
  }

}
