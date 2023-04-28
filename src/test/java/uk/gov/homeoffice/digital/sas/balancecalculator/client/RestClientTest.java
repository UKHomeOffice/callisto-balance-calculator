package uk.gov.homeoffice.digital.sas.balancecalculator.client;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.homeoffice.digital.sas.balancecalculator.testutils.CommonUtils.createAccrual;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.ApiResponse;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.PatchBody;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Accrual;

@ExtendWith(MockitoExtension.class)
public class RestClientTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Mock
  RestTemplate restTemplate;

  private RestClient restClient;

  @BeforeEach
  void setup() {
    restClient = new RestClient();
    ReflectionTestUtils.setField(restClient, "restTemplate", restTemplate);
    ReflectionTestUtils.setField(restClient, "accrualsNoFilterUrl", "accruals/");
  }

  @Test
  void patchAccruals_withValidAccruals_shouldRMakePatchRequestWithCorrectBody()
      throws JsonProcessingException {
    String tenantId = "3fa85f64-5717-4562-b3fc-2c963f66afa6";

    Accrual accrual1 = createAccrual(UUID.fromString("0936e7a6-2b2e-1696-2546-5dd25dcae6a0"));
    Accrual accrual2 = createAccrual(UUID.fromString("a613dd93-3bdf-d285-c263-84d6866d61c5"));
    List<Accrual> accrualList = new ArrayList<>();

    accrualList.add(accrual1);
    accrualList.add(accrual2);

    PatchBody body1 = createPatchBody(accrual1);
    PatchBody body2 = createPatchBody(accrual2);

    List<PatchBody> payloadBody = List.of(body1, body2);
    HttpEntity<List<PatchBody>> request = new HttpEntity<>(payloadBody);

    String responseString = "{ \"meta\": { \"next\": null }, \"items\": [] }";
    ApiResponse<Accrual> apiResponse =
        objectMapper.readValue(responseString, new TypeReference<>() {
        });

    when(restTemplate.exchange(any(String.class), eq(HttpMethod.PATCH), eq(request),
        Mockito.<ParameterizedTypeReference<ApiResponse<Accrual>>>any(),
        Mockito.<Map<String, ?>>any()))
        .thenReturn(new ResponseEntity<>(apiResponse, HttpStatus.OK));

    restClient.patchAccruals(tenantId, accrualList);

    verify(restTemplate).exchange(any(String.class), eq(HttpMethod.PATCH), eq(request),
        Mockito.<ParameterizedTypeReference<ApiResponse<Accrual>>>any(),
        Mockito.<Map<String, ?>>any());
  }

  private PatchBody createPatchBody(Accrual accrual) {
    PatchBody body1 = new PatchBody();
    body1.setOp("replace");
    body1.setPath("/" + accrual.getId().toString());
    body1.setValue(accrual);
    return body1;
  }

}