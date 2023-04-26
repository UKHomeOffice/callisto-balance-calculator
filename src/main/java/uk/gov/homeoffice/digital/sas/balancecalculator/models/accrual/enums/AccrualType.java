package uk.gov.homeoffice.digital.sas.balancecalculator.models.accrual.enums;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AccrualType {
  ANNUAL_TARGET_HOURS(UUID.fromString("e502eebb-4663-4e5b-9445-9a20441c18d9"), true),
  NIGHT_HOURS(UUID.fromString("5f06e6ce-1422-4a0c-89dd-f4952e735202"), false),
  WEEKEND_HOURS(UUID.fromString("05bbd915-e907-4259-a2e2-080d7956afec"), false),
  PUBLIC_HOLIDAY_HOURS(UUID.fromString("b94bb25a-7fe2-4599-91ab-f0d58e013aed"), false),
  ON_CALL_WEEKDAY(UUID.fromString("2a5ea69d-1a2c-409d-b430-43a5dbc403b3"), false),
  ON_CALL_WEEKEND(UUID.fromString("df4c4b08-ac4a-45e0-83bb-856d3219a8b3"), false),
  ON_CALL_PUBLIC_HOLIDAY(UUID.fromString("a628bf34-d834-437d-a57a-ed549bd9a330"), false),
  FLEXIBLE_CREDITS(UUID.fromString("c4fd5435-8239-4f1f-9c4b-7f458b7b636d"), false),
  ROSTERED_SHIFT_ALLOWANCE(UUID.fromString("c73030ed-ed28-4d59-85e8-185f70d85a94"), false),
  PUBLIC_HOLIDAY_CREDIT(UUID.fromString("787d2d12-2aff-4253-b382-bcefded61124"), false);

  private final UUID id;
  private final boolean enabled;
}
