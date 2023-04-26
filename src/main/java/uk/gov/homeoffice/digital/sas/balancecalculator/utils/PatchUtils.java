package uk.gov.homeoffice.digital.sas.balancecalculator.utils;

import java.util.ArrayList;
import java.util.List;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.PatchBody;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Accrual;

public class PatchUtils {

  private PatchUtils() {
  }

  public static List<PatchBody> createPatchBody(List<Accrual> accruals) {
    List<PatchBody> body = new ArrayList<>();

    accruals.forEach(a -> {
      if (a.getId() != null) {
        PatchBody blob = new PatchBody();
        blob.setOp("replace");
        blob.setPath("/" + a.getId().toString());
        blob.setValue(a);
        body.add(blob);
      }

    });

    return body;
  }

}
