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

  public static final String TENANT_ID_STRING_IDENTIFIER = "tenantId";
  public static final String FILTER_STRING_IDENTIFIER = "filter";

  private final RestTemplate restTemplate;
  private final String accrualsFilterUrl;
  private final String agreementsByIdUrl;


  @Autowired
  public RestClient(RestTemplateBuilder builder,
      @Value("${balance.calculator.accruals.url}") String accrualsUrl) {
    this.restTemplate = builder.build();
    this.accrualsFilterUrl =
        accrualsUrl + "/resources/accruals?tenantId={tenantId}&filter={filter}";
    this.agreementsByIdUrl = accrualsUrl + "/resources/agreements/{agreementId}?tenantId={tenantId}";
  }

  // TODO make some of this reusable

  public List<Accrual> getAccrualsByDate(String tenantId, String personId, LocalDate accrualDate) {
    Map<String, String> parameters = Map.of(
        TENANT_ID_STRING_IDENTIFIER, tenantId,
        FILTER_STRING_IDENTIFIER, "accrualDate=='" + accrualDate + "'&&personId=='" + personId + "'"
    );

    ResponseEntity<ApiResponse<Accrual>> entity
        = restTemplate.exchange(accrualsFilterUrl, HttpMethod.GET, null,
        new ParameterizedTypeReference<>() {}, parameters);

    return Objects.requireNonNull(entity.getBody()).getItems();
  }

  public Agreement getAgreementById(String tenantId, String agreementId) {
    Map<String, String> parameters = Map.of(TENANT_ID_STRING_IDENTIFIER, tenantId,
        "agreementId", agreementId);

    ResponseEntity<ApiResponse<Agreement>> entity
        = restTemplate.exchange(agreementsByIdUrl, HttpMethod.GET, null,
        new ParameterizedTypeReference<>() {}, parameters);

    if (Objects.requireNonNull(entity.getBody()).getItems().size() == 1) {
      return Objects.requireNonNull(entity.getBody()).getItems().get(0);
    }
    // else throw exception

    return null;
  }

  public List<Accrual> getAccrualsBetweenDates(String tenantId, String personId,
                                               LocalDate startDate, LocalDate endDate) {
    Map<String, String> parameters = Map.of(TENANT_ID_STRING_IDENTIFIER, tenantId,
        FILTER_STRING_IDENTIFIER,
        "personId=='" + personId + "'" +
            "&&accrualDate<='" + endDate + "'" +
            "&&accrualDate>='" + startDate + "'");

    ResponseEntity<ApiResponse<Accrual>> entity
        = restTemplate.exchange(accrualsFilterUrl, HttpMethod.GET, null,
        new ParameterizedTypeReference<>() {}, parameters);

    return Objects.requireNonNull(entity.getBody()).getItems();
  }

  public Accrual getPriorAccrual(String tenantId, String personId, String accrualTypeId,
      LocalDate referenceDate) {
    LocalDate priorAccrualDate = referenceDate.minusDays(1);
    Map<String, String> parameters = Map.of(
        TENANT_ID_STRING_IDENTIFIER, tenantId,
        FILTER_STRING_IDENTIFIER,
        "personId=='" + personId + "'"
            + "&&accrualTypeId=='" + accrualTypeId + "'"
            + "&&accrualDate=='" + priorAccrualDate + "'"
    );

    ResponseEntity<ApiResponse<Accrual>> entity
        = restTemplate.exchange(accrualsFilterUrl, HttpMethod.GET, null,
        new ParameterizedTypeReference<>() {}, parameters);

    if (Objects.requireNonNull(entity.getBody()).getItems().size() == 1) {
      return Objects.requireNonNull(entity.getBody()).getItems().get(0);
    }
    // else throw exception

    return null;
  }
}
