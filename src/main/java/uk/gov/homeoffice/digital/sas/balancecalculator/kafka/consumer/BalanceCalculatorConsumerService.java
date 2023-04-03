package uk.gov.homeoffice.digital.sas.balancecalculator.kafka.consumer;

import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.Constants.KAFKA_SUCCESSFUL_DESERIALIZATION;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.Constants.KAFKA_UNSUCCESSFUL_DESERIALIZATION;
import static uk.gov.homeoffice.digital.sas.balancecalculator.constants.Constants.TIME_ENTRY_SCHEMA_NAME;
import static uk.gov.homeoffice.digital.sas.kafka.constants.Constants.SCHEMA_JSON_ATTRIBUTE;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
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

  private final MeterRegistry meterRegistry;

  private Counter errorCounter;

  private TimeEntry timeEntry;
  private final String kafkaSupportedResourceName;

  KafkaConsumerService<TimeEntry> kafkaConsumerService;

  KafkaEventMessage<TimeEntry> kafkaEventMessage;

  public BalanceCalculatorConsumerService(MeterRegistry meterRegistry,
                                          KafkaConsumerService<TimeEntry> kafkaConsumerService,
                                          @Value("${kafka.supported.resource.name}") String kafkaSupportedResourceName) {
    this.kafkaSupportedResourceName = kafkaSupportedResourceName;
    this.kafkaConsumerService = kafkaConsumerService;
    this.meterRegistry = meterRegistry;
  }

  @KafkaListener(
      topics = {"${spring.kafka.template.default-topic}"},
      groupId = "${spring.kafka.consumer.group-id}"
  )
  public void onMessage(@Payload String message) {
    setUpCounters();
    if (isResourceTimeEntry(message)) {
      kafkaEventMessage = kafkaConsumerService.consume(message);
    }

    createTimeEntryFromKafkaEventMessage(message);
  }

  private void createTimeEntryFromKafkaEventMessage(String message) {
    if (!ObjectUtils.isEmpty(kafkaEventMessage)) {
      timeEntry = new Gson().fromJson(String.valueOf(kafkaEventMessage.getResource()),
          new TypeToken<TimeEntry>() { }.getType());

      log.info(String.format(KAFKA_SUCCESSFUL_DESERIALIZATION,
          timeEntry));
    } else {
      errorCounter.increment();
      log.error(String.format(KAFKA_UNSUCCESSFUL_DESERIALIZATION,
          message));

    }
  }

  private boolean isResourceTimeEntry(String message) {
    JsonObject jsonMessage = JsonParser.parseString(message).getAsJsonObject();
    String schema = jsonMessage.get(SCHEMA_JSON_ATTRIBUTE).getAsString();

    return schema.contains(kafkaSupportedResourceName);
  }

  private void setUpCounters() {
    errorCounter = Counter.builder("balance.calculator.messages")
        .tag("type", "error")
        .description("The number of errors messages when consuming messages")
        .register(meterRegistry);
  }
}
