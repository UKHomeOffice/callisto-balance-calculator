package uk.gov.homeoffice.digital.sas.balancecalculator;

import static java.time.temporal.ChronoUnit.MINUTES;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import uk.gov.homeoffice.digital.sas.balancecalculator.accrual.Accrual;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.TimeEntry;

@Component
public class BalanceCalculator {

  private final RestTemplate restTemplate;

  private final String accrualsUrl;

  @Autowired
  public BalanceCalculator(RestTemplateBuilder builder,
      @Value("${balance.calculator.accruals.url}") String accrualsUrl) {
    this.restTemplate = builder.build();
    this.accrualsUrl = accrualsUrl;
  }

  public void calculate(TimeEntry timeEntry) {

//    Long minutes = MINUTES.between(timeEntry.getActualStartTime(), timeEntry.getActualEndTime());
//    BigDecimal hours = new BigDecimal(minutes / 60);

    String tenantId = timeEntry.getTenantId();

    String url = accrualsUrl + "/resources/accruals?tenantId={tenantId}&filter={filter}";
    Map<String, String> parameters = Map.of(
        "tenantId", tenantId,
        "filter", "'accrualDate==" + timeEntry.getActualStartTime().toLocalDate()+"'"
    );

    Accrual accrual =
        restTemplate.getForObject(url, Accrual.class, parameters);
  }

}
