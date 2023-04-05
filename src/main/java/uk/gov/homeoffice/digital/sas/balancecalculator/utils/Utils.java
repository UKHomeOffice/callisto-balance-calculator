package uk.gov.homeoffice.digital.sas.balancecalculator.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import java.util.Date;

public class Utils {

  private Utils() {

  }

  public static Gson createTimeJsonDeserializer() {
    return new GsonBuilder()
        .registerTypeAdapter(Date.class, new JsonDeserializer<Date>() {
          public Date deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) throws JsonParseException {
            return new Date(jsonElement.getAsJsonPrimitive().getAsLong());
          }
        })
        .create();
  }
}

