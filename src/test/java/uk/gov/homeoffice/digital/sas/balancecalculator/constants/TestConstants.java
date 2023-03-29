package uk.gov.homeoffice.digital.sas.balancecalculator.constants;

public class TestConstants {

  public static final String MESSAGE_VALID_VERSION = "0.1.0";
  public static final String MESSAGE_INVALID_VERSION = "0.1.5";

  public static final String MESSAGE_VALID_RESOURCE = "uk.gov.homeoffice.digital.sas.timecard.model.TimeEntry";
  public static final String MESSAGE_INVALID_RESOURCE = "uk.gov.homeoffice.digital.sas.timecard.model.UnknownResource";


  public static final String MESSAGE_EXPECTED_SCHEMA = MESSAGE_VALID_RESOURCE + ", " + MESSAGE_VALID_VERSION;

  public static final String KAFKA_JSON_MESSAGE =
      "{\"schema\":\"%s, %s\",\""
          + "resource\":{\"id\":\"c0a80018-870e-11b0-8187-0ea38cb30001\","
          + "\"tenantId\":\"00000000-0000-0000-0000-000000000000\","
          + "\"ownerId\":\"3343a960-de03-42ba-8769-767404fb2fcf\","
          + "\"timePeriodTypeId\":\"00000000-0000-0000-0000-000000000001\",\"shiftType\":null,"
          + "\"actualStartTime\":1679456400000,\"actualEndTime\":1679457000000},"
          + "\"action\":\"CREATE\"}";
}
