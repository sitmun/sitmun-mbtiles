package org.sitmun.mbtiles.dto;

import java.util.List;
import lombok.*;

@Value
@Builder
@With
public class TileServiceDto {

  String url;
  List<String> layers;
  String type;
  BoundingBoxDto bbox;
  int minZoom;
  int maxZoom;
  String matrixSet;
}
