package uk.gov.homeoffice.digital.sas.balancecalculator.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.TimeEntry;
import uk.gov.homeoffice.digital.sas.balancecalculator.validators.BalanceCalculatorSchemaValidator;
import uk.gov.homeoffice.digital.sas.kafka.consumer.KafkaConsumerService;

@Service
@Slf4j
public class BalanceCalculatorConsumerService extends KafkaConsumerService<TimeEntry> {

  private ObjectMapper mapper = new ObjectMapper();

  private final MeterRegistry meterRegistry;

  private Counter errorCounter;

  protected BalanceCalculatorConsumerService(BalanceCalculatorSchemaValidator schemaValidator,
                                             MeterRegistry meterRegistry) {
    super(schemaValidator);
    this.meterRegistry = meterRegistry;
  }

  @KafkaListener(
      topics = {"${spring.kafka.template.default-topic}"},
      groupId = "${spring.kafka.consumer.group-id}"
  )
  public void onMessage(@Payload String message) throws JsonProcessingException {
    setUpCounters();
    kafkaEventMessage = consume(message);
    if (!ObjectUtils.isEmpty(kafkaEventMessage)) {
      TimeEntry timeEntry = mapper.convertValue(
          kafkaEventMessage.getResource(), new TypeReference<>() {
          });
      log.info(String.format("Successful deserialization of message entity [ %s ] created",
          timeEntry));
    } else {
      errorCounter.increment();
      log.error("Failed deserialization of message entity [ %s ]", kafkaEventMessage);
    }
  }

  private void setUpCounters() {
    errorCounter = Counter.builder("balance.calculator.messages")    // 2 - create a counter
        .tag("type", "error")
        .description("The number of errors messages when consuming messages")
        .register(meterRegistry);
  }
}
