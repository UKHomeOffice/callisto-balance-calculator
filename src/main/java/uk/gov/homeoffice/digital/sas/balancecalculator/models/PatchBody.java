package uk.gov.homeoffice.digital.sas.balancecalculator.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Accrual;

@Getter
@AllArgsConstructor
@Builder
public class PatchBody {

  String op;

  String path;

  Accrual value;
}
