package uk.gov.homeoffice.digital.sas.balancecalculator.utils;


import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static uk.gov.homeoffice.digital.sas.balancecalculator.utils.PatchUtils.createPatchBody;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.PatchBody;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Accrual;

class PatchUtilsTest {


  @Test
  void createPatchBody_withValidAccruals_shouldReturnPatchBody() {
    Accrual accrual1 = createAccrual(UUID.fromString("0936e7a6-2b2e-1696-2546-5dd25dcae6a0"));
    Accrual accrual2 = createAccrual(UUID.fromString("a613dd93-3bdf-d285-c263-84d6866d61c5"));
    List<Accrual> accrualList = new ArrayList<>();

    accrualList.add(accrual1);
    accrualList.add(accrual2);

    List<PatchBody> patchBody = createPatchBody(accrualList);

    for(int i = 0; i < accrualList.size(); i++) {
      assertThat(patchBody.get(i).getOp()).isEqualTo("replace");
      assertThat(patchBody.get(i).getPath()).isEqualTo("/" + accrualList.get(i).getId());
      assertThat(patchBody.get(i).getValue()).isEqualTo(accrualList.get(i));
    }
  }

  @Test
  void createPatchBody_withNullId_shouldReturnEmptyBody() {
    Accrual accrual = createAccrual(null);
    List<Accrual> accrualList = new ArrayList<>();
    accrualList.add(accrual);

    List<PatchBody> patchBody = createPatchBody(accrualList);

    assertThat(patchBody.isEmpty()).isTrue();
  }

  private Accrual createAccrual(UUID id) {
    return Accrual.builder()
        .id(id)
        .tenantId(UUID.randomUUID())
        .build();
  }

}