package uk.gov.homeoffice.digital.sas.balancecalculator.configuration;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan({
    "uk.gov.homeoffice.digital.sas.kafka.consumer",
    "uk.gov.homeoffice.digital.sas.kafka.validators",
    "uk.gov.homeoffice.digital.sas.kafka.configuration",
    "uk.gov.homeoffice.digital.sas.kafka.actuator"})
public class KafkaConsumerConfig {
}
