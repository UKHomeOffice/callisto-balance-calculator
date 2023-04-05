package uk.gov.homeoffice.digital.sas.balancecalculator.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import java.util.Date;

public class Utils {

  private Utils() {

  }

  public static Gson createTimeJsonDeserializer() {
    return new GsonBuilder()
        .registerTypeAdapter(Date.class, (JsonDeserializer<Date>) (jsonElement, type, context) ->
            new Date(jsonElement.getAsJsonPrimitive().getAsLong()))
        .create();
  }
}

