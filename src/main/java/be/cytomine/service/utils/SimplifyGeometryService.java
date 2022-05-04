package be.cytomine.service.utils;

/*
* Copyright (c) 2009-2022. Authors: see NOTICE file.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import be.cytomine.dto.SimplifiedAnnotation;
import be.cytomine.exceptions.WrongArgumentException;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.precision.GeometryPrecisionReducer;
import com.vividsolutions.jts.simplify.DouglasPeuckerSimplifier;
import com.vividsolutions.jts.simplify.TopologyPreservingSimplifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SimplifyGeometryService {

    @Value("${application.annotation.maxNumberOfPoint}")
    Double maxNumberOfPoint;

    public SimplifiedAnnotation simplifyPolygon(String form, Long minPoint, Long maxPoint) {
        try {
            return simplifyPolygon(new WKTReader().read(form), minPoint, maxPoint);
        } catch (ParseException e) {
            throw new WrongArgumentException("Annotation cannot be parsed "+ form);
        }
    }

    /**
     * Simplify form (limit point number)
     * Return simplify polygon and the rate used for simplification
     */
    public SimplifiedAnnotation simplifyPolygon(Geometry geometry, Long minPoint, Long maxPoint) {
        // Fast response for simple geometries
        if (geometry.getNumPoints() < 100)
            return new SimplifiedAnnotation(geometry, 0.0d);

        int numOfGeometry = 0;
        if (geometry instanceof MultiPolygon) {
            for (int i = 0; i < geometry.getNumGeometries(); i++) {
                Geometry geom = geometry.getGeometryN(i);
                int nbInteriorRing = 1;
                if(geom instanceof Polygon) {
                    nbInteriorRing = ((Polygon)geom).getNumInteriorRing();
                }
                numOfGeometry +=  geom.getNumGeometries() * nbInteriorRing;
            }
        } else {
            int nbInteriorRing = 1;
            if(geometry instanceof Polygon)
                nbInteriorRing = ((Polygon)geometry).getNumInteriorRing();
            numOfGeometry = geometry.getNumGeometries() * nbInteriorRing;
        }
        numOfGeometry = Math.max(1, numOfGeometry);

        if (numOfGeometry > 10) {
            numOfGeometry = numOfGeometry / 2;
        }
        numOfGeometry = Math.min(10, numOfGeometry);

        double ratioMax = 1.3d;
        double ratioMin = 1.7d;
        Double maxNumberOfPoint = this.maxNumberOfPoint;
        Double minNumberOfPoint = 100.0d;

        double numberOfPoint = geometry.getNumPoints();

        /* Maximum number of point that we would have (500/5 (max 150)=max 100 points)*/
        double rateLimitMax;
        if (maxPoint!=null && maxPoint!=0) {
            rateLimitMax = maxPoint * numOfGeometry;
        } else {
            rateLimitMax = Math.max(numberOfPoint / ratioMax, numOfGeometry * maxNumberOfPoint);
        }

        /* Minimum number of point that we would have (500/10 (min 10 max 100)=min 50 points)*/
        double rateLimitMin;
        if (minPoint!=null) {
            rateLimitMin = minPoint * numOfGeometry;
        } else {
            rateLimitMin = Math.min(Math.max(numberOfPoint / ratioMin, 10), numOfGeometry * minNumberOfPoint);
        }

        /* Increase value for the increment (allow to converge faster) */
        double incrThreshold = 0.25d;
        double i = 0;
        double rate = 0;

        /* Max number of loop (prevent infinite loop) */
        int maxLoop = 1000;

        Geometry newGeometry = (Geometry)geometry.clone();
        Boolean isPolygonAndNotValid = (geometry instanceof Polygon && !((Polygon) geometry).isValid());
        Boolean isMultiPolygon = (geometry instanceof MultiPolygon);
        while (numberOfPoint > rateLimitMax && maxLoop > 0) {
            rate = i;
            if (isPolygonAndNotValid || isMultiPolygon) {
                newGeometry = TopologyPreservingSimplifier.simplify(geometry, rate);
            } else {
                newGeometry = DouglasPeuckerSimplifier.simplify(geometry, rate);
            }

            if (newGeometry.getNumPoints() < rateLimitMin)
                break;

            i = i + ((incrThreshold));
            numberOfPoint = newGeometry.getNumPoints();
            maxLoop--;
        }
        return new SimplifiedAnnotation(newGeometry, rate);
    }


    public SimplifiedAnnotation simplifyPolygon(String form, double rate) {
        try {
            return simplifyPolygon(new WKTReader().read(form), rate);
        } catch (ParseException e) {
            throw new WrongArgumentException("Annotation cannot be parsed "+ form);
        }
    }

    public SimplifiedAnnotation simplifyPolygon(Geometry geometry, double rate) {
        Geometry newGeometry;
        Boolean isPolygonAndNotValid = (geometry instanceof Polygon && !((Polygon) geometry).isValid());
        Boolean isMultiPolygon = (geometry instanceof MultiPolygon);
        if (isPolygonAndNotValid || isMultiPolygon) {
            newGeometry = TopologyPreservingSimplifier.simplify(geometry, rate);
        } else {
            newGeometry = DouglasPeuckerSimplifier.simplify(geometry, rate);;
        }
        return new SimplifiedAnnotation(newGeometry, rate);
    }


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
