package uk.gov.homeoffice.digital.sas.balancecalculator.kafka.consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import uk.gov.homeoffice.digital.sas.balancecalculator.models.TimeEntry;
import uk.gov.homeoffice.digital.sas.balancecalculator.utils.TestUtils;
import uk.gov.homeoffice.digital.sas.kafka.message.KafkaAction;
import uk.gov.homeoffice.digital.sas.kafka.message.KafkaEventMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.Constants.KAFKA_SUCCESSFUL_DESERIALIZATION;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.Constants.KAFKA_UNSUCCESSFUL_DESERIALIZATION;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.KAFKA_JSON_MESSAGE;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.MESSAGE_EXPECTED_SCHEMA;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.MESSAGE_VALID_RESOURCE;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.MESSAGE_VALID_VERSION;

import java.util.UUID;

@SpringBootTest
@ExtendWith({OutputCaptureExtension.class})
class BalanceCalculatorConsumerServiceTest {

  @Autowired
  BalanceCalculatorConsumerService balanceCalculatorConsumerService;

  private final static UUID OWNER_ID = UUID.fromString("ec703cac-de76-49c8-b1c4-83da6f8b42ce");
  private final static UUID TENANT_ID = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");

  private final static String EXISTING_SHIFT_START_TIME = "1641060000212";

  private final static String EXISTING_SHIFT_END_TIME = "1641063600212";

  TimeEntry expectedTimeEntry = TestUtils.createTimeEntry(OWNER_ID, TENANT_ID,
                              EXISTING_SHIFT_START_TIME, EXISTING_SHIFT_END_TIME);

  @Test
  void should_deserializeKafkaMessageAndLogSuccess_when_validMessageIsReceived
      (CapturedOutput capturedOutput) {

    // given
    String message = TestUtils.createKafkaMessage();

    // when
    balanceCalculatorConsumerService.onMessage(message);
    // then
    assertThat(balanceCalculatorConsumerService.getKafkaEventMessage().getSchema()).isEqualTo(MESSAGE_EXPECTED_SCHEMA);
    assertThat(balanceCalculatorConsumerService.getKafkaEventMessage().getAction()).isEqualTo(KafkaAction.CREATE);
    assertMessageIsDeserializedAsExpected();
    assertThat(capturedOutput.getOut()).contains(String.format(KAFKA_SUCCESSFUL_DESERIALIZATION,
        expectedTimeEntry));
  }

  @Test
  void should_notDeserializeKafkaMessageAndLogSuccess_when_inValidMessageIsReceived
      (CapturedOutput capturedOutput) {
    //given
    String message =
        String.format(KAFKA_JSON_MESSAGE, MESSAGE_VALID_RESOURCE, MESSAGE_VALID_VERSION);
    // when
    balanceCalculatorConsumerService.onMessage(message);
    // then
    assertThat(balanceCalculatorConsumerService.getKafkaEventMessage()).isNull();
    assertThat(capturedOutput.getOut()).contains(String.format(KAFKA_UNSUCCESSFUL_DESERIALIZATION,
        expectedTimeEntry));


  }

  private void assertMessageIsDeserializedAsExpected() {

    KafkaEventMessage<TimeEntry> expectedKafkaEventMessage =
        TestUtils.generateExpectedKafkaEventMessage(MESSAGE_VALID_VERSION,
        expectedTimeEntry,
        KafkaAction.CREATE);

    assertThat(balanceCalculatorConsumerService.getKafkaEventMessage().getSchema()).isEqualTo("uk.gov.homeoffice.digital.sas.timecard.model.TimeEntry, 0.1.0");
    assertThat(balanceCalculatorConsumerService.getKafkaEventMessage().getAction()).isEqualTo(expectedKafkaEventMessage.getAction());

    assertResourceIsDeserializedAsExpected(
        balanceCalculatorConsumerService.getKafkaEventMessage().getResource(),
        expectedKafkaEventMessage.getResource());
  }

  private void assertResourceIsDeserializedAsExpected(TimeEntry actualTimeEntry,
                                                      TimeEntry timeEntry) {

    assertAll(
        () -> assertThat(actualTimeEntry.getId()).isEqualTo(timeEntry.getId()),
        () -> assertThat(actualTimeEntry.getTenantId()).isEqualTo(timeEntry.getTenantId()),
        () -> assertThat(actualTimeEntry.getOwnerId()).isEqualTo(timeEntry.getOwnerId())
    );
  }

}