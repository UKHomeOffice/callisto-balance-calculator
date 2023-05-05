package uk.gov.homeoffice.digital.sas.balancecalculator.models;

import java.net.URL;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// TODO: re-use JPA REST's  ApiResponse after adding NoArg constructor
// TODO: should the ApiResponse class be in an independent library?
//  (for client code only needing to consume JPA REST response, and not necessarily the
//  full features of the jparest library)
@NoArgsConstructor(force = true)
public class ApiResponse<T> {

  public static class Metadata {
    @Getter
    @Setter
    private URL next;
  }

  @Getter
  private final Metadata meta;

  @Getter
  private final List<T> items;
}
