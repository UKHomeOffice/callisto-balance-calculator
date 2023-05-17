package uk.gov.homeoffice.digital.sas.balancecalculator.kafka.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import uk.gov.homeoffice.digital.sas.balancecalculator.BalanceCalculator;
import uk.gov.homeoffice.digital.sas.balancecalculator.configuration.ObjectMapperConfig;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Accrual;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.timecard.TimeEntry;
import uk.gov.homeoffice.digital.sas.kafka.consumer.KafkaConsumerService;
import uk.gov.homeoffice.digital.sas.kafka.consumer.configuration.KafkaConsumerConfig;
import uk.gov.homeoffice.digital.sas.kafka.exceptions.KafkaConsumerException;
import uk.gov.homeoffice.digital.sas.kafka.message.KafkaEventMessage;

import java.util.List;

import static uk.gov.homeoffice.digital.sas.kafka.constants.Constants.KAFKA_COULD_NOT_DESERIALIZE_RESOURCE;
import static uk.gov.homeoffice.digital.sas.kafka.constants.Constants.KAFKA_RESOURCE_NOT_UNDERSTOOD;
import static uk.gov.homeoffice.digital.sas.kafka.constants.Constants.KAFKA_SUCCESSFUL_DESERIALIZATION;
import static uk.gov.homeoffice.digital.sas.kafka.consumer.KafkaConsumerUtils.getResourceFromMessageAsString;
import static uk.gov.homeoffice.digital.sas.kafka.consumer.KafkaConsumerUtils.getSchemaFromMessageAsString;

@Service
@Slf4j
@Import({KafkaConsumerConfig.class, ObjectMapperConfig.class})
public class TimeEntryConsumer {

  private final ObjectMapper objectMapper;

  private final KafkaConsumerService<TimeEntry> kafkaConsumerService;

  private final BalanceCalculator balanceCalculator;

  @Autowired
  public TimeEntryConsumer(KafkaConsumerService<TimeEntry> kafkaConsumerService,
                           ObjectMapper objectMapper, BalanceCalculator balanceCalculator) {
    this.kafkaConsumerService = kafkaConsumerService;
    this.objectMapper = objectMapper;
    this.balanceCalculator = balanceCalculator;
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

        List<Accrual> accrualsToBatchUpdate = balanceCalculator.calculate(timeEntry, kafkaEventMessage.getAction());
        balanceCalculator.sendToAccruals(timeEntry.getTenantId(), accrualsToBatchUpdate);
      }

      // TODO What should we do with Kafka offset when errors are thrown during balance calculation?
    } else {
      throw new KafkaConsumerException(
          String.format(KAFKA_RESOURCE_NOT_UNDERSTOOD, getSchemaFromMessageAsString(payload)));
    }
  }

  private TimeEntry createTimeEntryFromKafkaEventMessage(
      KafkaEventMessage<TimeEntry> kafkaEventMessage, String payload) {
    try {
      return objectMapper.convertValue(kafkaEventMessage.getResource(), TimeEntry.class);
    } catch (IllegalArgumentException e) {
      throw new KafkaConsumerException(
          String.format(KAFKA_COULD_NOT_DESERIALIZE_RESOURCE,
              getResourceFromMessageAsString(payload)), e);
    }
  }
}
