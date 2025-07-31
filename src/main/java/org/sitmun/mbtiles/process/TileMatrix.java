package org.sitmun.mbtiles.process;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TileMatrix {

  private String matrix;
  private int zoomLevel;
  private int minRow;
  private int maxRow;
  private int minCol;
  private int maxCol;
}
