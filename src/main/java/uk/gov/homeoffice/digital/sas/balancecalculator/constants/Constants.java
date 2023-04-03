package uk.gov.homeoffice.digital.sas.balancecalculator.constants;

public class Constants {

  private Constants() {
  }

  public static final String KAFKA_SUCCESSFUL_DESERIALIZATION =
      "Successful deserialization of message entity [ %s ] created";

  public static final String KAFKA_UNSUCCESSFUL_DESERIALIZATION =
      "Failed deserialization of message entity [ %s ]";

  public static final String ACTUATOR_KAFKA_FAILURE_URL = "balance.calculator.messages";

  public static final String ACTUATOR_ERROR_TYPE = "error";

  public static final String ACTUATOR_KAFKA_FAILURE_DESCRIPTION = "The total deserialization "
      + "errors for balance calculator";


}
