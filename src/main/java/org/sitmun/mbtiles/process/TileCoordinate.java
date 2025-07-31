package org.sitmun.mbtiles.process;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class TileCoordinate {
  private final int x;
  private final int y;
  private final int z;
  private final String zId;
}
