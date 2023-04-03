package uk.gov.homeoffice.digital.sas.balancecalculator;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class ActuatorCounters {

  private final MeterRegistry meterRegistry;

  public ActuatorCounters(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  public Counter setUpCounters(String endpointUrl, String type, String description) {
    return Counter.builder(endpointUrl)
        .tag("type", type)
        .description(description)
        .register(meterRegistry);
  }
}
