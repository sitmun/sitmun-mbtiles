package org.sitmun.mbtiles.dto;

import java.util.List;
import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TileRequestDto {
  private List<MapServiceDto> mapServices;
  private double minLat;
  private double minLon;
  private double maxLat;
  private double maxLon;
  private int minZoom;
  private int maxZoom;
  private String srs;
}
