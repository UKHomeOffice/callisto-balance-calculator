package uk.gov.homeoffice.digital.sas.balancecalculator.models;

import org.junit.jupiter.api.Test;
import com.openpojo.validation.Validator;
import com.openpojo.validation.ValidatorBuilder;
import com.openpojo.validation.test.impl.GetterTester;
import com.openpojo.validation.test.impl.SetterTester;

class ModelPojoTest {

    @Test
    void validate() {
      Validator validator = ValidatorBuilder.create()
          .with(new SetterTester(),
              new GetterTester())
          .build();
      validator.validate(this.getClass().getPackageName());
    }
}
