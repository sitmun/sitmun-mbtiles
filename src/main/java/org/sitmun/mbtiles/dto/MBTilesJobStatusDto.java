package org.sitmun.mbtiles.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MBTilesJobStatusDto {
  private String status;
  private Long processedTiles;
  private Long totalTiles;
  private String errorMessage;
}
