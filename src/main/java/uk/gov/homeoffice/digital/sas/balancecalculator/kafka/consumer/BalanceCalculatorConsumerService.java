package uk.gov.homeoffice.digital.sas.balancecalculator.kafka.consumer;

import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.Constants.ACTUATOR_ERROR_TYPE;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.Constants.ACTUATOR_KAFKA_FAILURE_DESCRIPTION;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.Constants.ACTUATOR_KAFKA_FAILURE_URL;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.Constants.KAFKA_SUCCESSFUL_DESERIALIZATION;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.Constants.KAFKA_UNSUCCESSFUL_DESERIALIZATION;
import static uk.gov.homeoffice.digital.sas.balancecalculator.utils.Utils.createTimeJsonDeserializer;
import static uk.gov.homeoffice.digital.sas.kafka.constants.Constants.SCHEMA_JSON_ATTRIBUTE;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import io.micrometer.core.instrument.Counter;
import java.sql.Time;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import uk.gov.homeoffice.digital.sas.balancecalculator.actuator.ActuatorCounters;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.TimeEntry;
import uk.gov.homeoffice.digital.sas.kafka.consumer.KafkaConsumerService;
import uk.gov.homeoffice.digital.sas.kafka.message.KafkaEventMessage;

@Service
@Slf4j
@Getter
@ComponentScan({
    "uk.gov.homeoffice.digital.sas.kafka.consumer",
    "uk.gov.homeoffice.digital.sas.kafka.validators"})
public class BalanceCalculatorConsumerService {

  private ObjectMapper mapper = new ObjectMapper();

  private Counter errorCounter;

  private final KafkaConsumerService<TimeEntry> kafkaConsumerService;

  private final ActuatorCounters counters;

  public BalanceCalculatorConsumerService(KafkaConsumerService<TimeEntry> kafkaConsumerService,
                                          ActuatorCounters counters) {
    this.kafkaConsumerService = kafkaConsumerService;
    this.counters = counters;
    errorCounter = counters.setUpCounters(ACTUATOR_KAFKA_FAILURE_URL, ACTUATOR_ERROR_TYPE,
        ACTUATOR_KAFKA_FAILURE_DESCRIPTION);
  }

  @KafkaListener(
      topics = {"${spring.kafka.template.default-topic}"},
      groupId = "${spring.kafka.consumer.group-id}"
  )
  public void onMessage(@Payload String message) throws ClassNotFoundException {

    if (isResourceTimeEntry(message)) {
      KafkaEventMessage<TimeEntry> kafkaEventMessage =
          kafkaConsumerService.convertToKafkaEventMessage(message);
      if (!ObjectUtils.isEmpty(kafkaEventMessage)) {
        TimeEntry timeEntry = createTimeEntryFromKafkaEventMessage(kafkaEventMessage);
        isTimeEntryDeserialized(message, timeEntry);
      }
    } else {
      throw new ClassNotFoundException(String.format(KAFKA_UNSUCCESSFUL_DESERIALIZATION, message));
    }
  }

  private boolean isTimeEntryDeserialized(String message, TimeEntry timeEntry) {
    if (ObjectUtils.isEmpty(timeEntry)) {
      errorCounter.increment();
      log.error(String.format(KAFKA_UNSUCCESSFUL_DESERIALIZATION, message));
      return false;
    } else {
      log.info(String.format(KAFKA_SUCCESSFUL_DESERIALIZATION, timeEntry.getId()));
      return true;
    }
  }

  protected TimeEntry createTimeEntryFromKafkaEventMessage(
      KafkaEventMessage<TimeEntry> kafkaEventMessage) {

    Gson gson = createTimeJsonDeserializer();
    return gson.fromJson(
        String.valueOf(kafkaEventMessage.getResource()), new TypeToken<TimeEntry>() {}.getType());
  }

  private boolean isResourceTimeEntry(String message) {
    JsonObject jsonMessage = JsonParser.parseString(message).getAsJsonObject();
    String schema = jsonMessage.get(SCHEMA_JSON_ATTRIBUTE).getAsString();

    return schema.contains(TimeEntry.class.getSimpleName());
  }
}
