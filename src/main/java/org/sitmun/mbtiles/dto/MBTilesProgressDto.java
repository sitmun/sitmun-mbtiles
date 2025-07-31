package org.sitmun.mbtiles.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class MBTilesProgressDto {

  private long totalTiles;
  private long processedTiles;
}
