package org.sitmun.mbtiles.tilesources.wmts;

public class WMTSConstants {

  private WMTSConstants() {
    // Prevent instantiation
  }

  public static final String WMTS_TYPE = "WMTS";

  public static final String MB_TILES_SRS = "EPSG:3857";
  public static final String CAPABILITIES_EXTENT_SRS = "EPSG:4326";

  public static final double ORIGIN_X_3857 = -20037508.3427892;
  public static final double ORIGIN_Y_3857 = 20037508.3427892;

  public static final double GLOBAL_SIZE_3857 = 2 * 20037508.3427892;
}
