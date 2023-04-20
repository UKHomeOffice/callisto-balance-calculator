package uk.gov.homeoffice.digital.sas.balancecalculator.kafka.consumer;

import static uk.gov.homeoffice.digital.sas.kafka.constants.Constants.KAFKA_COULD_NOT_DESERIALIZE_RESOURCE;
import static uk.gov.homeoffice.digital.sas.kafka.constants.Constants.KAFKA_RESOURCE_NOT_UNDERSTOOD;
import static uk.gov.homeoffice.digital.sas.kafka.constants.Constants.KAFKA_SUCCESSFUL_DESERIALIZATION;
import static uk.gov.homeoffice.digital.sas.kafka.consumer.KafkaConsumerUtils.getResourceFromMessageAsString;
import static uk.gov.homeoffice.digital.sas.kafka.consumer.KafkaConsumerUtils.getSchemaFromMessageAsString;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import uk.gov.homeoffice.digital.sas.balancecalculator.configuration.BalanceCalculatorObjectMapperConfig;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.timecard.TimeEntry;
import uk.gov.homeoffice.digital.sas.kafka.consumer.KafkaConsumerService;
import uk.gov.homeoffice.digital.sas.kafka.consumer.configuration.KafkaConsumerConfig;
import uk.gov.homeoffice.digital.sas.kafka.exceptions.KafkaConsumerException;
import uk.gov.homeoffice.digital.sas.kafka.message.KafkaEventMessage;

@Service
@Slf4j
@Import({KafkaConsumerConfig.class, BalanceCalculatorObjectMapperConfig.class})
public class TimeEntryConsumer {

  private final ObjectMapper balanceCalculatorObjectMapper;

  private final KafkaConsumerService<TimeEntry> kafkaConsumerService;


  @Autowired
  public TimeEntryConsumer(KafkaConsumerService<TimeEntry> kafkaConsumerService,
      ObjectMapper balanceCalculatorObjectMapper) {
    this.kafkaConsumerService = kafkaConsumerService;
    this.balanceCalculatorObjectMapper = balanceCalculatorObjectMapper;
  }

  @KafkaListener(topics = {"${spring.kafka.template.default-topic}"},
                groupId = "${spring.kafka.consumer.group-id}",
                errorHandler = "kafkaConsumerErrorHandler")
  public void onMessage(@Payload String payload) throws JsonProcessingException {

    if (kafkaConsumerService.isResourceOfType(payload, TimeEntry.class)) {
      KafkaEventMessage<TimeEntry> kafkaEventMessage =
          kafkaConsumerService.convertToKafkaEventMessage(payload);

      if (!ObjectUtils.isEmpty(kafkaEventMessage)) {
        TimeEntry timeEntry = createTimeEntryFromKafkaEventMessage(kafkaEventMessage, payload);
        log.info(String.format(KAFKA_SUCCESSFUL_DESERIALIZATION, payload));

        //this log message can be deleted when calculate logic is implemented. This is to stop
        // introduction of a code smell by having an Object(timeEntry) not used within the code
        log.info(String.format("TimeEntry Created [ %s ]", timeEntry.toString()));
      }
    } else {
      throw new KafkaConsumerException(
          String.format(KAFKA_RESOURCE_NOT_UNDERSTOOD, getSchemaFromMessageAsString(payload)));
    }
  }

  private TimeEntry createTimeEntryFromKafkaEventMessage(
      KafkaEventMessage<TimeEntry> kafkaEventMessage, String payload) {
    try {
      return balanceCalculatorObjectMapper.convertValue(kafkaEventMessage.getResource(), TimeEntry.class);
    } catch (IllegalArgumentException e) {
      throw new KafkaConsumerException(
          String.format(KAFKA_COULD_NOT_DESERIALIZE_RESOURCE,
              getResourceFromMessageAsString(payload)), e);
    }
  }
}
