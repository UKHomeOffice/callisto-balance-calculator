package uk.gov.homeoffice.digital.sas.balancecalculator.kafka.consumer;

import org.apache.kafka.common.security.oauthbearer.secured.ValidateException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import scala.reflect.internal.Trees;
import uk.gov.homeoffice.digital.sas.balancecalculator.kafka.consumer.BalanceCalculatorConsumerService;

import uk.gov.homeoffice.digital.sas.balancecalculator.models.TimeEntry;
import uk.gov.homeoffice.digital.sas.balancecalculator.utils.TestUtils;
import uk.gov.homeoffice.digital.sas.kafka.exceptions.KafkaConsumerException;
import uk.gov.homeoffice.digital.sas.kafka.message.KafkaAction;
import uk.gov.homeoffice.digital.sas.kafka.message.KafkaEventMessage;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.awaitility.Awaitility.waitAtMost;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.Constants.KAFKA_SUCCESSFUL_DESERIALIZATION;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.Constants.KAFKA_UNSUCCESSFUL_DESERIALIZATION;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.MESSAGE_INVALID_RESOURCE;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.MESSAGE_INVALID_VERSION;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.MESSAGE_KEY;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.MESSAGE_VALID_RESOURCE;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.MESSAGE_VALID_VERSION;
import static uk.gov.homeoffice.digital.sas.kafka.constants.Constants.KAFKA_SCHEMA_INVALID_VERSION;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ExtendWith({OutputCaptureExtension.class})
@EmbeddedKafka(
    partitions = 1
)
@TestPropertySource(properties = {
    "spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "spring.kafka.consumer.bootstrap-servers=${spring.embedded.kafka.brokers}"})
class KafkaConsumerIntegrationTest {

  @Value("${spring.kafka.template.default-topic}")
  private String topicName;

  @Autowired
  KafkaTemplate<String, KafkaEventMessage<TimeEntry>> kafkaTemplate;

  KafkaEventMessage<TimeEntry> kafkaEventMessage;

  @Autowired
  BalanceCalculatorConsumerService consumerService;

  @Test
  void should_logSuccessMessage_when_messageValid(CapturedOutput capturedOutput) {
    // Given
    String id = UUID.randomUUID().toString();
    String ownerId = UUID.randomUUID().toString();

    TimeEntry timeEntry = TestUtils.createTimeEntry(id, ownerId,
        TestUtils.getAsDate(LocalDateTime.now()),
        TestUtils.getAsDate(LocalDateTime.now().plusHours(1)));
    kafkaEventMessage = new KafkaEventMessage<>(MESSAGE_VALID_VERSION, timeEntry,
        KafkaAction.CREATE);

    // When
    kafkaTemplate.send(topicName, MESSAGE_KEY, kafkaEventMessage);
    // Then
    waitAtMost(3, TimeUnit.SECONDS)
        .untilAsserted(() -> {
          assertThat(capturedOutput.getOut().contains(String.format(KAFKA_SUCCESSFUL_DESERIALIZATION,
              timeEntry.getId())));
        });
  }

  @Test
  void should_throwException_when_versionInvalid() throws JsonProcessingException {
    // Given
    String id = UUID.randomUUID().toString();
    String ownerId = UUID.randomUUID().toString();

    TimeEntry timeEntry = TestUtils.createTimeEntry(id, ownerId,
        TestUtils.getAsDate(LocalDateTime.now()),
        TestUtils.getAsDate(LocalDateTime.now().plusHours(1)));

    String message = TestUtils.createKafkaMessage(MESSAGE_VALID_RESOURCE, MESSAGE_INVALID_VERSION
        , id, ownerId);

    kafkaEventMessage = new KafkaEventMessage<>(MESSAGE_INVALID_VERSION, timeEntry,
        KafkaAction.CREATE);
    // When
    kafkaTemplate.send(topicName, MESSAGE_KEY, kafkaEventMessage);
    // Then
    waitAtMost(3, TimeUnit.SECONDS)
        .untilAsserted(() -> {
          assertThatThrownBy(() -> {
            consumerService.onMessage(message);
          }).isInstanceOf(KafkaConsumerException.class)
              .hasMessageContaining(String.format(KAFKA_SCHEMA_INVALID_VERSION, message));
          });
  }

  @Test
  void should_throwException_when_resourceInvalid() throws JsonProcessingException {
    // Given
    String id = UUID.randomUUID().toString();
    String ownerId = UUID.randomUUID().toString();

    TimeEntry timeEntry = TestUtils.createTimeEntry(id, ownerId,
        TestUtils.getAsDate(LocalDateTime.now()),
        TestUtils.getAsDate(LocalDateTime.now().plusHours(1)));

    String message = TestUtils.createKafkaMessage(MESSAGE_INVALID_RESOURCE, MESSAGE_VALID_VERSION
        , id, ownerId);

    kafkaEventMessage = new KafkaEventMessage<>(MESSAGE_INVALID_VERSION, timeEntry,
        KafkaAction.CREATE);
    // When
    kafkaTemplate.send(topicName, MESSAGE_KEY, kafkaEventMessage);
    // Then
    waitAtMost(3, TimeUnit.SECONDS)
        .untilAsserted(() -> {
          assertThatThrownBy(() -> {
            consumerService.onMessage(message);
          }).isInstanceOf(KafkaConsumerException.class)
              .hasMessageContaining(String.format(KAFKA_UNSUCCESSFUL_DESERIALIZATION, message));
        });
  }

}