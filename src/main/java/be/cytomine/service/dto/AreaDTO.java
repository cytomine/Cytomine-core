package be.cytomine.service.dto;

import be.cytomine.domain.social.MongodbLocation;
import be.cytomine.utils.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.geo.GeoJsonPolygon;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AreaDTO {

    Point topLeft;
    Point topRight;
    Point bottomRight;
    Point bottomLeftX;

    public List<List<Double>> toList() {
        return List.of(topLeft.toList(), topRight.toList(), bottomRight.toList(), bottomLeftX.toList());
    }

    public List<Point> toPointList() {
        return List.of(topLeft, topRight, bottomRight, bottomLeftX);
    }

    public GeoJsonPolygon toGeoJsonPolygon() {
        return new GeoJsonPolygon(toPointList().stream()
                .map(point ->
                        new org.springframework.data.geo.Point(point.getX(), point.getY()))
                .collect(Collectors.toList()));
    }

    public MongodbLocation toMongodbLocation() {
        MongodbLocation location = new MongodbLocation();
        location.setType("polygon");
        location.setCoordinates(toPointList().stream().map(point -> List.of(point.getX(), point.getY())).collect(Collectors.toList()));
        return location;

//        return new GeoJsonPolygon(toPointList().stream()
//                .map(point ->
//                        new org.springframework.data.geo.Point(point.getX(), point.getY()))
//                .collect(Collectors.toList()));
    }
}
