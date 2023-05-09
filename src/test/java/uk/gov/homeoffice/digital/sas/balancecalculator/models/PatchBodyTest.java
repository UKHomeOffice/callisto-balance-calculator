package uk.gov.homeoffice.digital.sas.balancecalculator.models;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Accrual;

@Disabled
class PatchBodyTest {

  @Test
  void sameReference_shouldBeDeemedIdentical() {
    PatchBody patchBody = PatchBody.builder().build();
    Object nonPatchBody = patchBody;

    assertThat(patchBody)
        .isEqualTo(nonPatchBody)
        .hasSameHashCodeAs(nonPatchBody.hashCode());
  }

  @Test
  void comparedWithNonPatchBodyObject_shouldBeDeemedDifferent() {
    PatchBody patchBody = PatchBody.builder().build();
    Object nonPatchBody = new Object();

    assertThat(patchBody).isNotEqualTo(nonPatchBody);
    assertThat(patchBody.hashCode()).isNotEqualTo(nonPatchBody.hashCode());
  }

  @Test
  void patchBodiesWithSameOpSamePathSameValue_shouldBeDeemedIdentical() {
    Accrual accrual = Accrual.builder().build();
    PatchBody patchBody1 = PatchBody.builder()
        .op("replace")
        .path("/same/path")
        .value(accrual)
        .build();
    PatchBody patchBody2 = PatchBody.builder()
        .op("replace")
        .path("/same/path")
        .value(accrual)
        .build();

    assertThat(patchBody1)
        .isEqualTo(patchBody2)
        .hasSameHashCodeAs(patchBody2);
  }

  @Test
  void patchBodiesWithSamePathSameValueDifferentOp_shouldBeDeemedDifferent() {
    Accrual accrual = Accrual.builder().build();
    PatchBody patchBody1 = PatchBody.builder()
        .op("replace")
        .path("/same/path")
        .value(accrual)
        .build();
    PatchBody patchBody2 = PatchBody.builder()
        .op("delete")
        .path("/same/path")
        .value(accrual)
        .build();

    assertThat(patchBody1).isNotEqualTo(patchBody2);
    assertThat(patchBody1.hashCode()).isNotEqualTo(patchBody2.hashCode());
  }

  @Test
  void patchBodyWithNullOpSamePathSameValue_shouldBeDeemedDifferent() {
    Accrual accrual = Accrual.builder().build();
    PatchBody patchBody1 = PatchBody.builder()
        .op(null)
        .path("/same/path")
        .value(accrual)
        .build();
    PatchBody patchBody2 = PatchBody.builder()
        .op("replace")
        .path("/some/path")
        .value(accrual)
        .build();

    assertThat(patchBody1).isNotEqualTo(patchBody2);
    assertThat(patchBody1.hashCode()).isNotEqualTo(patchBody2.hashCode());
  }

  @Test
  void patchBodyWithNullPathSameOpSameValue_shouldBeDeemedDifferent() {
    Accrual accrual = Accrual.builder().build();
    PatchBody patchBody1 = PatchBody.builder()
        .op("replace")
        .path(null)
        .value(accrual)
        .build();
    PatchBody patchBody2 = PatchBody.builder()
        .op("replace")
        .path("/some/path")
        .value(accrual)
        .build();

    assertThat(patchBody1).isNotEqualTo(patchBody2);
    assertThat(patchBody1.hashCode()).isNotEqualTo(patchBody2.hashCode());
  }

  @Test
  void patchBodyWithNullValueSameOpSamePath_shouldBeDeemedDifferent() {
    Accrual accrual = Accrual.builder().build();
    PatchBody patchBody1 = PatchBody.builder()
        .op("replace")
        .path("/some/path")
        .value(null)
        .build();
    PatchBody patchBody2 = PatchBody.builder()
        .op("replace")
        .path("/some/path")
        .value(accrual)
        .build();

    assertThat(patchBody1).isNotEqualTo(patchBody2);
    assertThat(patchBody1.hashCode()).isNotEqualTo(patchBody2.hashCode());
  }

  @Test
  void patchBodiesWithSameOpSamePathDifferentValues_shouldBeDeemedDifferent() {
    Accrual accrual1 = Accrual.builder().build();
    PatchBody patchBody1 = PatchBody.builder()
        .op("replace")
        .path("/same/path")
        .value(accrual1)
        .build();
    Accrual accrual2 = Accrual.builder().build();
    PatchBody patchBody2 = PatchBody.builder()
        .op("replace")
        .path("/some/path")
        .value(accrual2)
        .build();

    assertThat(patchBody1).isNotEqualTo(patchBody2);
    assertThat(patchBody1.hashCode()).isNotEqualTo(patchBody2.hashCode());
  }
}