package uk.gov.homeoffice.digital.sas.balancecalculator.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.TimeEntry;
import uk.gov.homeoffice.digital.sas.kafka.consumer.KafkaConsumerService;

@Service
@Slf4j
public class Consumer extends KafkaConsumerService<TimeEntry> {

}