package org.sitmun.mbtiles.tilesources.wmts;

import ch.poole.geo.mbtiles4j.MBTilesWriteException;
import ch.poole.geo.mbtiles4j.Tile;
import ch.poole.geo.mbtiles4j.model.MetadataBounds;
import ch.poole.geo.mbtiles4j.model.MetadataEntry;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.mbtiles.batch.MBTilesTaskContext;
import org.sitmun.mbtiles.batch.MBTilesTaskStrategy;
import org.sitmun.mbtiles.dto.BoundingBoxDto;
import org.sitmun.mbtiles.dto.MBTilesEstimateDto;
import org.sitmun.mbtiles.dto.TileServiceDto;
import org.sitmun.mbtiles.io.CustomMBTilesReader;
import org.sitmun.mbtiles.io.CustomMBTilesWriter;
import org.sitmun.mbtiles.service.MBTilesEstimateStrategy;
import org.sitmun.mbtiles.service.MBTilesProgressService;
import org.sitmun.mbtiles.utils.*;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class WMTSProcess implements MBTilesTaskStrategy, MBTilesEstimateStrategy {

  private final MBTilesProgressService mbTilesProgressService;

  public WMTSProcess(MBTilesProgressService mbTilesProgressService) {
    this.mbTilesProgressService = mbTilesProgressService;
  }

  @Override
  public boolean accept(MBTilesTaskContext taskContext) {
    TileServiceDto service = taskContext.getService();
    return WMTSConstants.WMTS_TYPE.equals(service.getType());
  }

  @Override
  public MBTilesEstimateDto estimate(MBTilesTaskContext context) {
    try {
      TileServiceDto service = context.getService();
      WMTSCapabilities capabilities = WMTSCapabilitiesUtils.parseWMTSCapabilities(service);
      BoundingBoxDto bbox = service.getBbox();
      double[] bounds = bbox.toBounds();
      int tileCount = 0;

      if (!bbox.getSrs().equals(WMTSConstants.MB_TILES_SRS)) {
        bounds =
            Proj4CoordinateUtils.transformExtent(bounds, bbox.getSrs(), WMTSConstants.MB_TILES_SRS);
      }
      double estimateSize = 0;
      for (WMTSLayerCapabilities lc : capabilities.getLayers()) {
        log.info("Estimating size for layer: {}", lc.getLayerIdentifier());
        List<WMTSTileCoordinate> coordinates =
            WMTSCoordinatesUtils.calculateCoordinates(service, lc, bounds);
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
      TileServiceDto service, String layer, List<WMTSTileCoordinate> coordinates) {
    double estimation = 0;
    Map<String, List<WMTSTileCoordinate>> coordinatesByZoom = new HashMap<>();

    coordinates.forEach(
        coordinate -> {
          if (coordinatesByZoom.containsKey(coordinate.getZId())) {
            coordinatesByZoom.get(coordinate.getZId()).add(coordinate);
          } else {
            List<WMTSTileCoordinate> zoomCoordinates = new ArrayList<>();
            zoomCoordinates.add(coordinate);
            coordinatesByZoom.put(coordinate.getZId(), zoomCoordinates);
          }
        });

    for (Map.Entry<String, List<WMTSTileCoordinate>> entry : coordinatesByZoom.entrySet()) {
      List<WMTSTileCoordinate> coordinatesForZoom = entry.getValue();
      int maxX =
          coordinatesForZoom.stream()
              .map(WMTSTileCoordinate::getX)
              .max(Comparator.naturalOrder())
              .orElse(Integer.MAX_VALUE);
      int minX =
          coordinatesForZoom.stream()
              .map(WMTSTileCoordinate::getX)
              .min(Comparator.naturalOrder())
              .orElse(Integer.MIN_VALUE);
      int maxY =
          coordinatesForZoom.stream()
              .map(WMTSTileCoordinate::getY)
              .max(Comparator.naturalOrder())
              .orElse(Integer.MAX_VALUE);
      int minY =
          coordinatesForZoom.stream()
              .map(WMTSTileCoordinate::getY)
              .min(Comparator.naturalOrder())
              .orElse(Integer.MIN_VALUE);
      int centerX = (minX + maxX) / 2;
      int centerY = (minY + maxY) / 2;
      int coordinatesCount = coordinatesForZoom.size();
      List<int[]> sampleCoordinates = new ArrayList<>();
      sampleCoordinates.add(new int[] {minX, minY});
      sampleCoordinates.add(new int[] {maxX, maxY});
      sampleCoordinates.add(new int[] {centerX, centerY});
      sampleCoordinates.add(new int[] {centerX - 1, centerY + 1});
      sampleCoordinates.add(new int[] {centerX + 1, centerY - 1});
      int validSampleCount = 0;
      String zoomLevel = entry.getKey();
      int totalBytes = 0;
      byte[] tileData;
      for (int[] coordinate : sampleCoordinates) {
        int tileColumn = coordinate[0];
        int tileRow = coordinate[1];
        tileData =
            WMTSUtils.downloadTile(
                service.getUrl(), layer, service.getMatrixSet(), tileColumn, tileRow, zoomLevel);
        if (tileData != null) {
          totalBytes += tileData.length;
          validSampleCount++;
        }
      }
      double averageTileSize = validSampleCount > 0 ? ((double) totalBytes / validSampleCount) : 0;
      log.info(
          "AvgSize {} -> {}KB ({} tiles)", zoomLevel, averageTileSize / 1024, validSampleCount);
      estimation += averageTileSize * coordinatesCount;
    }

    return estimation;
  }

  @Override
  public void process(StepContext stepContext, MBTilesTaskContext mbtilesContext) {
    try {
      TileServiceDto service = mbtilesContext.getService();
      String outputPath = mbtilesContext.getOutputPath();
      WMTSCapabilities capabilities = WMTSCapabilitiesUtils.parseWMTSCapabilities(service);
      File outputFile = new File(outputPath);
      CustomMBTilesWriter writer = new CustomMBTilesWriter(outputFile);
      CustomMBTilesReader reader = new CustomMBTilesReader(outputFile);
      BoundingBoxDto bbox = service.getBbox();
      double[] bounds = bbox.toBounds();

      if (!bbox.getSrs().equals(WMTSConstants.MB_TILES_SRS)) {
        bounds =
            Proj4CoordinateUtils.transformExtent(bounds, bbox.getSrs(), WMTSConstants.MB_TILES_SRS);
      }
      long processedTiles = 0;
      long totalTiles = 0;
      Map<WMTSLayerCapabilities, List<WMTSTileCoordinate>> layersCoordinates = new HashMap<>();
      for (WMTSLayerCapabilities layerCapabilities : capabilities.getLayers()) {
        List<WMTSTileCoordinate> coordinates =
            WMTSCoordinatesUtils.calculateCoordinates(service, layerCapabilities, bounds);
        layersCoordinates.put(layerCapabilities, coordinates);
        totalTiles += coordinates.size();
      }
      long jobId = stepContext.getJobInstanceId();
      mbTilesProgressService.updateJobProgress(jobId, totalTiles, processedTiles);
      for (Map.Entry<WMTSLayerCapabilities, List<WMTSTileCoordinate>> entry :
          layersCoordinates.entrySet()) {
        WMTSLayerCapabilities layerCapabilities = entry.getKey();
        List<WMTSTileCoordinate> coordinates = entry.getValue();
        for (WMTSTileCoordinate coordinate : coordinates) {
          // Check if the job has been stopped by checking the step execution status
          if (stepContext.getStepExecution().getStatus().isUnsuccessful()
              || stepContext.getStepExecution().getJobExecution().getStatus().isUnsuccessful()) {
            log.info("Job {} has been stopped, exiting tile processing", jobId);
            return;
          }

          BufferedImage tile =
              getTileImage(service, layerCapabilities.getLayerIdentifier(), coordinate);

          if (tile != null) {
            addOrUpdateTitle(coordinate, reader, tile, writer);
          }
          processedTiles++;

          // Check for stop signals more frequently (every 10 tiles instead of 50)
          if (processedTiles % 10 == 0) {
            mbTilesProgressService.updateJobProgress(jobId, totalTiles, processedTiles);

            // Additional check for stop signals
            if (stepContext.getStepExecution().getStatus().isUnsuccessful()
                || stepContext.getStepExecution().getJobExecution().getStatus().isUnsuccessful()) {
              log.info(
                  "Job {} has been stopped during progress update, exiting tile processing", jobId);
              return;
            }
          }
        }
      }
      bounds = calculateBounds(service, capabilities.getLayers());
      MetadataBounds metBounds = new MetadataBounds(bounds[0], bounds[1], bounds[2], bounds[3]);
      writer.addMetadataEntry(
          new MetadataEntry(
              service.getLayers().get(0),
              MetadataEntry.TileSetType.BASE_LAYER,
              "1.0",
              "Layer generated by Sitmun mbtiles",
              MetadataEntry.TileMimeType.PNG,
              metBounds));
      writer.close();
    } catch (Exception e) {
      throw new WMTSHarvestException(e);
    }
  }

  private void addOrUpdateTitle(
      WMTSTileCoordinate coordinate,
      CustomMBTilesReader reader,
      BufferedImage tile,
      CustomMBTilesWriter writer)
      throws IOException, MBTilesWriteException {
    int zoomLevel = coordinate.getZ();
    int yTMS = invertTileRow(coordinate.getY(), zoomLevel);
    BufferedImage saveTile = checkTileImage(reader, zoomLevel, coordinate.getX(), yTMS);
    if (saveTile == null) {
      try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
        ImageIO.write(tile, "png", outputStream);
        writer.addTile(outputStream.toByteArray(), zoomLevel, coordinate.getX(), yTMS);
      }
    } else {
      BufferedImage combinedTile = writer.combineTiles(saveTile, tile);
      try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
        ImageIO.write(combinedTile, "png", outputStream);
        writer.updateTile(zoomLevel, coordinate.getX(), yTMS, outputStream.toByteArray());
      }
    }
  }

  private BufferedImage getTileImage(
      TileServiceDto service, String layer, WMTSTileCoordinate coordinate) {
    byte[] tileData =
        WMTSUtils.downloadTile(
            service.getUrl(),
            layer,
            service.getMatrixSet(),
            coordinate.getX(),
            coordinate.getY(),
            coordinate.getZId());

    if (tileData != null) {
      try {
        return ImageIO.read(new ByteArrayInputStream(tileData));
      } catch (IOException e) {
        log.error("Error decoding image", e);
      }
    }
    return null;
  }

  private double[] calculateBounds(TileServiceDto service, List<WMTSLayerCapabilities> layers) {
    double[] bounds = service.getBbox().toBounds();
    for (WMTSLayerCapabilities lc : layers) {
      if (lc.getMinLon() != null && lc.getMinLon() < bounds[0]) {
        bounds[0] = lc.getMinLon();
      }
      if (lc.getMinLat() != null && lc.getMinLat() < bounds[1]) {
        bounds[1] = lc.getMinLat();
      }
      if (lc.getMaxLon() != null && lc.getMaxLon() > bounds[2]) {
        bounds[2] = lc.getMaxLon();
      }
      if (lc.getMaxLat() != null && lc.getMaxLat() > bounds[3]) {
        bounds[3] = lc.getMaxLat();
      }
    }
    return bounds;
  }

  private int invertTileRow(int tileRow, int zoomLevel) {
    return ((1 << zoomLevel) - 1 - tileRow);
  }

  private BufferedImage checkTileImage(
      CustomMBTilesReader reader, int zoomLevel, int tileColumn, int tileRow) {
    try {
      Tile tile = reader.getTile(zoomLevel, tileColumn, tileRow);
      if (tile != null && tile.getData() != null) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(tile.getData().readAllBytes());
        return ImageIO.read(inputStream);
      }
      return null;
    } catch (Exception e) {
      log.error(
          "Error reading tile Z: {}, X: {}, Y: {} - {}",
          zoomLevel,
          tileColumn,
          tileRow,
          e.getMessage());
      return null;
    }
  }
}
