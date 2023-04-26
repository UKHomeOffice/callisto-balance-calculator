package uk.gov.homeoffice.digital.sas.balancecalculator.kafka.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.INVALID_RESOURCE_SCHEMA;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.MESSAGE_INVALID_VERSION;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.MESSAGE_VALID_VERSION;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.VALID_END_TIME;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.VALID_RESOURCE_SCHEMA;
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
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.annotation.DirtiesContext;
import uk.gov.homeoffice.digital.sas.balancecalculator.BalanceCalculator;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.timecard.TimeEntry;
import uk.gov.homeoffice.digital.sas.balancecalculator.utils.TestUtils;
import uk.gov.homeoffice.digital.sas.kafka.exceptions.KafkaConsumerException;

@SpringBootTest
@ExtendWith({OutputCaptureExtension.class})
@AutoConfigureWireMock(port = 9999)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class TimeEntryConsumerTest {

  @Captor
  private ArgumentCaptor<TimeEntry> timeEntryCaptor;

  @Autowired
  TimeEntryConsumer timeEntryConsumer;

  ObjectMapper mapper = new ObjectMapper();

  @MockBean
  private BalanceCalculator balanceCalculator;

  @Test
  void onMessage_deserializeKafkaMessageAndLogSuccess_when_validMessageIsReceived
      (CapturedOutput capturedOutput) throws JsonProcessingException {

    // given
    String id = UUID.randomUUID().toString();
    String ownerId = UUID.randomUUID().toString();

    String message = TestUtils.createKafkaMessage(VALID_RESOURCE_SCHEMA, MESSAGE_VALID_VERSION,
        id, ownerId);

    //when
    timeEntryConsumer.onMessage(message);

    // then
    assertThat(capturedOutput.getOut()).contains(String.format(KAFKA_SUCCESSFUL_DESERIALIZATION,
        message));
    verify(balanceCalculator).calculate(timeEntryCaptor.capture());
    assertThat(timeEntryCaptor.getValue().getId()).isEqualTo(id);
  }

  @Test
  void onMessage_notDeserializeKafkaMessageAndThrowException_when_inValidResourceIsReceived() throws JsonProcessingException {
    //given
    String id = UUID.randomUUID().toString();
    String ownerId = UUID.randomUUID().toString();

    String message = TestUtils.createKafkaMessage(INVALID_RESOURCE_SCHEMA, MESSAGE_VALID_VERSION
        , id, ownerId);


    assertThatThrownBy(() -> timeEntryConsumer.onMessage(message))
        .isInstanceOf(KafkaConsumerException.class)
        .hasMessageContaining(String.format(KAFKA_RESOURCE_NOT_UNDERSTOOD,
            getSchemaFromMessageAsString(message)));

  }

  @Test
  void onMessage_notDeserializeKafkaMessageAndThrowException_inValidVersionIsReceived() throws JsonProcessingException {
    String id = UUID.randomUUID().toString();
    String ownerId = UUID.randomUUID().toString();

    String message = TestUtils.createKafkaMessage(VALID_RESOURCE_SCHEMA, MESSAGE_INVALID_VERSION
        , id, ownerId);

    assertThatThrownBy(() -> timeEntryConsumer.onMessage(message))
        .isInstanceOf(KafkaConsumerException.class)
        .hasMessageContaining(String.format(KAFKA_SCHEMA_INVALID_VERSION,
            getSchemaFromMessageAsString(message)));
  }

  @Test
  void onMessage_notDeserializeKafkaMessage_extraFieldReceived() throws JsonProcessingException {
    String id = UUID.randomUUID().toString();
    String ownerId = UUID.randomUUID().toString();

    ObjectNode resource = mapper.createObjectNode();
    resource.put("id", id);
    resource.put("tenantId", VALID_TENANT_ID);
    resource.put("ownerId", ownerId);
    resource.put("timePeriodTypeId", VALID_TIME_PERIOD_TYPE_ID);
    resource.put("shiftType", " ");
    resource.put("actualStartTime", VALID_START_TIME);
    resource.put("actualEndTime", VALID_END_TIME);
    resource.put("extraField", "EXTRA_FIELD");

    String message = TestUtils.createKafkaMessage(VALID_RESOURCE_SCHEMA, MESSAGE_VALID_VERSION,
        resource);

    assertThatThrownBy(() -> timeEntryConsumer.onMessage(message))
        .isInstanceOf(KafkaConsumerException.class)
        .hasMessageContaining(String.format(KAFKA_COULD_NOT_DESERIALIZE_RESOURCE,
            getResourceFromMessageAsString(message)));
  }

  @Test
  void onMessage_notDeserializeKafkaMessage_invalidDateFormatReceived() throws JsonProcessingException {
    String id = UUID.randomUUID().toString();
    String ownerId = UUID.randomUUID().toString();

    ObjectNode resource = createResourceJson(id, ownerId, "3GJN-TH-01T15:TGSJU",
        "3GHH-TH-01T15:GHJU");

    String message = TestUtils.createKafkaMessage(VALID_RESOURCE_SCHEMA, MESSAGE_VALID_VERSION
        , resource);

    assertThatThrownBy(() -> timeEntryConsumer.onMessage(message))
        .isInstanceOf(KafkaConsumerException.class);
  }
}