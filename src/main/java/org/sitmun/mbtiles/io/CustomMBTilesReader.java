package org.sitmun.mbtiles.io;

import ch.poole.geo.mbtiles4j.MBTilesReadException;
import ch.poole.geo.mbtiles4j.MBTilesReader;
import ch.poole.geo.mbtiles4j.Tile;
import java.io.File;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class CustomMBTilesReader extends MBTilesReader {

  public CustomMBTilesReader(File f) throws MBTilesReadException {
    super(f);
  }

  @Override
  public Tile getTile(int zoom, int column, int row) throws MBTilesReadException {
    String sql =
        """
                SELECT tile_data
                FROM tiles
                WHERE zoom_level = %d AND tile_column = %d AND tile_row = %d
                """
            .formatted(zoom, column, row);

    try {
      InputStream tileDataStream;
      try (Statement statement = getConnection().createStatement()) {
        ResultSet resultSet = statement.executeQuery(sql);
        tileDataStream = resultSet.getBinaryStream("tile_data");
        resultSet.close();
      }
      if (tileDataStream == null) {
        return null; // No tile found
      }
      return new Tile(zoom, column, row, tileDataStream);
    } catch (SQLException e) {
      throw new MBTilesReadException(
          "Could not get Tile for z:%d, column:%d, row:%d".formatted(zoom, column, row), e);
    }
  }
}
