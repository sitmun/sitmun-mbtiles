package org.sitmun.mbtiles.tilesources.wmts;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import lombok.*;

@Builder
@Value
@With
public class WMTSLayerCapabilities {
  String layerIdentifier;
  @Builder.Default List<WMTSTileMatrix> limits = new ArrayList<>();
  Double minLon;
  Double minLat;
  Double maxLon;
  Double maxLat;

  public WMTSTileMatrix getLimitsByMatrix(int id) {
    return limits.stream().filter(l -> l.getZoomLevel() == id).findFirst().orElse(null);
  }

  public WMTSLayerCapabilities withTileMatrix(
      String identifier, int minTileRow, int maxTileRow, int minTileCol, int maxTileCol) {
    Pattern pattern = Pattern.compile("\\d+");
    String[] array =
        pattern.matcher(identifier).results().map(MatchResult::group).toArray(String[]::new);
    int zoomLevel = 0;
    if (array.length > 0) {
      zoomLevel = Integer.parseInt(array[array.length - 1]);
    }
    WMTSTileMatrix tileMatrix =
        WMTSTileMatrix.builder()
            .matrix(identifier)
            .zoomLevel(zoomLevel)
            .minRow(minTileRow)
            .maxRow(maxTileRow)
            .minCol(minTileCol)
            .maxCol(maxTileCol)
            .build();
    List<WMTSTileMatrix> newLimits = new ArrayList<>(this.limits);
    newLimits.add(tileMatrix);
    return WMTSLayerCapabilities.builder()
        .layerIdentifier(layerIdentifier)
        .minLon(minLon)
        .minLat(minLat)
        .maxLon(maxLon)
        .maxLat(maxLat)
        .limits(newLimits)
        .build();
  }
}
