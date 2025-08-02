package org.sitmun.mbtiles.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.*;
import org.hibernate.validator.constraints.URL;

@Value
@Builder
@With
public class MapServiceDto {

  @NotNull(message = "Service URL cannot be null")
  @URL(regexp = "^(https?://).*", message = "Service URL must be a valid HTTP/HTTPS URL")
  String url;

  @NotNull(message = "Layers list cannot be null")
  @NotEmpty(message = "At least one layer must be specified")
  @Size(min = 1, message = "At least one layer must be specified")
  List<String> layers;

  @NotNull(message = "Service type cannot be null")
  String type;
}
