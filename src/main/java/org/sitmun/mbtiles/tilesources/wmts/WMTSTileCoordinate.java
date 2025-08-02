package org.sitmun.mbtiles.tilesources.wmts;

import lombok.*;

@Value
@Builder
@With
public class WMTSTileCoordinate {
  int x;
  int y;
  int z;
  String zId;
}
