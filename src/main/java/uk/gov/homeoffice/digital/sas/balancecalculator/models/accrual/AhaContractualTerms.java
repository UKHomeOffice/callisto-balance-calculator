package uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual;

import static uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.constants.AgreementTypes.AHA_AGREEMENT_TYPE;

import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.enums.SalaryBasis;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.enums.TermsAndConditions;

@JsonTypeName(AHA_AGREEMENT_TYPE)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class AhaContractualTerms implements ContractualTerms {

  @DecimalMin(value = "0.0", inclusive = false)
  @Digits(integer = 1, fraction = 4)
  @DecimalMax(value = "1.0")
  private BigDecimal fteValue;

  @NotNull(message = "Terms and conditions should not be empty")
  @Enumerated(EnumType.STRING)
  private TermsAndConditions termsAndConditions;

  @NotNull(message = "Salary basis should not be empty")
  @Enumerated(EnumType.STRING)
  private SalaryBasis salaryBasis;
}
