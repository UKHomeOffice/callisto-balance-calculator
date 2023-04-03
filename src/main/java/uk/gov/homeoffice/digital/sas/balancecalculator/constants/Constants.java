package uk.gov.homeoffice.digital.sas.balancecalculator.constants;

public class Constants {

  private Constants() {}

  public static final String KAFKA_SUCCESSFUL_DESERIALIZATION =
      "Successful deserialization of message entity [ %s ] created";

  public static final String KAFKA_UNSUCCESSFUL_DESERIALIZATION =
      "Failed deserialization of message entity [ %s ]";

  public static final String TIME_ENTRY_SCHEMA_NAME =
      "uk.gov.homeoffice.digital.sas.timecard.model.TimeEntry";
}
