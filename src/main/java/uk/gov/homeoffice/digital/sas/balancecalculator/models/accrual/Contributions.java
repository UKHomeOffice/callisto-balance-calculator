package uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual;

import java.math.BigDecimal;
import java.util.Map;
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
public class Contributions {

  private Map<UUID, BigDecimal> timeEntries = Map.of();

  private BigDecimal total = BigDecimal.ZERO;
}
