package uk.gov.homeoffice.digital.sas.balancecalculator.handlers;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Accrual;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.Agreement;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.enums.AccrualType;
import uk.gov.homeoffice.digital.sas.balancecalculator.models.timecard.TimeEntry;
import uk.gov.homeoffice.digital.sas.balancecalculator.module.AccrualModule;
import uk.gov.homeoffice.digital.sas.kafka.message.KafkaAction;

@Component
@Slf4j
public class ContributionsHandler {

  private final List<AccrualModule> accrualModules;

  @Autowired
  public ContributionsHandler(List<AccrualModule> accrualModules) {
    this.accrualModules = accrualModules;
  }

  public boolean handle(TimeEntry timeEntry,
                              KafkaAction action,
                              Agreement applicableAgreement,
                              Map<AccrualType, SortedMap<LocalDate, Accrual>> allAccruals) {

    for (AccrualModule module : accrualModules) {
      AccrualType accrualType = module.getAccrualType();
      SortedMap<LocalDate, Accrual> accruals = allAccruals.get(accrualType);

      boolean result = module.applyTimeEntryToAccruals(timeEntry, action,
          accruals, applicableAgreement);

      if (!result) {
        return false;
      }
    }
    return true;
  }
}
