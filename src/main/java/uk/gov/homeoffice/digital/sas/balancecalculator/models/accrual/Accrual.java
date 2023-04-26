package uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class Accrual {

  private UUID id;

  private UUID tenantId;

  private UUID personId;

  @NotNull(message = "Agreement ID should not be null")
  private UUID agreementId;

  @NotNull(message = "Accrual date should not be null")
  private LocalDate accrualDate;

  @NotNull(message = "Accrual type ID should not be null")
  private UUID accrualTypeId;

  @NotNull(message = "Balance should not be null")
  @Min(0)
  private BigDecimal cumulativeTotal;

  @NotNull(message = "Target should not be null")
  @Min(0)
  private BigDecimal cumulativeTarget;

  private Contributions contributions = new Contributions();

}
