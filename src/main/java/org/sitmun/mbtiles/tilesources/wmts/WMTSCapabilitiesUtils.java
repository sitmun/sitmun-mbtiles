package org.sitmun.mbtiles.tilesources.wmts;

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
public class WMTSCapabilitiesUtils {

  private WMTSCapabilitiesUtils() {
    // Private constructor to prevent instantiation
  }

  public static WMTSCapabilities parseWMTSCapabilities(TileServiceDto service) {
    try {
      String capabilitiesUrl =
          service.getUrl() + "?SERVICE=" + service.getType() + "&REQUEST=GetCapabilities";
      SAXReader reader = new SAXReader();
      reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
      reader.setFeature("http://xml.org/sax/features/external-general-entities", false);
      reader.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
      Document doc = reader.read(new URL(capabilitiesUrl).openStream());
      List<String> layers = service.getLayers();
      List<WMTSLayerCapabilities> layerCapabilities = new ArrayList<>();

      List<Node> capLayers = doc.selectNodes("//*[local-name()='Layer']");
      WMTSCapabilities capabilities =
          WMTSCapabilities.builder().tileMatrixSet(service.getMatrixSet()).build();

      processLayers(capLayers, layers, layerCapabilities, capabilities);
      validate(layerCapabilities);

      return orderLayers(capabilities, layers, layerCapabilities);
    } catch (Exception e) {
      log.error("Error parsing WMTS capabilities", e);
      throw new WMTSCapabilitiesException(e);
    }
  }

  private static void validate(List<WMTSLayerCapabilities> layerCapabilities) {
    for (WMTSLayerCapabilities lc : layerCapabilities) {
      if (lc.getLimits() == null || lc.getLimits().isEmpty()) {
        throw new MBTilesUnexpectedRequestException();
      }
    }
  }

  private static WMTSCapabilities orderLayers(
      WMTSCapabilities capabilities,
      List<String> layers,
      List<WMTSLayerCapabilities> layerCapabilities) {
    for (String layerName : layers) {
      for (WMTSLayerCapabilities lc : layerCapabilities) {
        if (lc.getLayerIdentifier().equals(layerName)) {
          capabilities = capabilities.withLayer(lc);
          break;
        }
      }
    }
    return capabilities;
  }

  private static void processLayers(
      List<Node> capLayers,
      List<String> layers,
      List<WMTSLayerCapabilities> layerCapabilities,
      WMTSCapabilities capabilities) {
    for (Node capLayer : capLayers) {
      Node identifier = capLayer.selectSingleNode("*[local-name()='Identifier']");
      if (identifier == null) {
        continue;
      }
      String name = identifier.getStringValue();
      if (layers.contains(name)) {
        WMTSLayerCapabilities wmtsLayerCapabilities =
            createWMTSLayerCapabilities(name, capLayer, capabilities.getTileMatrixSet());
        if (wmtsLayerCapabilities != null) {
          layerCapabilities.add(wmtsLayerCapabilities);
        }
      }
    }
  }

  private static WMTSLayerCapabilities createWMTSLayerCapabilities(
      String identifier, Node layer, String tileMatrixSet) {
    WMTSLayerCapabilities layerCapabilities = extractLayerCapabilities(identifier, layer);
    List<Node> tileMatrixSetLinks = layer.selectNodes("*[local-name()='TileMatrixSetLink']");
    for (Node matrixSet : tileMatrixSetLinks) {
      String matrixIdentifier =
          matrixSet.selectSingleNode("*[local-name()='TileMatrixSet']").getStringValue();
      if (matrixIdentifier.equals(tileMatrixSet)) {
        List<Node> limits = matrixSet.selectNodes("descendant::*[local-name()='TileMatrixLimits']");
        layerCapabilities = addLimitsToWMTSLayerCapabilities(layerCapabilities, limits);
      }
    }

    return layerCapabilities;
  }

  private static @NotNull WMTSLayerCapabilities extractLayerCapabilities(
      String identifier, Node layer) {
    Node lowerCornerElements = layer.selectSingleNode("descendant::*[local-name()='LowerCorner']");
    Node upperCornerElements = layer.selectSingleNode("descendant::*[local-name()='UpperCorner']");
    if (lowerCornerElements != null && upperCornerElements != null) {
      String[] lowerCorner = lowerCornerElements.getStringValue().split(" ");
      String[] upperCorner = upperCornerElements.getStringValue().split(" ");
      return WMTSLayerCapabilities.builder()
          .layerIdentifier(identifier)
          .minLon(Double.parseDouble(lowerCorner[0]))
          .minLat(Double.parseDouble(lowerCorner[1]))
          .maxLon(Double.parseDouble(upperCorner[0]))
          .maxLat(Double.parseDouble(upperCorner[1]))
          .build();
    }
    return WMTSLayerCapabilities.builder()
        .layerIdentifier(identifier)
        .minLon(-180.0)
        .minLat(-90.0)
        .maxLon(180.0)
        .maxLat(90.0)
        .build();
  }

  private static WMTSLayerCapabilities addLimitsToWMTSLayerCapabilities(
      WMTSLayerCapabilities layerCapabilities, List<Node> limits) {
    for (Node limit : limits) {
      String tileMatrix = limit.selectSingleNode("*[local-name()='TileMatrix']").getStringValue();
      String minTileRow = limit.selectSingleNode("*[local-name()='MinTileRow']").getStringValue();
      String maxTileRow = limit.selectSingleNode("*[local-name()='MaxTileRow']").getStringValue();
      String minTileCol = limit.selectSingleNode("*[local-name()='MinTileCol']").getStringValue();
      String maxTileCol = limit.selectSingleNode("*[local-name()='MaxTileCol']").getStringValue();
      layerCapabilities =
          layerCapabilities.withTileMatrix(
              tileMatrix,
              Integer.parseInt(minTileRow),
              Integer.parseInt(maxTileRow),
              Integer.parseInt(minTileCol),
              Integer.parseInt(maxTileCol));
    }
    return layerCapabilities;
  }
}
