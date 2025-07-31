package org.sitmun.mbtiles.wmts;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Capabilities {
  String tileMatrixSet;
  List<LayerCapabilities> layers = new ArrayList<>();

  public void addLayer(LayerCapabilities layerCapabilities) {
    layers.add(layerCapabilities);
  }
}
