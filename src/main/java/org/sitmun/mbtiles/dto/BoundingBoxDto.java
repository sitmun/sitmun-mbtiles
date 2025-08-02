package org.sitmun.mbtiles.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Value;
import lombok.With;

@Value
@Builder
@With
public class BoundingBoxDto {
  double minY;
  double minX;
  double maxY;
  double maxX;

  @NotNull(message = "SRS (Spatial Reference System) cannot be null")
  @Pattern(
      regexp = "^EPSG:\\d+$",
      message = "SRS must be in EPSG format (e.g., EPSG:4326, EPSG:3857)")
  String srs;

  /**
   * Validates that minimum X coordinate is less than or equal to maximum X coordinate. This method
   * is automatically called by Bean Validation.
   */
  @AssertTrue(message = "Minimum X coordinate must be less than or equal to maximum X coordinate")
  public boolean isXBoundsValid() {
    return minX <= maxX;
  }

  /**
   * Validates that minimum Y coordinate is less than or equal to maximum Y coordinate. This method
   * is automatically called by Bean Validation.
   */
  @AssertTrue(message = "Minimum Y coordinate must be less than or equal to maximum Y coordinate")
  public boolean isYBoundsValid() {
    return minY <= maxY;
  }

  public double[] toBounds() {
    return new double[] {minX, minY, maxX, maxY};
  }
}
