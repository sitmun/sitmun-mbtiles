package org.sitmun.mbtiles.wmts;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

@Slf4j
public class WMTSUtils {

  private WMTSUtils() {
    // Private constructor to prevent instantiation
  }

  private static final String TEMPLATE =
      "%s?SERVICE=WMTS&VERSION=1.0.0&REQUEST=GetTile&LAYER=%s&TILEMATRIXSET=%s&TILEMATRIX=%s&TILEROW=%d&TILECOL=%d&FORMAT=image/png";

  public static byte @Nullable [] downloadTile(
      String urlService, String layer, String matrixSet, int x, int y, String zoom) {
    String fullUrl = null;
    try {
      fullUrl = getFullUrl(urlService, layer, matrixSet, x, y, zoom);
      URL url = new URL(fullUrl);
      BufferedImage image = ImageIO.read(url);
      try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
        ImageIO.write(image, "png", baos);
        log.info(
            "Downloading tile at matrix {} in coordinates ({},{}) from url {}",
            zoom,
            x,
            y,
            fullUrl);
        return baos.toByteArray();
      }
    } catch (Exception e) {
      log.error(
          "Error downloading tile at matrix {} in coordinates ({},{}) from url {}",
          zoom,
          x,
          y,
          fullUrl);
    }
    return null;
  }

  private static String getFullUrl(
      String urlService, String layer, String matrixSet, int x, int y, String zoom) {
    return TEMPLATE.formatted(urlService, layer, matrixSet, zoom, y, x);
  }
}
