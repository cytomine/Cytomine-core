package be.cytomine.service.utils;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.precision.GeometryPrecisionReducer;
import com.vividsolutions.jts.simplify.TopologyPreservingSimplifier;
import org.springframework.stereotype.Service;

@Service
public class SimplifyGeometryService {

    public Geometry simplifyPolygonForCrop(Geometry geometry) {
        if (geometry.getNumPoints() < 100) {
            return reduceGeometryPrecision(geometry);
        }

        double index = 2d;
        int max = 1000;
        while (index < max) {
            geometry = TopologyPreservingSimplifier.simplify(geometry, index);
            if (geometry.getNumPoints() < 150) {
                break;
            }
            index = (index + 5) * 1.1;
        }

        return reduceGeometryPrecision(geometry);
    }

    public Geometry reduceGeometryPrecision(Geometry geometry) {
        return reduceGeometryPrecision(geometry, 100);
    }

    public Geometry reduceGeometryPrecision(Geometry geometry, int scale) {
        GeometryPrecisionReducer reducer = new GeometryPrecisionReducer(new PrecisionModel(scale));
        return reducer.reduce(geometry);
    }


}
