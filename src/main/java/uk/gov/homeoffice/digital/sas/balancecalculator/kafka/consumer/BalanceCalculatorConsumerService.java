package uk.gov.homeoffice.digital.sas.balancecalculator.kafka.consumer;

import static uk.gov.homeoffice.digital.sas.kafka.constants.Constants.KAFKA_COULD_NOT_DESERIALIZE_RESOURCE;
import static uk.gov.homeoffice.digital.sas.kafka.constants.Constants.KAFKA_RESOURCE_NOT_UNDERSTOOD;
import static uk.gov.homeoffice.digital.sas.kafka.consumer.KafkaConsumerUtils.getResourceFromMessageAsString;
import static uk.gov.homeoffice.digital.sas.kafka.consumer.KafkaConsumerUtils.getSchemaFromMessageAsString;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonParser;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.TimeEntry;
import uk.gov.homeoffice.digital.sas.kafka.consumer.KafkaConsumerService;
import uk.gov.homeoffice.digital.sas.kafka.consumer.KafkaConsumerUtils;
import uk.gov.homeoffice.digital.sas.kafka.exceptions.KafkaConsumerException;
import uk.gov.homeoffice.digital.sas.kafka.message.KafkaEventMessage;

@Service
@Slf4j
@Getter
@Configuration("KafkaConsumerConfig")
public class BalanceCalculatorConsumerService {

  private ObjectMapper mapper = new ObjectMapper();

  private final KafkaConsumerService<TimeEntry> kafkaConsumerService;

  private final KafkaConsumerUtils<TimeEntry> kafkaConsumerUtils;

  public BalanceCalculatorConsumerService(KafkaConsumerService<TimeEntry> kafkaConsumerService,
                                          KafkaConsumerUtils<TimeEntry> kafkaConsumerUtils) {
    this.kafkaConsumerService = kafkaConsumerService;
    this.kafkaConsumerUtils = kafkaConsumerUtils;
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
        kafkaConsumerUtils.checkDeserializedResource(payload, timeEntry);
      }
    } else {
      throw new KafkaConsumerException(String.format(KAFKA_RESOURCE_NOT_UNDERSTOOD,
          getSchemaFromMessageAsString(payload)));
    }
  }

  private TimeEntry createTimeEntryFromKafkaEventMessage(
      KafkaEventMessage<TimeEntry> kafkaEventMessage, String message) {
    try {
      return mapper.convertValue(
          kafkaEventMessage.getResource(), TimeEntry.class);
    } catch (IllegalArgumentException e) {
      throw new KafkaConsumerException(String.format(KAFKA_COULD_NOT_DESERIALIZE_RESOURCE,
          getResourceFromMessageAsString(message)), e);
    }
  }

  private boolean isResourceTimeEntry(String message) {
    JsonParser.parseString(message).getAsJsonObject();
    String schema = getSchemaFromMessageAsString(message);

    return schema.contains(TimeEntry.class.getSimpleName());
  }
}
