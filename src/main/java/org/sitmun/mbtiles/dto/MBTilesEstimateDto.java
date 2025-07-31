package org.sitmun.mbtiles.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MBTilesEstimateDto {
  private int tileCount;
  private double estimatedTileSizeKb;
  private double estimatedMbtilesSizeMb;
}
