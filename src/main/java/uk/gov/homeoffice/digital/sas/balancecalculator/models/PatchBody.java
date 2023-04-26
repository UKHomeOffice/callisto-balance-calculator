package uk.gov.homeoffice.digital.sas.balancecalculator.models;

import lombok.Getter;
import lombok.Setter;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Accrual;

@Getter
@Setter
public class PatchBody {

  String op;

  String path;

  Accrual value;
}
