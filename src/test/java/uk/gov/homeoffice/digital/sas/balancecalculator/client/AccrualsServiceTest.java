package uk.gov.homeoffice.digital.sas.balancecalculator.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.homeoffice.digital.sas.balancecalculator.testutils.CommonUtils.createAccrual;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Agreement;

@ExtendWith(MockitoExtension.class)
class AccrualsServiceTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Captor
  ArgumentCaptor<HttpEntity<List<PatchBody>>> patchBodyListCaptor;

  @Mock
  ApiResponse<Accrual> emptyAccrualResponse;

  @Mock
  ApiResponse<Agreement> emptyAgreementResponse;

  @Mock
  RestTemplate restTemplate;

  private AccrualsService accrualsService;

  @BeforeEach
  void setup() {
    accrualsService = new AccrualsService();
    ReflectionTestUtils.setField(accrualsService, "restTemplate", restTemplate);
    ReflectionTestUtils.setField(accrualsService, "accrualsNoFilterUrl", "accruals/");
    ReflectionTestUtils.setField(accrualsService, "accrualsFilterUrl", "accruals/");
    ReflectionTestUtils.setField(accrualsService, "agreementsByIdUrl", "agreements/");
  }

  @Test
  void getApplicableAgreement_accrualNotFound_shouldReturnNullAgreement() {

    String tenantId = "3fa85f64-5717-4562-b3fc-2c963f66afa6";
    String personId = "0936e7a6-2b2e-1696-2546-5dd25dcae6a0";
    LocalDate accrualDate = LocalDate.of(2023,5,15);

    when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET),
        Mockito.<HttpEntity<Accrual>>any() ,
        Mockito.<ParameterizedTypeReference<ApiResponse<Accrual>>>any(),
        Mockito.<Map<String, ?>>any()))
        .thenReturn(new ResponseEntity<>(emptyAccrualResponse, HttpStatus.OK));

    when(emptyAccrualResponse.getItems()).thenReturn(List.of());

    Agreement applicableAgreement =
        accrualsService.getApplicableAgreement(tenantId, personId, accrualDate);

    assertThat(applicableAgreement).isNull();
  }

  @Test
  void getAgreementById_agreementNotFound_shouldReturnNullAgreement() {

    String tenantId = "3fa85f64-5717-4562-b3fc-2c963f66afa6";
    String agreementId = "c0a80193-87a3-1ff0-8187-a3bfe2b80004";

    when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET),
        Mockito.<HttpEntity<Agreement>>any() ,
        Mockito.<ParameterizedTypeReference<ApiResponse<Agreement>>>any(),
        Mockito.<Map<String, ?>>any()))
        .thenReturn(new ResponseEntity<>(emptyAgreementResponse, HttpStatus.OK));

    when(emptyAgreementResponse.getItems()).thenReturn(List.of());

    Agreement agreement = accrualsService.getAgreementById(tenantId, agreementId);

    assertThat(agreement).isNull();
  }

  @Test
  void patchAccruals_withValidAccruals_shouldMakePatchRequestWithCorrectBody()
      throws JsonProcessingException {
    String tenantId = "3fa85f64-5717-4562-b3fc-2c963f66afa6";

    Accrual accrual1 = createAccrual(UUID.fromString("0936e7a6-2b2e-1696-2546-5dd25dcae6a0"));
    Accrual accrual2 = createAccrual(UUID.fromString("a613dd93-3bdf-d285-c263-84d6866d61c5"));
    List<Accrual> accrualList = new ArrayList<>();

    accrualList.add(accrual1);
    accrualList.add(accrual2);

    PatchBody body1 = createPatchBody(accrual1);
    PatchBody body2 = createPatchBody(accrual2);
    List<PatchBody> expectedPatchBodies = List.of(body1, body2);

    String responseString = "{ \"meta\": { \"next\": null }, \"items\": [] }";
    ApiResponse<Accrual> apiResponse =
        objectMapper.readValue(responseString, new TypeReference<>() {
        });

    when(restTemplate.exchange(any(String.class), eq(HttpMethod.PATCH),
        Mockito.<HttpEntity<List<PatchBody>>>any() ,
        Mockito.<ParameterizedTypeReference<ApiResponse<Accrual>>>any(),
        Mockito.<Map<String, ?>>any()))
        .thenReturn(new ResponseEntity<>(apiResponse, HttpStatus.OK));

    accrualsService.updateAccruals(tenantId, accrualList);

    verify(restTemplate).exchange(any(String.class), eq(HttpMethod.PATCH), patchBodyListCaptor.capture(),
        Mockito.<ParameterizedTypeReference<ApiResponse<Accrual>>>any(),
        Mockito.<Map<String, ?>>any());

    List<PatchBody> actualPatchBodies = patchBodyListCaptor.getValue().getBody();

    assertThat(actualPatchBodies).hasSize(2);

    IntStream.range(0, actualPatchBodies.size()).forEach(index -> {
      PatchBody expectedBody = expectedPatchBodies.get(index);
      PatchBody actualBody = actualPatchBodies.get(index);

      assertThat(actualBody.getOp()).isEqualTo(expectedBody.getOp());
      assertThat(actualBody.getPath()).isEqualTo(expectedBody.getPath());
      assertThat(actualBody.getValue()).isEqualTo(expectedBody.getValue());
    });
  }

  private PatchBody createPatchBody(Accrual accrual) {
    return PatchBody.builder()
        .op("replace")
        .path("/" + accrual.getId().toString())
        .value(accrual)
        .build();
  }

}