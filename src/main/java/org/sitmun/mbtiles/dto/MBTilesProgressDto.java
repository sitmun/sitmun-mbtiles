package org.sitmun.mbtiles.dto;

import lombok.*;

@Value
@Builder
@With
public class MBTilesProgressDto {

  long totalTiles;
  long processedTiles;
}
