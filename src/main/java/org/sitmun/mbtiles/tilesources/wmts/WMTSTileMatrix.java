package org.sitmun.mbtiles.tilesources.wmts;

import lombok.*;

@Builder
@Value
@With
public class WMTSTileMatrix {

  String matrix;
  int zoomLevel;
  int minRow;
  int maxRow;
  int minCol;
  int maxCol;
}
