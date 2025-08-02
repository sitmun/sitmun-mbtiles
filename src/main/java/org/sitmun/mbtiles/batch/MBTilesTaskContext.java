package org.sitmun.mbtiles.batch;

import lombok.*;
import org.sitmun.mbtiles.dto.TileServiceDto;

@Value
@Builder
@With
public class MBTilesTaskContext {
  TileServiceDto service;
  String outputPath;
}
