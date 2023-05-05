package uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual;

import static uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.constants.AgreementTypes.AHA_AGREEMENT_TYPE;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "agreementType")
@JsonSubTypes({
    @JsonSubTypes.Type(value = AhaContractualTerms.class, name = AHA_AGREEMENT_TYPE)
})
public interface ContractualTerms {
}
