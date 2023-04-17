package uk.gov.homeoffice.digital.sas.balancecalculator.kafka.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.MESSAGE_INVALID_RESOURCE;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.MESSAGE_INVALID_VERSION;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.MESSAGE_VALID_RESOURCE;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.MESSAGE_VALID_VERSION;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.VALID_END_TIME;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.VALID_START_TIME;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.VALID_TENANT_ID;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.VALID_TIME_PERIOD_TYPE_ID;
import static uk.gov.homeoffice.digital.sas.balancecalculator.utils.TestUtils.createResourceJson;
import static uk.gov.homeoffice.digital.sas.kafka.constants.Constants.KAFKA_COULD_NOT_DESERIALIZE_RESOURCE;
import static uk.gov.homeoffice.digital.sas.kafka.constants.Constants.KAFKA_RESOURCE_NOT_UNDERSTOOD;
import static uk.gov.homeoffice.digital.sas.kafka.constants.Constants.KAFKA_SCHEMA_INVALID_VERSION;
import static uk.gov.homeoffice.digital.sas.kafka.constants.Constants.KAFKA_SUCCESSFUL_DESERIALIZATION;
import static uk.gov.homeoffice.digital.sas.kafka.consumer.KafkaConsumerUtils.getResourceFromMessageAsString;
import static uk.gov.homeoffice.digital.sas.kafka.consumer.KafkaConsumerUtils.getSchemaFromMessageAsString;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.annotation.DirtiesContext;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.TimeEntry;
import uk.gov.homeoffice.digital.sas.balancecalculator.utils.TestUtils;
import uk.gov.homeoffice.digital.sas.kafka.exceptions.KafkaConsumerException;

@SpringBootTest
@ExtendWith({OutputCaptureExtension.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class BalanceCalculatorConsumerServiceTest {

  @Autowired
  BalanceCalculatorConsumerService balanceCalculatorConsumerService;

  @Test
  void onMessage_deserializeKafkaMessageAndLogSuccess_when_validMessageIsReceived
      (CapturedOutput capturedOutput) throws JsonProcessingException {

    // given
    String id = UUID.randomUUID().toString();
    String ownerId = UUID.randomUUID().toString();

    TimeEntry expectedTimeEntry = TestUtils.createTimeEntry(
        id,
        ownerId,
        TestUtils.getAsDate(LocalDateTime.now()),
        TestUtils.getAsDate(LocalDateTime.now().plusHours(1)));

    String message = TestUtils.createKafkaMessage(MESSAGE_VALID_RESOURCE, MESSAGE_VALID_VERSION,
        id, ownerId);

    //when
    balanceCalculatorConsumerService.onMessage(message);

    // then
    assertThat(capturedOutput.getOut()).contains(String.format(KAFKA_SUCCESSFUL_DESERIALIZATION,
        message));
  }

  //Invalid resource throws error
  @Test
  void onMessage_notDeserializeKafkaMessageAndThrowException_when_inValidResourceIsReceived() throws JsonProcessingException {
    //given
    String id = UUID.randomUUID().toString();
    String ownerId = UUID.randomUUID().toString();

    String message = TestUtils.createKafkaMessage(MESSAGE_INVALID_RESOURCE, MESSAGE_VALID_VERSION
        , id, ownerId);


    assertThatThrownBy(() -> {
     balanceCalculatorConsumerService.onMessage(message);
    }).isInstanceOf(KafkaConsumerException.class)
        .hasMessageContaining(String.format(KAFKA_RESOURCE_NOT_UNDERSTOOD,
            getSchemaFromMessageAsString(message)));

  }

  //Invalid version throws error
  @Test
  void onMessage_notDeserializeKafkaMessageAndThrowException_inValidVersionIsReceived() throws JsonProcessingException {
    String id = UUID.randomUUID().toString();
    String ownerId = UUID.randomUUID().toString();

    String message = TestUtils.createKafkaMessage(MESSAGE_VALID_RESOURCE, MESSAGE_INVALID_VERSION
        , id, ownerId);

    assertThatThrownBy(() -> {
      balanceCalculatorConsumerService.onMessage(message);
    }).isInstanceOf(KafkaConsumerException.class)
        .hasMessageContaining(String.format(KAFKA_SCHEMA_INVALID_VERSION,
            getSchemaFromMessageAsString(message)));
  }

  //Desearilization error (missing/extra field?) on resource
  @Test
  void onMessage_notDeserializeKafkaMessage_extraFieldReceived() throws JsonProcessingException {
    String id = UUID.randomUUID().toString();
    String ownerId = UUID.randomUUID().toString();

    ObjectMapper mapper = new ObjectMapper();
    ObjectNode resource = mapper.createObjectNode();
    resource.put("id", id);
    resource.put("tenantId", VALID_TENANT_ID);
    resource.put("ownerId", ownerId);
    resource.put("timePeriodTypeId", VALID_TIME_PERIOD_TYPE_ID);
    resource.put("shiftType", " ");
    resource.put("actualStartTime", VALID_START_TIME);
    resource.put("actualEndTime", VALID_END_TIME);
    resource.put("extraField", "EXTRA_FIELD");

    String message = TestUtils.createKafkaMessage(MESSAGE_VALID_RESOURCE, MESSAGE_VALID_VERSION,
        resource);

    assertThatThrownBy(() -> {
      balanceCalculatorConsumerService.onMessage(message);
    }).isInstanceOf(KafkaConsumerException.class)
        .hasMessageContaining(String.format(KAFKA_COULD_NOT_DESERIALIZE_RESOURCE,
            getResourceFromMessageAsString(message)));
  }

  //Invalid date format received
  @Test
  void onMessage_notDeserializeKafkaMessage_invalidDateFormatReceived() throws JsonProcessingException {
    String id = UUID.randomUUID().toString();
    String ownerId = UUID.randomUUID().toString();

    ObjectNode resource = createResourceJson(id, ownerId, "3GJN-TH-01T15:TGSJU",
        "3GHH-TH-01T15:GHJU");

    String message = TestUtils.createKafkaMessage(MESSAGE_VALID_RESOURCE, MESSAGE_VALID_VERSION
        , resource);

    assertThatThrownBy(() -> {
      balanceCalculatorConsumerService.onMessage(message);
    }).isInstanceOf(KafkaConsumerException.class);
  }
}