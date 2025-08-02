package org.sitmun.mbtiles.batch;

import org.springframework.batch.core.scope.context.StepContext;

public interface MBTilesTaskStrategy {
  boolean accept(MBTilesTaskContext mbtilesContext);

  void process(StepContext stepContext, MBTilesTaskContext mbtilesContext);
}
