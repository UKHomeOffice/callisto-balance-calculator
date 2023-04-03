package uk.gov.homeoffice.digital.sas.balancecalculator.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
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

  KafkaConsumerService<TimeEntry> kafkaConsumerService;

  KafkaEventMessage<TimeEntry> kafkaEventMessage;

  public BalanceCalculatorConsumerService(MeterRegistry meterRegistry,
                                          KafkaConsumerService<TimeEntry> kafkaConsumerService) {
    this.kafkaConsumerService = kafkaConsumerService;
    this.meterRegistry = meterRegistry;
  }

  @KafkaListener(
      topics = {"${spring.kafka.template.default-topic}"},
      groupId = "${spring.kafka.consumer.group-id}"
  )
  public void onMessage(@Payload String message) {
    setUpCounters();
    kafkaEventMessage = kafkaConsumerService.consume(message);
    if (!ObjectUtils.isEmpty(kafkaEventMessage)) {
      timeEntry =  new Gson().fromJson(String.valueOf(kafkaEventMessage.getResource()),
          new TypeToken<TimeEntry>() { }.getType());

      log.info(String.format("Successful deserialization of message entity [ %s ] created",
          timeEntry));
    } else {
      errorCounter.increment();
      log.error(String.format("Failed deserialization of message entity [ %s ]",
          message));
    }
  }

  private void setUpCounters() {
    errorCounter = Counter.builder("balance.calculator.messages")    // 2 - create a counter
        .tag("type", "error")
        .description("The number of errors messages when consuming messages")
        .register(meterRegistry);
  }
}
