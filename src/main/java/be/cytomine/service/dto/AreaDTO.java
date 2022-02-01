package be.cytomine.service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.geo.GeoJsonPolygon;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
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
}
