package uk.gov.homeoffice.digital.sas.balancecalculator.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
// TODO this can be renamed ObjectMapperConfig
public class BalanceCalculatorObjectMapperConfig {

  @Bean
  public ObjectMapper balanceCalculatorObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }
}