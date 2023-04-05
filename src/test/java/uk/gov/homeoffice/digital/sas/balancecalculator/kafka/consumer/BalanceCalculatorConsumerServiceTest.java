package uk.gov.homeoffice.digital.sas.balancecalculator.kafka.consumer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.annotation.DirtiesContext;

import uk.gov.homeoffice.digital.sas.balancecalculator.models.TimeEntry;
import uk.gov.homeoffice.digital.sas.balancecalculator.utils.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.Constants.KAFKA_SUCCESSFUL_DESERIALIZATION;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.Constants.KAFKA_UNSUCCESSFUL_DESERIALIZATION;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.MESSAGE_INVALID_RESOURCE;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.MESSAGE_INVALID_VERSION;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.MESSAGE_VALID_RESOURCE;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.TestConstants.MESSAGE_VALID_VERSION;
import static uk.gov.homeoffice.digital.sas.balancecalculator.utils.TestUtils.createResourceJson;
import static uk.gov.homeoffice.digital.sas.balancecalculator.utils.Utils.createTimeJsonDeserializer;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.UUID;

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
    String id = UUID.randomUUID().toString();
    String ownerId = UUID.randomUUID().toString();

    Gson gson = createTimeJsonDeserializer();

    String timeEntryJson = createResourceJson(id, ownerId);

    TimeEntry expectedTimeEntry = gson.fromJson(String.valueOf(timeEntryJson),
        new TypeToken<TimeEntry>() {}.getType());


    String message = TestUtils.createKafkaMessage(MESSAGE_VALID_RESOURCE, MESSAGE_VALID_VERSION,
        id, ownerId);

    //when
    balanceCalculatorConsumerService.onMessage(message);

    // then
    assertThat(capturedOutput.getOut()).contains(String.format(KAFKA_SUCCESSFUL_DESERIALIZATION,
        expectedTimeEntry.getId()));
  }

  @Test
  void onMessage_notDeserializeKafkaMessageAndThrowException_when_inValidResourceIsReceived() {
    //given
    String id = UUID.randomUUID().toString();
    String ownerId = UUID.randomUUID().toString();

    String message = TestUtils.createKafkaMessage(MESSAGE_INVALID_RESOURCE, MESSAGE_VALID_VERSION
        , id, ownerId);

    Assertions.assertThrows(ClassNotFoundException.class, () -> {
      balanceCalculatorConsumerService.onMessage(message);;
    });
  }
}