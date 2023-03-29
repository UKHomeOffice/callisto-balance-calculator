package uk.gov.homeoffice.digital.sas.balancecalculator.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.minidev.json.parser.ParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import uk.gov.homeoffice.digital.sas.balancecalculator.config.TestConfig;
import uk.gov.homeoffice.digital.sas.kafka.message.KafkaAction;
import uk.gov.homeoffice.digital.sas.kafka.message.KafkaEventMessage;
import uk.gov.homeoffice.digital.sas.kafka.producer.KafkaProducerService;
import uk.gov.homeoffice.digital.sas.timecard.model.TimeEntry;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest(classes = TestConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@EmbeddedKafka(
    partitions = 3,
    brokerProperties = {
        "listeners=PLAINTEXT://localhost:3333",
        "port=3333"
    }
)
@TestPropertySource(properties = {
    "spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "spring.kafka.consumer.bootstrap-servers=${spring.embedded.kafka.brokers}"})
public class BalanceCalculatorConsumerServiceIntegrationTest {

  private final static String TOPIC_NAME = "callisto-timecard-timeentries";
  private final static int LATCH_TIMEOUT = 3;
  private final static String SCHEMA_VERSION = "0.1.0";

  private final static UUID OWNER_ID = UUID.fromString("ec703cac-de76-49c8-b1c4-83da6f8b42ce");
  private final static UUID TENANT_ID = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");

  private final static LocalDateTime EXISTING_SHIFT_START_TIME = LocalDateTime.of(
      2022, 1, 1, 9, 0, 0);
  private final static LocalDateTime EXISTING_SHIFT_END_TIME = LocalDateTime.of(
      2022, 1, 1, 17, 0, 0);

  private TimeEntry timeEntry;

  @Autowired
  KafkaProducerService<TimeEntry> kafkaProducerService;

  @Mock
  private KafkaTemplate<String, KafkaEventMessage<TimeEntry>> kafkaTemplate;


  @Autowired
  BalanceCalculatorConsumerService balanceCalculatorConsumerService;

  @BeforeEach
  void setup() {
    timeEntry = createTimeEntry(OWNER_ID, TENANT_ID,
            Date.from(EXISTING_SHIFT_START_TIME.toInstant(ZoneOffset.UTC)),
            Date.from(EXISTING_SHIFT_END_TIME.toInstant(ZoneOffset.UTC)));
  }

  @Test
  void should_validateConsumedMessage_when_messageNotNull () throws InterruptedException, ParseException {
    // given
    // when
    kafkaProducerService.sendMessage(OWNER_ID.toString(), timeEntry, KafkaAction.CREATE);
    balanceCalculatorConsumerService.getLatch().await(LATCH_TIMEOUT, TimeUnit.SECONDS);
    // THEN
    assertThat(balanceCalculatorConsumerService.getKafkaEventMessage()).isNotNull();

    KafkaEventMessage expectedKafkaEventMessage = generateExpectedKafkaEventMessage(
        SCHEMA_VERSION,
        timeEntry,
        KafkaAction.CREATE);

    isMessageDeserialized(expectedKafkaEventMessage);
  }


  private KafkaEventMessage generateExpectedKafkaEventMessage(String version, TimeEntry resource,
                                                              KafkaAction action) {
    return new KafkaEventMessage<>(version, resource, action);
  }

  public static TimeEntry createTimeEntry(UUID ownerId, UUID tenantId, Date actualStartTime,
                                          Date actualEndTime) {
    var timeEntry = new TimeEntry();
    timeEntry.setOwnerId(ownerId.toString());
    timeEntry.setTenantId(tenantId.toString());
    timeEntry.setActualStartTime(actualStartTime);
    timeEntry.setActualEndTime(actualEndTime);
    return timeEntry;
  }

  private void isMessageDeserialized(KafkaEventMessage expectedKafkaEventMessage) {

    assertThat(balanceCalculatorConsumerService.getKafkaEventMessage().getSchema()).isEqualTo(expectedKafkaEventMessage.getSchema());
    assertThat(balanceCalculatorConsumerService.getKafkaEventMessage().getAction()).isEqualTo(expectedKafkaEventMessage.getAction());

    isResourceDeserialized();
  }

  private void isResourceDeserialized() {

    TimeEntry actualTimeEntry = getTimeEntryAsConcreteType();
    assertAll(
        () -> assertThat(actualTimeEntry.getId()).isEqualTo(timeEntry.getId()),
        () -> assertThat(actualTimeEntry.getTenantId()).isEqualTo(timeEntry.getTenantId()),
        () -> assertThat(actualTimeEntry.getOwnerId()).isEqualTo(timeEntry.getOwnerId())
    );
  }

  //This method is needed due to Jackson defaulting to LinkedHasMap on deserialization with
  // generic types. It converts the LinkedHaspMap to a concrete type.
  private TimeEntry getTimeEntryAsConcreteType() {
    ObjectMapper mapper = new ObjectMapper();
    return mapper.convertValue(
        balanceCalculatorConsumerService.getKafkaEventMessage().getResource(), new TypeReference<>() {});
  }

}
