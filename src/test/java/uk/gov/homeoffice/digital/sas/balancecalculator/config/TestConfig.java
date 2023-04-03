package uk.gov.homeoffice.digital.sas.balancecalculator.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan({"uk.gov.homeoffice.digital.sas.balancecalculator",
    "uk.gov.homeoffice.digital.sas.kafka.validators"})
@EntityScan("uk.gov.homeoffice.digital.sas.balancecalculator.models")
public class TestConfig {
}
