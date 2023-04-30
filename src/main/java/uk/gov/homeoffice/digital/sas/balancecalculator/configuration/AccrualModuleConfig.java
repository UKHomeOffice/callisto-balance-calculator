package uk.gov.homeoffice.digital.sas.balancecalculator.configuration;

import java.util.List;
import org.springframework.context.annotation.Bean;
import uk.gov.homeoffice.digital.sas.balancecalculator.module.AccrualModule;
import uk.gov.homeoffice.digital.sas.balancecalculator.module.AnnualTargetHoursAccrualModule;

public class AccrualModuleConfig {

  @Bean
  List<AccrualModule> accrualModules() {
    return List.of(
        new AnnualTargetHoursAccrualModule()
    );
  }
}