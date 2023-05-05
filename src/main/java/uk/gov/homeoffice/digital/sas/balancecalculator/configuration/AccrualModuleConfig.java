package uk.gov.homeoffice.digital.sas.balancecalculator.configuration;

import org.springframework.context.annotation.Bean;
import uk.gov.homeoffice.digital.sas.balancecalculator.module.AccrualModule;
import uk.gov.homeoffice.digital.sas.balancecalculator.module.AnnualTargetHoursAccrualModule;

import java.util.List;

public class AccrualModuleConfig {

  @Bean
  List<AccrualModule> accrualModules() {
    return List.of(
        new AnnualTargetHoursAccrualModule()
    );
  }
}