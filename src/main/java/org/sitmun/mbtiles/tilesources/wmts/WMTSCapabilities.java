package org.sitmun.mbtiles.tilesources.wmts;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import lombok.With;

@Value
@Builder
@With
public class WMTSCapabilities {
  String tileMatrixSet;
  @Builder.Default List<WMTSLayerCapabilities> layers = new ArrayList<>();

  public WMTSCapabilities withLayer(WMTSLayerCapabilities lc) {
    List<WMTSLayerCapabilities> newLayers = new ArrayList<>(this.layers);
    newLayers.add(lc);
    return WMTSCapabilities.builder().tileMatrixSet(this.tileMatrixSet).layers(newLayers).build();
  }
}
