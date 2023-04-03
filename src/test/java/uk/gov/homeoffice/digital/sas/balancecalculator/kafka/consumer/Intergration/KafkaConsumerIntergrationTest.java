package uk.gov.homeoffice.digital.sas.balancecalculator.kafka.consumer.Intergration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.TimeEntry;
import uk.gov.homeoffice.digital.sas.balancecalculator.utils.TestUtils;
import uk.gov.homeoffice.digital.sas.kafka.message.KafkaEventMessage;

public class KafkaConsumerIntergrationTest {

  @Autowired
  KafkaTemplate<String, KafkaEventMessage<TimeEntry>> kafkaTemplate;

  KafkaEventMessage<TimeEntry> kafkaEventMessage;

  //@Test
  //void consumerIntergrationTest() {
  //  TimeEntry timeEntry = TestUtils.createTimeEntry("00001", )
  //
  //  var kafkaEventMessage = new KafkaEventMessage<>("0.1.0", TimeEntry, timeEntry, action);
  //
  //
  //  kafkaTemplate.send(
  //      "Callisto-test-topic",
  //      "10001",
  //      new KafkaEventMessage<>();
  //}
}
