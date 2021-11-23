package be.cytomine.utils;

import be.cytomine.service.dto.BoundariesCropParameter;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.io.WKTReader;

public class GeometryUtils {

    public static Geometry createBoundingBox(String bbox) throws ParseException {
        if(bbox.startsWith("POLYGON")) {
            return new WKTReader().read(bbox);
        }
        String[] coordinates = bbox.split(",");
        double minX = Double.parseDouble(coordinates[0]);
        double minY = Double.parseDouble(coordinates[1]);
        double maxX = Double.parseDouble(coordinates[2]);
        double maxY = Double.parseDouble(coordinates[3]);
        return GeometryUtils.createBoundingBox(minX, maxX, minY , maxY);
    }

    public static Geometry createBoundingBox(double minX, double maxX, double minY, double maxY) {
        Coordinate[] roiPoints = new Coordinate[5];
        roiPoints[0] = new Coordinate(minX, minY);
        roiPoints[1] = new Coordinate(minX, maxY);
        roiPoints[2] = new Coordinate(maxX, maxY);
        roiPoints[3] = new Coordinate(maxX, minY);
        roiPoints[4] = roiPoints[0];
        //Build geometry
        LinearRing linearRing = new GeometryFactory().createLinearRing(roiPoints);
        return new GeometryFactory().createPolygon(linearRing);
    }

    public static BoundariesCropParameter getGeometryBoundaries(Geometry geometry) {
        if (geometry.getNumPoints() > 1) {
            Envelope env = geometry.getEnvelopeInternal();
            BoundariesCropParameter cropParameter = new BoundariesCropParameter();
            cropParameter.setTopLeftX((int)Math.round(env.getMinX()));
            cropParameter.setTopLeftY((int)Math.round(env.getMaxY()));
            cropParameter.setWidth((int)env.getWidth());
            cropParameter.setHeight((int)env.getHeight());
            return cropParameter;
        } else if (geometry.getNumPoints() == 1) {
            Envelope env = geometry.getEnvelopeInternal();
            BoundariesCropParameter cropParameter = new BoundariesCropParameter();
            cropParameter.setTopLeftX((int)Math.round(env.getMinX() - 50));
            cropParameter.setTopLeftY((int)Math.round(env.getMaxY() + 50));
            cropParameter.setWidth(100);
            cropParameter.setHeight(100);
            return cropParameter;
        }
        throw new IllegalArgumentException("Cannot extract boundaries for " + geometry);
    }
}
