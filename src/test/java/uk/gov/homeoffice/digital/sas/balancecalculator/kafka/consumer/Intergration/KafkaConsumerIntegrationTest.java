package uk.gov.homeoffice.digital.sas.balancecalculator.kafka.consumer.Intergration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import uk.gov.homeoffice.digital.sas.balancecalculator.kafka.consumer.BalanceCalculatorConsumerService;

import uk.gov.homeoffice.digital.sas.balancecalculator.models.TimeEntry;
import uk.gov.homeoffice.digital.sas.balancecalculator.utils.TestUtils;
import uk.gov.homeoffice.digital.sas.kafka.message.KafkaAction;
import uk.gov.homeoffice.digital.sas.kafka.message.KafkaEventMessage;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.waitAtMost;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.MESSAGE_INVALID_VERSION;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.MESSAGE_KEY;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.MESSAGE_VALID_VERSION;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
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

  TimeEntry timeEntry;

  @Autowired
  BalanceCalculatorConsumerService consumerService;

  @BeforeEach
  void setup() {
    timeEntry = TestUtils.createTimeEntry();
    kafkaEventMessage = new KafkaEventMessage<>(MESSAGE_VALID_VERSION,  timeEntry,
        KafkaAction.CREATE);
  }

  @Test
  void should_validateConsumedMessage_when_messageNotNull() {
    // Given
    // When
    kafkaTemplate.send(topicName, MESSAGE_KEY, kafkaEventMessage);
    // Then
    waitAtMost(3, TimeUnit.SECONDS)
        .untilAsserted(() -> {
          assertThat(consumerService.getKafkaEventMessage()).isNotNull();
        });

    KafkaEventMessage<TimeEntry> expectedKafkaEventMessage = TestUtils.generateExpectedKafkaEventMessage(
        MESSAGE_VALID_VERSION,
        timeEntry,
        KafkaAction.CREATE);

    isMessageDeserialized(expectedKafkaEventMessage);
  }

  @Test
  void should_validateConsumedMessage_when_messageNull () {
    // Given
    kafkaEventMessage = new KafkaEventMessage<>(MESSAGE_INVALID_VERSION,  timeEntry, KafkaAction.CREATE);
    // When
    kafkaTemplate.send(topicName, MESSAGE_KEY, kafkaEventMessage);
    // Then
    waitAtMost(3, TimeUnit.SECONDS)
        .untilAsserted(() -> {
          assertThat(consumerService.getKafkaEventMessage()).isNull();
        });
  }

  private void isMessageDeserialized(KafkaEventMessage<TimeEntry> expectedKafkaEventMessage) {
    assertThat(consumerService.getKafkaEventMessage().getSchema()).isEqualTo(expectedKafkaEventMessage.getSchema());
    assertThat(consumerService.getKafkaEventMessage().getAction()).isEqualTo(expectedKafkaEventMessage.getAction());
  }



}
