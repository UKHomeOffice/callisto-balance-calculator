package uk.gov.homeoffice.digital.sas.balancecalculator.kafka.consumer;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.kafka.common.security.oauthbearer.secured.ValidateException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.annotation.DirtiesContext;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.TimeEntry;
import uk.gov.homeoffice.digital.sas.balancecalculator.utils.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.Constants.KAFKA_SUCCESSFUL_DESERIALIZATION;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.MESSAGE_INVALID_RESOURCE;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.MESSAGE_INVALID_VERSION;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.MESSAGE_VALID_RESOURCE;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.MESSAGE_VALID_VERSION;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.OWNER_ID;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.TIME_ENTRY_ID;
import static uk.gov.homeoffice.digital.sas.balancecalculator.utils.TestUtils.createValidResourceJson;
import static uk.gov.homeoffice.digital.sas.balancecalculator.utils.Utils.createTimeJsonDeserializer;

@SpringBootTest
@ExtendWith({OutputCaptureExtension.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class BalanceCalculatorConsumerServiceTest {

  @Autowired
  BalanceCalculatorConsumerService balanceCalculatorConsumerService;

  @Test
  void onMessage_deserializeKafkaMessageAndLogSuccess_when_validMessageIsReceived
      (CapturedOutput capturedOutput) throws ClassNotFoundException {

    // given
    Gson gson = createTimeJsonDeserializer();

    String timeEntryJson = createValidResourceJson(TIME_ENTRY_ID, OWNER_ID);

    TimeEntry expectedTimeEntry = gson.fromJson(String.valueOf(timeEntryJson),
        new TypeToken<TimeEntry>() {}.getType());

    String message = TestUtils.createKafkaMessage(MESSAGE_VALID_RESOURCE, MESSAGE_VALID_VERSION,
        TIME_ENTRY_ID, OWNER_ID);

    //when
    balanceCalculatorConsumerService.onMessage(message);

    // then
    assertThat(capturedOutput.getOut()).contains(String.format(KAFKA_SUCCESSFUL_DESERIALIZATION,
        expectedTimeEntry.getId()));
  }

  @Test
  void onMessage_notDeserializeKafkaMessageAndThrowException_when_invalidResourceIsReceived() {
    //given
    String message = TestUtils.createKafkaMessage(MESSAGE_INVALID_RESOURCE, MESSAGE_VALID_VERSION
        ,TIME_ENTRY_ID, OWNER_ID);

    Assertions.assertThrows(ClassNotFoundException.class, () -> {
      balanceCalculatorConsumerService.onMessage(message);;
    });
  }


  @Test
  void onMessage_notDeserializeKafkaMessageAndThrowException_when_invalidVersionIsReceived() {
    //given
    String message = TestUtils.createKafkaMessage(MESSAGE_VALID_RESOURCE, MESSAGE_INVALID_VERSION
        , TIME_ENTRY_ID, OWNER_ID);

    Assertions.assertThrows(ValidateException.class, () -> {
      balanceCalculatorConsumerService.onMessage(message);;
    });
  }

  //Invalid date format received
  @Test
  void onMessage_notDeserializeKafkaMessageAndThrowException_when_invalidDateString() {
    //given
    String message = TestUtils.createKafkaInvalidMessage(MESSAGE_VALID_RESOURCE, MESSAGE_VALID_VERSION
        ,TIME_ENTRY_ID, OWNER_ID);

    Assertions.assertThrows(NumberFormatException.class, () -> {
      balanceCalculatorConsumerService.onMessage(message);;
    });
  }

  // TODO: Deserialization error (missing/extra field?) on resource
}