package org.sitmun.mbtiles.service;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.sitmun.mbtiles.batch.MBTilesNoStrategyException;
import org.sitmun.mbtiles.batch.MBTilesTaskContext;
import org.sitmun.mbtiles.dto.MBTilesEstimateDto;
import org.sitmun.mbtiles.dto.MapServiceDto;
import org.sitmun.mbtiles.dto.TileRequestDto;
import org.sitmun.mbtiles.dto.TileServiceDto;
import org.sitmun.mbtiles.tilesources.wmts.WMTSConstants;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MBTilesEstimateService {

  private final List<MBTilesEstimateStrategy> mbTilesStrategies;

  public MBTilesEstimateService(List<MBTilesEstimateStrategy> mbTilesStrategies) {
    this.mbTilesStrategies = mbTilesStrategies;
  }

  public MBTilesEstimateDto estimateSize(TileRequestDto tileRequest) {
    MBTilesEstimateDto estimation = MBTilesEstimateDto.builder().build();
    for (MapServiceDto mapService : tileRequest.getMapServices()) {
      MBTilesTaskContext context = getContext(tileRequest, mapService);
      try {
        MBTilesEstimateDto result = computeEstimate(context);
        if (result != null) {
          estimation = sumMBTilesEstimations(estimation, result);
        }
      } catch (Exception e) {
        log.error("Error estimating MBTiles size", e);
        throw new MBTilesUnexpectedInternalException(e);
      }
    }
    return estimation;
  }

  private MBTilesEstimateDto computeEstimate(MBTilesTaskContext context)
      throws JobExecutionException {
    MBTilesEstimateDto result;
    try {
      result =
          mbTilesStrategies.stream()
              .filter(estimateStrategy -> estimateStrategy.accept(context))
              .findFirst()
              .orElseThrow(MBTilesNoStrategyException::new)
              .estimate(context);
    } catch (Exception e) {
      throw new JobExecutionException(e.getMessage(), e);
    }
    return result;
  }

  @NotNull
  private static MBTilesTaskContext getContext(
      TileRequestDto tileRequest, MapServiceDto mapService) {
    TileServiceDto tileService =
        TileServiceDto.builder()
            .url(mapService.getUrl())
            .layers(mapService.getLayers())
            .type(mapService.getType())
            .bbox(tileRequest.getBbox())
            .minZoom(tileRequest.getMinZoom())
            .maxZoom(tileRequest.getMaxZoom())
            .matrixSet(WMTSConstants.MB_TILES_SRS)
            .build();
    return MBTilesTaskContext.builder().service(tileService).build();
  }

  private MBTilesEstimateDto sumMBTilesEstimations(
      MBTilesEstimateDto total, @NotNull MBTilesEstimateDto newSize) {
    // Store old values before updating tile count
    int oldTotalTileCount = total.getTileCount();
    double oldEstimatedTileSizeKb = total.getEstimatedTileSizeKb();
    double oldTotalWeightedSize = oldEstimatedTileSizeKb * oldTotalTileCount;

    // Update tile count first
    int newTotalTileCount = oldTotalTileCount + newSize.getTileCount();
    total = total.withTileCount(newTotalTileCount);

    // Calculate weighted average tile size only if we have tiles
    if (newTotalTileCount > 0) {
      double totalWeightedSize =
          oldTotalWeightedSize + newSize.getEstimatedTileSizeKb() * newSize.getTileCount();
      double newEstimateTileSize = totalWeightedSize / newTotalTileCount;
      total = total.withEstimatedTileSizeKb(newEstimateTileSize);
    } else {
      // If no tiles, set tile size to 0
      total = total.withEstimatedTileSizeKb(0.0);
    }

    // Sum the MBTiles size (maintain original behavior)
    total =
        total.withEstimatedMbtilesSizeMb(
            total.getEstimatedMbtilesSizeMb() + newSize.getEstimatedMbtilesSizeMb());
    return total;
  }
}
