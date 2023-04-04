package uk.gov.homeoffice.digital.sas.balancecalculator.constants;

import java.util.UUID;

public class TestConstants {

  public static final String MESSAGE_VALID_VERSION = "0.1.0";
  public static final String MESSAGE_INVALID_VERSION = "1.1.5";

  public static final String MESSAGE_KEY = "10001:500001";
  public static final String MESSAGE_VALID_RESOURCE = "uk.gov.homeoffice.digital.sas" +
      ".balancecalculator.models.TimeEntry";

  public static final String MESSAGE_INVALID_RESOURCE =
      "uk.gov.homeoffice.digital.sas.model.unknownResource";

  public static final String MESSAGE_EXPECTED_SCHEMA = MESSAGE_VALID_RESOURCE + ", " + MESSAGE_VALID_VERSION;
}
