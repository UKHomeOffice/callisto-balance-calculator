package uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Agreement {

  private UUID id;

  private UUID tenantId;

  @NotNull(message = "Person ID should not be null")
  private UUID personId;

  @Valid
  private ContractualTerms contractualTerms;

  @NotNull(message = "Start date should not be empty")
  private LocalDate startDate;

  @NotNull(message = "End date should not be empty")
  private LocalDate endDate;

}
