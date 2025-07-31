package org.sitmun.mbtiles.process;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.mbtiles.Constants;
import org.sitmun.mbtiles.dto.MBTilesEstimateDto;
import org.sitmun.mbtiles.dto.TileServiceDto;
import org.sitmun.mbtiles.jobs.TaskContext;
import org.sitmun.mbtiles.wmts.*;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class WMTSEstimateProcess implements Function<TaskContext, MBTilesEstimateDto> {

  @Override
  public MBTilesEstimateDto apply(TaskContext taskContext) {
    try {
      TileServiceDto service = taskContext.getService();
      Capabilities capabilities = CapabilitiesUtils.parseWMTSCapabilities(service);
      double[] bounds = {
        service.getMinLon(), service.getMinLat(), service.getMaxLon(), service.getMaxLat()
      };
      int tileCount = 0;

      if (!service.getSrs().equals(Constants.MB_TILES_SRS)) {
        bounds = Proj4Utils.transformExtent(bounds, service.getSrs(), Constants.MB_TILES_SRS);
      }
      double estimateSize = 0;
      for (LayerCapabilities lc : capabilities.getLayers()) {
        log.info("Estimating size for layer: {}", lc.getLayerIdentifier());
        List<TileCoordinate> coordinates =
            CoordinatesUtils.calculateCoordinates(service, lc, bounds);
        estimateSize += getAvgTileSize(service, lc.getLayerIdentifier(), coordinates) / 1024;
        tileCount += coordinates.size();
      }
      double avgSizeKb =
          BigDecimal.valueOf(estimateSize / tileCount)
              .setScale(3, RoundingMode.HALF_UP)
              .doubleValue();
      double avgSizeMb =
          BigDecimal.valueOf(estimateSize / 1024).setScale(3, RoundingMode.HALF_UP).doubleValue();
      log.info("Estimate size: {} KB per tile, {} MB MBtiles", avgSizeKb, avgSizeMb);
      return MBTilesEstimateDto.builder()
          .tileCount(tileCount)
          .estimatedTileSizeKb(avgSizeKb)
          .estimatedMbtilesSizeMb(avgSizeMb)
          .build();
    } catch (Exception e) {
      log.error("Error estimating WMTSTile size", e);
      return null;
    }
  }

  private double getAvgTileSize(
      TileServiceDto service, String layer, List<TileCoordinate> coordinates) {
    double estimation = 0;
    Map<String, List<TileCoordinate>> splitedCoordinates = new HashMap<>();

    coordinates.forEach(
        c -> {
          if (splitedCoordinates.containsKey(c.getZId())) {
            splitedCoordinates.get(c.getZId()).add(c);
          } else {
            List<TileCoordinate> zoomCoords = new ArrayList<>();
            zoomCoords.add(c);
            splitedCoordinates.put(c.getZId(), zoomCoords);
          }
        });

    for (Map.Entry<String, List<TileCoordinate>> entry : splitedCoordinates.entrySet()) {
      List<TileCoordinate> value = entry.getValue();
      int maxX =
          value.stream()
              .map(TileCoordinate::getX)
              .max(Comparator.naturalOrder())
              .orElse(Integer.MAX_VALUE);
      int minX =
          value.stream()
              .map(TileCoordinate::getX)
              .min(Comparator.naturalOrder())
              .orElse(Integer.MIN_VALUE);
      int maxY =
          value.stream()
              .map(TileCoordinate::getY)
              .max(Comparator.naturalOrder())
              .orElse(Integer.MAX_VALUE);
      int minY =
          value.stream()
              .map(TileCoordinate::getY)
              .min(Comparator.naturalOrder())
              .orElse(Integer.MIN_VALUE);
      int centerX = (minX + maxX) / 2;
      int centerY = (minY + maxY) / 2;
      int coordsSize = value.size();
      List<int[]> sampleCoords = new ArrayList<>();
      sampleCoords.add(new int[] {minX, minY});
      sampleCoords.add(new int[] {maxX, maxY});
      sampleCoords.add(new int[] {centerX, centerY});
      sampleCoords.add(new int[] {centerX - 1, centerY + 1});
      sampleCoords.add(new int[] {centerX + 1, centerY - 1});
      int sampleSize = 0;
      String zoom = entry.getKey();
      int bytes = 0;
      byte[] tile;
      for (int[] coord : sampleCoords) {
        int col = coord[0];
        int row = coord[1];
        tile =
            WMTSUtils.downloadTile(service.getUrl(), layer, service.getMatrixSet(), col, row, zoom);
        if (tile != null) {
          bytes += tile.length;
          sampleSize++;
        }
      }
      double zoomEstimation = sampleSize > 0 ? ((double) bytes / sampleSize) : 0;
      log.info("AvgSize {} -> {}KB ({} tiles)", zoom, zoomEstimation / 1024, sampleSize);
      estimation += zoomEstimation * coordsSize;
    }

    return estimation;
  }
}
