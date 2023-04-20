package uk.gov.homeoffice.digital.sas.balancecalculator.client;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.ApiResponse;
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

  public List<Accrual> getAccrualByDate(String tenantId, String personId, LocalDate accrualDate) {
    String url = accrualsUrl + "/resources/accruals?tenantId={tenantId}&filter={filter}";
    Map<String, String> parameters = Map.of(
        "tenantId", tenantId,
        "filter", "accrualDate=='" + accrualDate + "'&&personId=='" + personId + "'"
    );

    ResponseEntity<ApiResponse<Accrual>> entity
        = restTemplate.exchange(url, HttpMethod.GET, null,
            new ParameterizedTypeReference<>() {}, parameters);

    return Objects.requireNonNull(entity.getBody()).getItems();
  }
}
