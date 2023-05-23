package uk.gov.homeoffice.digital.sas.balancecalculator.constants;

public class Constants {

  private Constants() {}

  public static final String NO_ACCRUALS_FOUND_FOR_TYPE =
      "No {0} Accrual records found for agreement between {1} and {2}";

  public static final String ACCRUALS_MAP_EMPTY = "Accruals Map must contain at least one entry!";

  public static final String MISSING_ACCRUAL =
      "Accrual missing for tenantId {0}, personId {1}, accrual type {2} and date {3}";
}
