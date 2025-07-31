package org.sitmun.mbtiles.wmts;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.mbtiles.Constants;
import org.sitmun.mbtiles.dto.TileServiceDto;
import org.sitmun.mbtiles.process.TileCoordinate;
import org.sitmun.mbtiles.process.TileMatrix;

@Slf4j
public class CoordinatesUtils {

  private CoordinatesUtils() {
    // Utility class, no instantiation
  }

  public static List<TileCoordinate> calculateCoordinates(
      TileServiceDto service, LayerCapabilities layerCapabilities, double[] bounds) {
    List<TileCoordinate> list = new ArrayList<>();
    for (int zoom = service.getMinZoom(); zoom <= service.getMaxZoom(); zoom++) {
      TileMatrix tileMatrix =
          calculateTileMatrixByExtent(layerCapabilities.getLimitsByMatrix(zoom), bounds, zoom);
      int maxCol = tileMatrix.getMaxCol();
      int maxRow = tileMatrix.getMaxRow();
      int minCol = tileMatrix.getMinCol();
      int minRow = tileMatrix.getMinRow();
      for (int x = minCol; x <= maxCol; x++) {
        for (int y = minRow; y <= maxRow; y++) {
          list.add(TileCoordinate.builder().x(x).y(y).z(zoom).zId(tileMatrix.getMatrix()).build());
        }
      }
    }
    return list;
  }

  private static TileMatrix calculateTileMatrixByExtent(
      TileMatrix tileMatrixOrig, double[] extent, int zoom) {
    int tileSize = 256;
    double originX = Constants.ORIGIN_X_3857;
    double originY = Constants.ORIGIN_Y_3857;
    double resolution = Constants.GLOBAL_SIZE_3857 / (tileSize * Math.pow(2, zoom));

    int tileMinX = Integer.MAX_VALUE;
    int tileMaxX = Integer.MIN_VALUE;
    int tileMinY = Integer.MAX_VALUE;
    int tileMaxY = Integer.MIN_VALUE;

    for (int x = tileMatrixOrig.getMinCol(); x <= tileMatrixOrig.getMaxCol(); x++) {
      double minX = originX + x * tileSize * resolution;
      double maxX = originX + (x + 1) * tileSize * resolution;

      if (maxX < extent[0] || minX > extent[2]) {
        continue;
      }
      if (x < tileMinX) {
        tileMinX = x;
      }
      if (x > tileMaxX) {
        tileMaxX = x;
      }

      for (int y = tileMatrixOrig.getMinRow(); y <= tileMatrixOrig.getMaxRow(); y++) {
        double maxY = originY - y * tileSize * resolution;
        double minY = originY - (y + 1) * tileSize * resolution;

        if (maxY < extent[1] || minY > extent[3]) {
          continue;
        }
        if (y < tileMinY) {
          tileMinY = y;
        }
        if (y > tileMaxY) {
          tileMaxY = y;
        }
      }
    }
    return new TileMatrix(
        tileMatrixOrig.getMatrix(),
        tileMatrixOrig.getZoomLevel(),
        tileMinY,
        tileMaxY,
        tileMinX,
        tileMaxX);
  }
}
