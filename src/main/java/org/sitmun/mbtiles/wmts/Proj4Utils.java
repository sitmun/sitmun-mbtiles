package org.sitmun.mbtiles.wmts;

import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.CoordinateTransform;
import org.locationtech.proj4j.CoordinateTransformFactory;
import org.locationtech.proj4j.ProjCoordinate;

public class Proj4Utils {

  private Proj4Utils() {
    // Prevent instantiation
  }

  public static double[] transformExtent(double[] extent, String srsOrig, String srsDest) {
    CRSFactory factory = new CRSFactory();
    CoordinateReferenceSystem src = factory.createFromName(srsOrig);
    CoordinateReferenceSystem dst = factory.createFromName(srsDest);

    CoordinateTransformFactory ctFactory = new CoordinateTransformFactory();
    CoordinateTransform transform = ctFactory.createTransform(src, dst);

    double[] min = {extent[0], extent[1]};
    double[] max = {extent[2], extent[3]};

    double[] minTrans = transform(min, transform);
    double[] maxTrans = transform(max, transform);

    return new double[] {minTrans[0], minTrans[1], maxTrans[0], maxTrans[1]};
  }

  private static double[] transform(double[] coords, CoordinateTransform transform) {
    ProjCoordinate srcCoord = new ProjCoordinate(coords[0], coords[1]);
    ProjCoordinate dstCoord = new ProjCoordinate();
    transform.transform(srcCoord, dstCoord);

    return new double[] {dstCoord.x, dstCoord.y};
  }
}
