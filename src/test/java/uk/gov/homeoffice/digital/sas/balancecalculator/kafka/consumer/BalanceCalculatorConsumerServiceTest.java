package uk.gov.homeoffice.digital.sas.balancecalculator.kafka.consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.annotation.DirtiesContext;

import uk.gov.homeoffice.digital.sas.balancecalculator.models.TimeEntry;
import uk.gov.homeoffice.digital.sas.balancecalculator.utils.TestUtils;
import uk.gov.homeoffice.digital.sas.kafka.message.KafkaAction;
import uk.gov.homeoffice.digital.sas.kafka.message.KafkaEventMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.Constants.KAFKA_SUCCESSFUL_DESERIALIZATION;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.Constants.KAFKA_UNSUCCESSFUL_DESERIALIZATION;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.MESSAGE_EXPECTED_SCHEMA;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.MESSAGE_INVALID_RESOURCE;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.MESSAGE_INVALID_VERSION;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.MESSAGE_VALID_RESOURCE;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.MESSAGE_VALID_VERSION;

@SpringBootTest
@ExtendWith({OutputCaptureExtension.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class BalanceCalculatorConsumerServiceTest {

  @Autowired
  BalanceCalculatorConsumerService balanceCalculatorConsumerService;

  TimeEntry expectedTimeEntry = TestUtils.createTimeEntry();

  @Test
  void onMessage_deserializeKafkaMessageAndLogSuccess_when_validMessageIsReceived
      (CapturedOutput capturedOutput) {

    // given
    String message = TestUtils.createKafkaMessage(MESSAGE_VALID_RESOURCE, MESSAGE_VALID_VERSION);

    // when
    balanceCalculatorConsumerService.onMessage(message);
    // then
    assertThat(balanceCalculatorConsumerService.getKafkaEventMessage().getSchema()).isEqualTo(MESSAGE_EXPECTED_SCHEMA);
    assertThat(balanceCalculatorConsumerService.getKafkaEventMessage().getAction()).isEqualTo(KafkaAction.CREATE);
    assertMessageIsDeserializedAsExpected();
    assertThat(capturedOutput.getOut()).contains(String.format(KAFKA_SUCCESSFUL_DESERIALIZATION,
        expectedTimeEntry.getId()));
  }

  @Test
  void onMessage_notDeserializeKafkaMessageAndLogFailure_when_inValidMessageIsReceived
      (CapturedOutput capturedOutput) {
    //given
    String message = TestUtils.createKafkaMessage(MESSAGE_VALID_RESOURCE, MESSAGE_INVALID_VERSION);
    // when
    balanceCalculatorConsumerService.onMessage(message);
    // then
    assertThat(balanceCalculatorConsumerService.getKafkaEventMessage()).isNull();
    assertThat(capturedOutput.getOut()).contains(String.format(KAFKA_UNSUCCESSFUL_DESERIALIZATION,
        message));
  }

  @Test
  void onMessage_notDeserializeKafkaMessage_when_schemaNotTimeEntry(CapturedOutput capturedOutput){
    String message = TestUtils.createKafkaMessage(MESSAGE_INVALID_RESOURCE,
        MESSAGE_VALID_VERSION);

    balanceCalculatorConsumerService.onMessage(message);

    assertThat(balanceCalculatorConsumerService.getKafkaEventMessage()).isNull();
    assertThat(capturedOutput.getOut()).contains(String.format(KAFKA_UNSUCCESSFUL_DESERIALIZATION,
        message));
  }

  private void assertMessageIsDeserializedAsExpected() {

    KafkaEventMessage<TimeEntry> expectedKafkaEventMessage =
        TestUtils.generateExpectedKafkaEventMessage(MESSAGE_VALID_VERSION,
        expectedTimeEntry,
        KafkaAction.CREATE);

    assertThat(balanceCalculatorConsumerService.getKafkaEventMessage().getSchema()).isEqualTo(MESSAGE_VALID_RESOURCE + ", " + MESSAGE_VALID_VERSION);
    assertThat(balanceCalculatorConsumerService.getKafkaEventMessage().getAction()).isEqualTo(expectedKafkaEventMessage.getAction());

    assertResourceIsDeserializedAsExpected(
        balanceCalculatorConsumerService.createTimeEntryFromKafkaEventMessage(),
        expectedKafkaEventMessage.getResource());
  }

  private void assertResourceIsDeserializedAsExpected(TimeEntry actualTimeEntry,
                                                      TimeEntry timeEntry) {

    assertAll(
        () -> assertThat(actualTimeEntry.getId()).isEqualTo(timeEntry.getId()),
        () -> assertThat(actualTimeEntry.getTenantId()).isEqualTo(timeEntry.getTenantId()),
        () -> assertThat(actualTimeEntry.getOwnerId()).isEqualTo(timeEntry.getOwnerId()),
        () -> assertThat(actualTimeEntry.getTimePeriodTypeId()).isEqualTo(timeEntry.getTimePeriodTypeId()),
        () -> assertThat(actualTimeEntry.getShiftType()).isEqualTo(timeEntry.getShiftType()),
        () -> assertThat(actualTimeEntry.getActualStartTime()).isEqualTo(timeEntry.getActualStartTime()),
        () -> assertThat(actualTimeEntry.getActualEndTime()).isEqualTo(timeEntry.getActualEndTime())
    );
  }

}