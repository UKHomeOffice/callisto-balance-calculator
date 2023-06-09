package uk.gov.homeoffice.digital.sas.balancecalculator.testutils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.EMPTY_STRING;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.VALID_END_TIME;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.VALID_PERSON_ID;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.VALID_START_TIME;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.VALID_TENANT_ID;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.VALID_TIME_ENTRY_ID;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.VALID_TIME_PERIOD_TYPE_ID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.springframework.util.ResourceUtils;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Accrual;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.enums.AccrualType;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.timecard.TimeEntry;

public class CommonUtils {

  private static final ObjectMapper mapper = new ObjectMapper();

  public static TimeEntry createTimeEntry(String startTime,
                                          String finishTime) {
    return createTimeEntry(VALID_TIME_ENTRY_ID, VALID_TENANT_ID, VALID_PERSON_ID,
        startTime, finishTime);
  }

  public static TimeEntry createTimeEntry(String id, String ownerId, String startTime,
                                          String finishTime) {
    return createTimeEntry(id, VALID_TENANT_ID, ownerId, startTime, finishTime);
  }

  public static TimeEntry createTimeEntry(String id, String tenantId, String ownerId,
                                          String startTime, String finishTime) {

    var timeEntry = new TimeEntry();
    timeEntry.setId(id);
    timeEntry.setTenantId(tenantId);
    timeEntry.setOwnerId(ownerId);
    timeEntry.setTimePeriodTypeId(VALID_TIME_PERIOD_TYPE_ID);
    timeEntry.setShiftType(EMPTY_STRING);
    timeEntry.setActualStartTime(ZonedDateTime.parse(startTime));
    timeEntry.setActualEndTime(ZonedDateTime.parse(finishTime));
    return timeEntry;
  }

  public static String createKafkaMessage(String schema,
                                          String version,
                                          String id,
                                          String ownerId) throws JsonProcessingException {

    ObjectNode resource = createResourceJson(id, ownerId);

    ObjectNode kafkaMessage = mapper.createObjectNode();
    kafkaMessage.put("schema", String.format("%s, %s", schema, version));
    kafkaMessage.set("resource", resource);
    kafkaMessage.put("action", "CREATE");

    return mapper.writeValueAsString(kafkaMessage);
  }

  public static String createKafkaMessage(String schema,
                                          String version,
                                          ObjectNode resource) throws JsonProcessingException {

    ObjectNode kafkaMessage = mapper.createObjectNode();
    kafkaMessage.put("schema", String.format("%s, %s", schema, version));
    kafkaMessage.set("resource", resource);
    kafkaMessage.put("action", "CREATE");

    return mapper.writeValueAsString(kafkaMessage);
  }

  public static Accrual createAccrual(UUID id) {
    return Accrual.builder()
        .id(id)
        .tenantId(UUID.randomUUID())
        .build();
  }

  public static ObjectNode createResourceJson(String id, String ownerId) {
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

  public static ObjectNode createResourceJson(String id,
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

  public static List<Accrual> loadAccrualsFromFile(String filePath) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());

    File file = ResourceUtils.getFile("classpath:" + filePath);
    return mapper.readValue(file, new TypeReference<>() {
    });
  }

  public static  <T> T loadObjectFromFile(String filePath, Class<T> type) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());

    File file = ResourceUtils.getFile("classpath:" + filePath);

    return mapper.readValue(file, type);
  }

  public static void assertTypeAndDateAndTotals(Accrual accrual, AccrualType expectedAccrualType,
                                                String expectedAccrualDate,
                                                int expectedContributionTotal,
                                                int expectedCumulativeTotal) {
    assertThat(accrual.getAccrualType()).isEqualTo(expectedAccrualType);
    assertThat(accrual.getAccrualDate()).hasToString(expectedAccrualDate);
    assertThat(accrual.getContributions().getTotal())
        .usingComparator(BigDecimal::compareTo)
        .isEqualTo(BigDecimal.valueOf(expectedContributionTotal));
    assertThat(accrual.getCumulativeTotal())
        .usingComparator(BigDecimal::compareTo)
        .isEqualTo(BigDecimal.valueOf(expectedCumulativeTotal));
  }

  public static void assertTypeAndDateAndTotalsForMultipleAccruals(List<Accrual> accruals,
                                                                   AccrualType expectedAccrualType,
                                                                   String[] expectedAccrualDates,
                                                                   int[] expectedCumulativeTotals,
                                                                   int[] expectedContributionTotals) {

    // Check the 4 collections have the same size
    assertEquals(accruals.size(), expectedAccrualDates.length);
    assertEquals(expectedAccrualDates.length, expectedCumulativeTotals.length, expectedContributionTotals.length);

    IntStream.range(0, expectedAccrualDates.length).forEach(i -> {
      assertThat(accruals.get(i).getAccrualType()).isEqualTo(expectedAccrualType);
      assertThat(accruals.get(i).getAccrualDate()).hasToString(expectedAccrualDates[i]);
      assertThat(accruals.get(i).getCumulativeTotal())
          .usingComparator(BigDecimal::compareTo)
          .isEqualTo(BigDecimal.valueOf(expectedCumulativeTotals[i]));
      assertThat(accruals.get(i).getContributions().getTotal())
          .usingComparator(BigDecimal::compareTo)
          .isEqualTo(BigDecimal.valueOf(expectedContributionTotals[i]));
    });
  }
}
