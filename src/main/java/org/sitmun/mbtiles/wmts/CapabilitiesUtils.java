package org.sitmun.mbtiles.wmts;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Document;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.jetbrains.annotations.NotNull;
import org.sitmun.mbtiles.dto.TileServiceDto;
import org.sitmun.mbtiles.service.MBTilesUnexpectedRequestException;

@Slf4j
public class CapabilitiesUtils {

  private CapabilitiesUtils() {
    // Private constructor to prevent instantiation
  }

  public static Capabilities parseWMTSCapabilities(TileServiceDto service) {
    try {
      String capabilitiesUrl =
          service.getUrl() + "?SERVICE=" + service.getType() + "&REQUEST=GetCapabilities";
      SAXReader reader = new SAXReader();
      reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
      reader.setFeature("http://xml.org/sax/features/external-general-entities", false);
      reader.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
      Document doc = reader.read(new URL(capabilitiesUrl).openStream());
      List<String> layers = service.getLayers();
      List<LayerCapabilities> layerCapabilities = new ArrayList<>();

      List<Node> capLayers = doc.selectNodes("//*[local-name()='Layer']");
      Capabilities capabilities = new Capabilities();
      capabilities.setTileMatrixSet(service.getMatrixSet());

      processLayers(capLayers, layers, layerCapabilities, capabilities);
      orderLayers(layers, layerCapabilities, capabilities);
      validate(layerCapabilities);

      return capabilities;
    } catch (Exception e) {
      log.error("Error parsing WMTS capabilities", e);
      throw new MBTilesUnexpectedRequestException();
    }
  }

  private static void validate(List<LayerCapabilities> layerCapabilities) {
    for (LayerCapabilities lc : layerCapabilities) {
      if (lc.getLimits() == null || lc.getLimits().isEmpty()) {
        throw new MBTilesUnexpectedRequestException();
      }
    }
  }

  private static void orderLayers(
      List<String> layers, List<LayerCapabilities> layerCapabilities, Capabilities capabilities) {
    for (String layerName : layers) {
      for (LayerCapabilities lc : layerCapabilities) {
        if (lc.getLayerIdentifier().equals(layerName)) {
          capabilities.addLayer(lc);
          break;
        }
      }
    }
  }

  private static void processLayers(
      List<Node> capLayers,
      List<String> layers,
      List<LayerCapabilities> layerCapabilities,
      Capabilities capabilities) {
    for (Node capLayer : capLayers) {
      Node identifier = capLayer.selectSingleNode("*[local-name()='Identifier']");
      if (identifier == null) {
        continue;
      }
      String name = identifier.getStringValue();
      if (layers.contains(name)) {
        layerCapabilities.add(
            createWMTSLayerCapabilities(name, capLayer, capabilities.getTileMatrixSet()));
      }
    }
  }

  private static LayerCapabilities createWMTSLayerCapabilities(
      String identifier, Node layer, String tileMatrixSet) {
    LayerCapabilities layerCapabilities = extractLayerCapabilities(identifier, layer);
    List<Node> tileMatrixSetLinks = layer.selectNodes("*[local-name()='TileMatrixSetLink']");
    for (Node matrixSet : tileMatrixSetLinks) {
      String matrixIdentifier =
          matrixSet.selectSingleNode("*[local-name()='TileMatrixSet']").getStringValue();
      if (matrixIdentifier.equals(tileMatrixSet)) {
        List<Node> limits = matrixSet.selectNodes("descendant::*[local-name()='TileMatrixLimits']");
        addLimitsToWMTSLayerCapabilities(layerCapabilities, limits);
      }
    }

    return layerCapabilities;
  }

  @NotNull
  private static LayerCapabilities extractLayerCapabilities(String identifier, Node layer) {
    LayerCapabilities layerCapabilities = new LayerCapabilities();
    layerCapabilities.setLayerIdentifier(identifier);
    Node lowerCornerElements = layer.selectSingleNode("descendant::*[local-name()='LowerCorner']");
    Node upperCornerElements = layer.selectSingleNode("descendant::*[local-name()='UpperCorner']");
    if (lowerCornerElements != null && upperCornerElements != null) {
      String[] lowerCorner = lowerCornerElements.getStringValue().split(" ");
      String[] upperCorner = upperCornerElements.getStringValue().split(" ");
      layerCapabilities.setMinLon(Double.parseDouble(lowerCorner[0]));
      layerCapabilities.setMinLat(Double.parseDouble(lowerCorner[1]));
      layerCapabilities.setMaxLon(Double.parseDouble(upperCorner[0]));
      layerCapabilities.setMaxLat(Double.parseDouble(upperCorner[1]));
    }
    return layerCapabilities;
  }

  private static void addLimitsToWMTSLayerCapabilities(
      LayerCapabilities layerCapabilities, List<Node> limits) {
    for (Node limit : limits) {
      String tileMatrix = limit.selectSingleNode("*[local-name()='TileMatrix']").getStringValue();
      String minTileRow = limit.selectSingleNode("*[local-name()='MinTileRow']").getStringValue();
      String maxTileRow = limit.selectSingleNode("*[local-name()='MaxTileRow']").getStringValue();
      String minTileCol = limit.selectSingleNode("*[local-name()='MinTileCol']").getStringValue();
      String maxTileCol = limit.selectSingleNode("*[local-name()='MaxTileCol']").getStringValue();
      layerCapabilities.addTileMatrix(
          tileMatrix,
          Integer.parseInt(minTileRow),
          Integer.parseInt(maxTileRow),
          Integer.parseInt(minTileCol),
          Integer.parseInt(maxTileCol));
    }
  }
}
