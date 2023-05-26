package uk.gov.homeoffice.digital.sas.balancecalculator.kafka.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.awaitility.Awaitility.waitAtMost;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.INVALID_RESOURCE_SCHEMA;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.MESSAGE_INVALID_VERSION;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.MESSAGE_KEY;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.MESSAGE_VALID_VERSION;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.VALID_RESOURCE_SCHEMA;
import static uk.gov.homeoffice.digital.sas.balancecalculator.testutils.CommonUtils.createKafkaMessage;
import static uk.gov.homeoffice.digital.sas.balancecalculator.testutils.CommonUtils.createResourceJson;
import static uk.gov.homeoffice.digital.sas.kafka.constants.Constants.KAFKA_RESOURCE_NOT_UNDERSTOOD;
import static uk.gov.homeoffice.digital.sas.kafka.constants.Constants.KAFKA_SCHEMA_INVALID_VERSION;
import static uk.gov.homeoffice.digital.sas.kafka.constants.Constants.KAFKA_SUCCESSFUL_DESERIALIZATION;
import static uk.gov.homeoffice.digital.sas.kafka.consumer.KafkaConsumerUtils.getSchemaFromMessageAsString;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.timecard.TimeEntry;
import uk.gov.homeoffice.digital.sas.balancecalculator.testutils.CommonUtils;
import uk.gov.homeoffice.digital.sas.kafka.exceptions.KafkaConsumerException;
import uk.gov.homeoffice.digital.sas.kafka.message.KafkaAction;
import uk.gov.homeoffice.digital.sas.kafka.message.KafkaEventMessage;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ExtendWith({OutputCaptureExtension.class})
@EmbeddedKafka(
    partitions = 1
)
@AutoConfigureWireMock(port = 9999)
@TestPropertySource(properties = {
    "spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "spring.kafka.consumer.bootstrap-servers=${spring.embedded.kafka.brokers}"})
class KafkaConsumerIntegrationTest {

  private static final String TIME_ENTRY_ID = UUID.randomUUID().toString();
  private static final String OWNER_ID = UUID.randomUUID().toString();
  public static final ZonedDateTime SHIFT_START_TIME = LocalDateTime.now().atZone(ZoneOffset.UTC);
  public static final ZonedDateTime SHIFT_END_TIME = SHIFT_START_TIME.plusHours(1);

  @Value("${spring.kafka.template.default-topic}")
  private String topicName;

  @Autowired
  KafkaTemplate<String, KafkaEventMessage<TimeEntry>> kafkaTemplate;

  KafkaEventMessage<TimeEntry> kafkaEventMessage;

  @Autowired
  TimeEntryConsumer consumerService;

  @Test
  void should_logSuccessMessage_when_messageValid(CapturedOutput capturedOutput)
      throws JsonProcessingException {
    // Given

    TimeEntry timeEntry = CommonUtils.createTimeEntry(TIME_ENTRY_ID, OWNER_ID,
        SHIFT_START_TIME.toString(),
        SHIFT_END_TIME.toString());
    kafkaEventMessage = new KafkaEventMessage<>(MESSAGE_VALID_VERSION, timeEntry,
        KafkaAction.CREATE);

    // When
    kafkaTemplate.send(topicName, MESSAGE_KEY, kafkaEventMessage);
    // Then

    String expectedPayload = createKafkaMessage(VALID_RESOURCE_SCHEMA, MESSAGE_VALID_VERSION,
        createResourceJson(timeEntry.getId(), timeEntry.getOwnerId(),
            timeEntry.getActualStartTime().toString(), timeEntry.getActualEndTime().toString()));
    waitAtMost(3, TimeUnit.SECONDS)
        .untilAsserted(() ->
          assertThat(capturedOutput.getOut()).contains(String.format(KAFKA_SUCCESSFUL_DESERIALIZATION,
              expectedPayload))
        );
  }

  @Test
  void should_throwException_when_versionInvalid() throws JsonProcessingException {
    // Given
    TimeEntry timeEntry = CommonUtils.createTimeEntry(TIME_ENTRY_ID, OWNER_ID,
        SHIFT_START_TIME.toString(),
        SHIFT_END_TIME.toString());

    String message = createKafkaMessage(VALID_RESOURCE_SCHEMA, MESSAGE_INVALID_VERSION
        , TIME_ENTRY_ID, OWNER_ID);

    kafkaEventMessage = new KafkaEventMessage<>(MESSAGE_INVALID_VERSION, timeEntry,
        KafkaAction.CREATE);
    // When
    kafkaTemplate.send(topicName, MESSAGE_KEY, kafkaEventMessage);
    // Then
    waitAtMost(3, TimeUnit.SECONDS)
        .untilAsserted(() ->
          assertThatThrownBy(() ->
            consumerService.onMessage(message))
              .isInstanceOf(KafkaConsumerException.class)
              .hasMessageContaining(String.format(KAFKA_SCHEMA_INVALID_VERSION,
                  getSchemaFromMessageAsString(message)))
        );
  }

  @Test
  void should_throwException_when_resourceInvalid() throws JsonProcessingException {
    // Given
    TimeEntry timeEntry = CommonUtils.createTimeEntry(TIME_ENTRY_ID, OWNER_ID,
        SHIFT_START_TIME.toString(),
        SHIFT_END_TIME.toString());

    String message = createKafkaMessage(INVALID_RESOURCE_SCHEMA, MESSAGE_VALID_VERSION
        , TIME_ENTRY_ID, OWNER_ID);

    kafkaEventMessage = new KafkaEventMessage<>(MESSAGE_INVALID_VERSION, timeEntry,
        KafkaAction.CREATE);
    // When
    kafkaTemplate.send(topicName, MESSAGE_KEY, kafkaEventMessage);
    // Then
    waitAtMost(3, TimeUnit.SECONDS)
        .untilAsserted(() ->
          assertThatThrownBy(() -> consumerService.onMessage(message))
              .isInstanceOf(KafkaConsumerException.class)
              .hasMessageContaining(String.format(KAFKA_RESOURCE_NOT_UNDERSTOOD,
                  getSchemaFromMessageAsString(message)))
        );
  }
}
