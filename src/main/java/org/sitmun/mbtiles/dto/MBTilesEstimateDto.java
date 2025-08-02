package org.sitmun.mbtiles.dto;

import lombok.*;

@Value
@Builder
@With
public class MBTilesEstimateDto {
  int tileCount;
  double estimatedTileSizeKb;
  double estimatedMbtilesSizeMb;
}
