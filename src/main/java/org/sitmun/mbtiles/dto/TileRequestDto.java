package org.sitmun.mbtiles.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.*;

@Value
@Builder
@With
public class TileRequestDto {
  @NotNull(message = "Map services list cannot be null")
  @Size(min = 1, message = "At least one map service must be provided")
  @Valid
  List<MapServiceDto> mapServices;

  @NotNull(message = "Bounding box cannot be null")
  @Valid
  BoundingBoxDto bbox;

  @Min(value = 0, message = "Minimum zoom level must be at least 0")
  int minZoom;

  @Min(value = 0, message = "Maximum zoom level must be at least 0")
  int maxZoom;

  /**
   * Validates that the minimum zoom level is less than or equal to the maximum zoom level. This
   * method is automatically called by Bean Validation.
   */
  @AssertTrue(message = "Minimum zoom level must be less than or equal to maximum zoom level")
  public boolean isZoomLevelsValid() {
    return minZoom <= maxZoom;
  }
}
