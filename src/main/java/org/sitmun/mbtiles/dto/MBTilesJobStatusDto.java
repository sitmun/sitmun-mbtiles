package org.sitmun.mbtiles.dto;

import lombok.*;

@Value
@Builder
@With
public class MBTilesJobStatusDto {
  String status;
  long processedTiles;
  long totalTiles;
  String errorMessage;
}
