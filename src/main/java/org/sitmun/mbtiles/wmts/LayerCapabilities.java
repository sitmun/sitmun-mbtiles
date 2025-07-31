package org.sitmun.mbtiles.wmts;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.mbtiles.process.TileMatrix;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class LayerCapabilities {
  String layerIdentifier;
  List<TileMatrix> limits = new ArrayList<>();
  Double minLon;
  Double minLat;
  Double maxLon;
  Double maxLat;

  public TileMatrix getLimitsByMatrix(int id) {
    return limits.stream().filter(l -> l.getZoomLevel() == id).findFirst().orElse(null);
  }

  public void addTileMatrix(
      String identifier, int minTileRow, int maxTileRow, int minTileCol, int maxTileCol) {
    Pattern pattern = Pattern.compile("\\d+");
    String[] array =
        pattern.matcher(identifier).results().map(MatchResult::group).toArray(String[]::new);
    int zoomLevel = 0;
    if (array.length > 0) {
      zoomLevel = Integer.parseInt(array[array.length - 1]);
    }
    TileMatrix tileMatrix =
        new TileMatrix(identifier, zoomLevel, minTileRow, maxTileRow, minTileCol, maxTileCol);
    limits.add(tileMatrix);
  }
}
