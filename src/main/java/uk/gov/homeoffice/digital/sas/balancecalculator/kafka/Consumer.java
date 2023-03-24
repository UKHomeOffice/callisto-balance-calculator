package uk.gov.homeoffice.digital.sas.balancecalculator.kafka;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.TimeEntry;
import uk.gov.homeoffice.digital.sas.kafka.consumer.KafkaConsumerService;

@Service
@Slf4j
public class Consumer extends KafkaConsumerService<TimeEntry> {

  public Consumer(@Value("${kafka.resource.name}") String resourceName,
                  @Value("${kafka.valid.schema.versions}") List<String> validVersions ) {
    super(resourceName, validVersions);
  }
}