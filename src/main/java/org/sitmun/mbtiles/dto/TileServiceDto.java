package org.sitmun.mbtiles.dto;

import java.util.List;
import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TileServiceDto {

  private String url;
  private List<String> layers;
  private String type;
  private double minLat;
  private double minLon;
  private double maxLat;
  private double maxLon;
  private int minZoom;
  private int maxZoom;
  private String srs;
  private String matrixSet;
}
