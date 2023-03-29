package uk.gov.homeoffice.digital.sas.balancecalculator.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import uk.gov.homeoffice.digital.sas.kafka.message.KafkaEventMessage;
import uk.gov.homeoffice.digital.sas.kafka.producer.KafkaProducerService;
import uk.gov.homeoffice.digital.sas.timecard.model.TimeEntry;


import java.util.HashMap;
import java.util.Map;

@Configuration
@ComponentScan("uk.gov.homeoffice.digital.sas.balancecalculator")
@EntityScan("uk.gov.homeoffice.digital.sas.balancecalculator.models")
public class TestConfig {

  @Value("${spring.kafka.bootstrap-servers}")
  private String bootstrapAddress;

  @Value("${spring.kafka.template.default-topic}")
  private String topicName;

  @Value("${kafka.valid.schema.version}")
  private String schemaVersion;

  @Bean
  public ProducerFactory<String, KafkaEventMessage<TimeEntry>> producerFactory() {
    Map<String, Object> configProps = new HashMap<>();
    configProps.put(
        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
        bootstrapAddress);
    configProps.put(
        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
        StringSerializer.class);
    configProps.put(
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
        JsonSerializer.class);
    return new DefaultKafkaProducerFactory<>(configProps);
  }

  @Bean KafkaTemplate<String, KafkaEventMessage<TimeEntry>> kafkaTemplate() {
    return new KafkaTemplate<>(producerFactory());
  }

  @Bean
  public KafkaProducerService<TimeEntry> kafkaProducerService () {
   return new KafkaProducerService<TimeEntry>(kafkaTemplate(), topicName, schemaVersion);
  }

}
