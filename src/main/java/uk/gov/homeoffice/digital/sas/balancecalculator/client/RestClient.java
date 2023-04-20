package uk.gov.homeoffice.digital.sas.balancecalculator.client;

import java.time.LocalDate;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Accrual;

@Component
public class RestClient {

  private final RestTemplate restTemplate;

  private final String accrualsUrl;

  @Autowired
  public RestClient(RestTemplateBuilder builder,
      @Value("${balance.calculator.accruals.url}") String accrualsUrl) {
    this.restTemplate = builder.build();
    this.accrualsUrl = accrualsUrl;
  }

  public Accrual getAccrualByDate(String tenantId, String personId, LocalDate accrualDate) {
    String url = accrualsUrl + "/resources/accruals?tenantId={tenantId}&filter={filter}";
    Map<String, String> parameters = Map.of(
        "tenantId", tenantId,
        "filter", "accrualDate=='" + accrualDate + "'&&personId=='" + personId + "'"
    );

    return restTemplate.getForObject(url, Accrual.class, parameters);
  }
}
