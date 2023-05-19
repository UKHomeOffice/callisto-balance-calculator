package uk.gov.homeoffice.digital.sas.balancecalculator.client;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.ApiResponse;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.PatchBody;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Accrual;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Agreement;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.enums.AccrualType;

@NoArgsConstructor
@Component
public class RestClient {

  public static final String TENANT_ID_STRING_IDENTIFIER = "tenantId";
  public static final String FILTER_STRING_IDENTIFIER = "filter";

  private RestTemplate restTemplate;
  private String accrualsFilterUrl;
  private String agreementsByIdUrl;
  private String accrualsNoFilterUrl;


  @Autowired
  public RestClient(RestTemplateBuilder builder,
                    @Value("${balance.calculator.accruals.url}") String accrualsUrl) {
    this.restTemplate = builder.build();
    this.accrualsNoFilterUrl = accrualsUrl + "/resources/accruals?tenantId={tenantId}";
    this.accrualsFilterUrl =
        accrualsUrl + "/resources/accruals?tenantId={tenantId}&filter={filter}";
    this.agreementsByIdUrl =
        accrualsUrl + "/resources/agreements/{agreementId}?tenantId={tenantId}";
  }

  public Accrual getAccrualByTypeAndDate(String tenantId, String personId, String accrualTypeId,
                                         LocalDate accrualDate) {
    Map<String, String> parameters = Map.of(
        TENANT_ID_STRING_IDENTIFIER, tenantId,
        FILTER_STRING_IDENTIFIER,
        "personId=='" + personId + "'"
            + "&&accrualTypeId=='" + accrualTypeId + "'"
            + "&&accrualDate=='" + accrualDate + "'"
    );

    ResponseEntity<ApiResponse<Accrual>> entity
        = restTemplate.exchange(accrualsFilterUrl, HttpMethod.GET, null,
          new ParameterizedTypeReference<>() {
          }, parameters);

    if (Objects.requireNonNull(entity.getBody()).getItems().size() == 1) {
      return Objects.requireNonNull(entity.getBody()).getItems().get(0);
    }
    return null;
  }

  public Agreement getAgreementById(String tenantId, String agreementId) {
    Map<String, String> parameters = Map.of(TENANT_ID_STRING_IDENTIFIER, tenantId,
        "agreementId", agreementId);

    ResponseEntity<ApiResponse<Agreement>> entity
        = restTemplate.exchange(agreementsByIdUrl, HttpMethod.GET, null,
          new ParameterizedTypeReference<>() {
          }, parameters);

    if (Objects.requireNonNull(entity.getBody()).getItems().size() == 1) {
      return Objects.requireNonNull(entity.getBody()).getItems().get(0);
    }
    return null;
  }

  public List<Accrual> getAccrualsBetweenDates(String tenantId, String personId,
                                               LocalDate startDate, LocalDate endDate) {
    Map<String, String> parameters = Map.of(TENANT_ID_STRING_IDENTIFIER, tenantId,
        FILTER_STRING_IDENTIFIER,
        "personId=='" + personId + "'"
            + "&&accrualDate<='" + endDate + "'"
            + "&&accrualDate>='" + startDate + "'");

    ResponseEntity<ApiResponse<Accrual>> entity
        = restTemplate.exchange(accrualsFilterUrl, HttpMethod.GET, null,
          new ParameterizedTypeReference<>() {
          }, parameters);

    return Objects.requireNonNull(entity.getBody()).getItems();
  }

  public Agreement getApplicableAgreement(String tenantId, String personId, LocalDate accrualDate) {

    // Using Annual Target Hours accrual type, but any other accrual type would do
    Accrual accrual = getAccrualByTypeAndDate(tenantId, personId,
        AccrualType.ANNUAL_TARGET_HOURS.getId().toString(), accrualDate);

    if (accrual != null) {
      String agreementId = accrual.getAgreementId().toString();

      return getAgreementById(tenantId, agreementId);
    }
    return null;
  }

  public List<Accrual> patchAccruals(String tenantId, List<Accrual> accruals) {
    Map<String, String> parameters = Map.of(TENANT_ID_STRING_IDENTIFIER, tenantId);

    List<PatchBody> payloadBody = createPatchBody(accruals);
    HttpEntity<List<PatchBody>> request = new HttpEntity<>(payloadBody);

    ResponseEntity<ApiResponse<Accrual>> entity = restTemplate.exchange(accrualsNoFilterUrl,
        HttpMethod.PATCH, request, new ParameterizedTypeReference<>() {
        }, parameters);

    return Objects.requireNonNull(entity.getBody()).getItems();
  }

  List<PatchBody> createPatchBody(List<Accrual> accruals) {
    return accruals.stream()
        .map(a -> PatchBody.builder()
              .op("replace")
              .path("/" + a.getId().toString())
              .value(a)
              .build()
        )
        .toList();
  }
}
