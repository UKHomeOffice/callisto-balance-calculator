package uk.gov.homeoffice.digital.sas.balancecalculator.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.TimeEntry;
import uk.gov.homeoffice.digital.sas.kafka.message.KafkaAction;
import uk.gov.homeoffice.digital.sas.kafka.message.KafkaEventMessage;
import uk.gov.homeoffice.digital.sas.kafka.validators.SchemaValidator;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.KAFKA_JSON_MESSAGE;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.MESSAGE_EXPECTED_SCHEMA;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.MESSAGE_INVALID_RESOURCE;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.MESSAGE_INVALID_VERSION;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.MESSAGE_VALID_RESOURCE;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.MESSAGE_VALID_VERSION;
import static uk.gov.homeoffice.digital.sas.kafka.constants.Constants.KAFKA_CONSUMING_MESSAGE;
import static uk.gov.homeoffice.digital.sas.kafka.constants.Constants.KAFKA_SCHEMA_INVALID_RESOURCE;
import static uk.gov.homeoffice.digital.sas.kafka.constants.Constants.KAFKA_SCHEMA_INVALID_VERSION;

@SpringBootTest
@ExtendWith({OutputCaptureExtension.class})
class BalanceCalculatorConsumerServiceTest {

  @Autowired
  private SchemaValidator schemaValidator;

  @Autowired
  private BalanceCalculatorConsumerService balanceCalculatorConsumerService;

  KafkaEventMessage<TimeEntry> expectedKafkaEventMessage;

  @Test
  void should_validateKafkaMessage_andLogValidated_when_validMessageReceived (CapturedOutput capturedOutput) throws JsonProcessingException {
    // given
    String message = String.format(KAFKA_JSON_MESSAGE, MESSAGE_VALID_RESOURCE, MESSAGE_VALID_VERSION);
    // when
    balanceCalculatorConsumerService.onMessage(message);
    // then
    assertThat(balanceCalculatorConsumerService.getKafkaEventMessage().getSchema()).isEqualTo(MESSAGE_EXPECTED_SCHEMA);
    assertThat(balanceCalculatorConsumerService.getKafkaEventMessage().getAction()).isEqualTo(KafkaAction.CREATE);
    assertThat(capturedOutput.getOut()).contains(String.format(KAFKA_CONSUMING_MESSAGE, message));
  }

  @Test
  void should_returnNull_andLogError_when_invalidSchemaResource (CapturedOutput capturedOutput) throws JsonProcessingException {
    // given
    String message = String.format(KAFKA_JSON_MESSAGE, MESSAGE_INVALID_RESOURCE, MESSAGE_VALID_VERSION);
    // when
    balanceCalculatorConsumerService.onMessage(message);
    // then
    assertThat(balanceCalculatorConsumerService.getKafkaEventMessage()).isNull();
    assertThat(capturedOutput.getOut()).contains(String.format(KAFKA_SCHEMA_INVALID_RESOURCE, MESSAGE_INVALID_RESOURCE));
  }

  @Test
  void should_returnNull_andLogError_when_invalidSchemaVersion (CapturedOutput capturedOutput) throws JsonProcessingException {
    // given
    String message = String.format(KAFKA_JSON_MESSAGE, MESSAGE_VALID_RESOURCE, MESSAGE_INVALID_VERSION);
    // when
    balanceCalculatorConsumerService.onMessage(message);
    // then
    assertThat(balanceCalculatorConsumerService.getKafkaEventMessage()).isNull();
    assertThat(capturedOutput.getOut()).contains(String.format(KAFKA_SCHEMA_INVALID_VERSION, MESSAGE_INVALID_VERSION));
  }

}