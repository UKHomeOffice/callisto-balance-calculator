package uk.gov.homeoffice.digital.sas.balancecalculator.kafka.consumer;

import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.Constants.ACTUATOR_ERROR_TYPE;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.Constants.ACTUATOR_KAFKA_FAILURE_DESCRIPTION;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.Constants.ACTUATOR_KAFKA_FAILURE_URL;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.Constants.KAFKA_SUCCESSFUL_DESERIALIZATION;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.Constants.KAFKA_UNSUCCESSFUL_DESERIALIZATION;
import static uk.gov.homeoffice.digital.sas.kafka.constants.Constants.SCHEMA_JSON_ATTRIBUTE;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.micrometer.core.instrument.Counter;
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
import uk.gov.homeoffice.digital.sas.kafka.exceptions.KafkaConsumerException;
import uk.gov.homeoffice.digital.sas.kafka.message.KafkaEventMessage;

@Service
@Slf4j
@Getter
@ComponentScan({
    "uk.gov.homeoffice.digital.sas.kafka.consumer",
    "uk.gov.homeoffice.digital.sas.kafka.validators",
    "uk.gov.homeoffice.digital.sas.kafka.configuration"})
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
      groupId = "${spring.kafka.consumer.group-id}",
      errorHandler = "kafkaConsumerErrorHandler"
  )
  public void onMessage(@Payload String payload)
      throws JsonProcessingException {

    if (isResourceTimeEntry(payload)) {
      KafkaEventMessage<TimeEntry> kafkaEventMessage =
          kafkaConsumerService.convertToKafkaEventMessage(payload);

      if (!ObjectUtils.isEmpty(kafkaEventMessage)) {
        TimeEntry timeEntry = createTimeEntryFromKafkaEventMessage(kafkaEventMessage, payload);
        isTimeEntryDeserialized(payload, timeEntry);
      }
    } else {
      throw new KafkaConsumerException(String.format(KAFKA_UNSUCCESSFUL_DESERIALIZATION, payload));
    }
  }

  private boolean isTimeEntryDeserialized(String payload, TimeEntry timeEntry) {
    if (ObjectUtils.isEmpty(timeEntry)) {
      errorCounter.increment();
      log.error(String.format(KAFKA_UNSUCCESSFUL_DESERIALIZATION, payload));
      return false;
    } else {
      log.info(String.format(KAFKA_SUCCESSFUL_DESERIALIZATION, timeEntry.getId()));
      return true;
    }
  }

  private TimeEntry createTimeEntryFromKafkaEventMessage(
      KafkaEventMessage<TimeEntry> kafkaEventMessage, String message) {
    try {
      return mapper.convertValue(
          kafkaEventMessage.getResource(), TimeEntry.class);
    } catch (IllegalArgumentException e) {
      throw new KafkaConsumerException(String.format(KAFKA_UNSUCCESSFUL_DESERIALIZATION,
          message), e);
    }
  }

  private boolean isResourceTimeEntry(String message) {
    JsonObject jsonMessage = JsonParser.parseString(message).getAsJsonObject();
    String schema = jsonMessage.get(SCHEMA_JSON_ATTRIBUTE).getAsString();

    return schema.contains(TimeEntry.class.getSimpleName());
  }
}
