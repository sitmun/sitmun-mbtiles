package org.sitmun.mbtiles.service;

import org.sitmun.mbtiles.batch.MBTilesTaskContext;
import org.sitmun.mbtiles.dto.MBTilesEstimateDto;

public interface MBTilesEstimateStrategy {
  boolean accept(MBTilesTaskContext context);

  MBTilesEstimateDto estimate(MBTilesTaskContext context);
}
