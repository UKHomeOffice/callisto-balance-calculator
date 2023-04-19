package uk.gov.homeoffice.digital.sas.balancecalculator.accrual;

import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class Contributions {

  @JdbcTypeCode(SqlTypes.JSON)
  private Map<UUID, BigDecimal> timeEntries = Map.of();

  @Min(0)
  private BigDecimal total = BigDecimal.ZERO;
}
