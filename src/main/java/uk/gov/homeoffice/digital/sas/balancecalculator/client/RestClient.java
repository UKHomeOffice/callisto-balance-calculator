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
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Agreement;

@Component
public class RestClient {

  private final RestTemplate restTemplate;

  private final String accrualsUrl;

  public static final String TENANT_ID_STRING_IDENTIFIER = "tenantId";

  public static final String FILTER_STRING_IDENTIFIER = "filter";


  @Autowired
  public RestClient(RestTemplateBuilder builder,
      @Value("${balance.calculator.accruals.url}") String accrualsUrl) {
    this.restTemplate = builder.build();
    this.accrualsUrl = accrualsUrl;
  }

  // TODO make some of this reusable

  public List<Accrual> getAccrualByDate(String tenantId, String personId, LocalDate accrualDate) {
    String url = accrualsUrl + "/resources/accruals?tenantId={tenantId}&filter={filter}";
    Map<String, String> parameters = Map.of(
        TENANT_ID_STRING_IDENTIFIER, tenantId,
        FILTER_STRING_IDENTIFIER, "accrualDate=='" + accrualDate + "'&&personId=='" + personId + "'"
    );

    ResponseEntity<ApiResponse<Accrual>> entity
        = restTemplate.exchange(url, HttpMethod.GET, null,
            new ParameterizedTypeReference<>() {}, parameters);

    return Objects.requireNonNull(entity.getBody()).getItems();
  }

  public Agreement getAgreementById(String tenantId, String agreementId) {
    String url = accrualsUrl + "/resources/agreements?tenantId={tenantId}&filter={filter}";
    Map<String, String> parameters = Map.of(TENANT_ID_STRING_IDENTIFIER, tenantId,
        FILTER_STRING_IDENTIFIER, "id=='" + agreementId + "'");

    ResponseEntity<ApiResponse<Agreement>> entity
        = restTemplate.exchange(url, HttpMethod.GET, null,
        new ParameterizedTypeReference<>() {}, parameters);

    if (Objects.requireNonNull(entity.getBody()).getItems().size() == 1) {
      return Objects.requireNonNull(entity.getBody()).getItems().get(0);
    }
    // else throw exception

    return null;
  }

  public List<Accrual> getAllAccrualsAfterDate(LocalDate agreementEndDate,
                                               LocalDate timeEntryDate, String tenantId,
                                               String personId) {
    String url = accrualsUrl + "/resources/accruals?tenantId={tenantId}&filter={filter}";

    Map<String, String> parameters = Map.of(TENANT_ID_STRING_IDENTIFIER, tenantId,
        FILTER_STRING_IDENTIFIER,
        "personId=='" + personId + "'" +
            "&&accrualDate<='" + agreementEndDate + "'" +
            "&&accrualDate>='" + timeEntryDate + "'");

    ResponseEntity<ApiResponse<Accrual>> entity
        = restTemplate.exchange(url, HttpMethod.GET, null,
        new ParameterizedTypeReference<>() {}, parameters);

    return Objects.requireNonNull(entity.getBody()).getItems();
  }
}
