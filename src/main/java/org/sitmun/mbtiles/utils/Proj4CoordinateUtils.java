package org.sitmun.mbtiles.utils;

import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.CoordinateTransform;
import org.locationtech.proj4j.CoordinateTransformFactory;
import org.locationtech.proj4j.ProjCoordinate;

public class Proj4CoordinateUtils {

  private Proj4CoordinateUtils() {
    // Prevent instantiation
  }

  public static double[] transformExtent(double[] extent, String srsOrig, String srsDest) {
    CRSFactory factory = new CRSFactory();
    CoordinateReferenceSystem src = factory.createFromName(srsOrig);
    CoordinateReferenceSystem dst = factory.createFromName(srsDest);

    CoordinateTransformFactory ctFactory = new CoordinateTransformFactory();
    CoordinateTransform transform = ctFactory.createTransform(src, dst);

    double[] minCoordinates = {extent[0], extent[1]};
    double[] maxCoordinates = {extent[2], extent[3]};

    double[] transformedMin = transform(minCoordinates, transform);
    double[] transformedMax = transform(maxCoordinates, transform);

    return new double[] {
      transformedMin[0], transformedMin[1], transformedMax[0], transformedMax[1]
    };
  }

  private static double[] transform(double[] coordinates, CoordinateTransform transform) {
    ProjCoordinate sourceCoordinate = new ProjCoordinate(coordinates[0], coordinates[1]);
    ProjCoordinate destinationCoordinate = new ProjCoordinate();
    transform.transform(sourceCoordinate, destinationCoordinate);

    return new double[] {destinationCoordinate.x, destinationCoordinate.y};
  }
}
