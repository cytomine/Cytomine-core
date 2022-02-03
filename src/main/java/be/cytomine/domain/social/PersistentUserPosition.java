package be.cytomine.domain.social;

import be.cytomine.domain.CytomineSocialDomain;
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.image.SliceInstance;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.SecUser;
import be.cytomine.domain.security.User;
import be.cytomine.service.dto.Point;
import be.cytomine.utils.DateUtils;
import be.cytomine.utils.JsonObject;
import com.vividsolutions.jts.geom.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.geo.GeoJsonPolygon;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

//@Entity
@Getter
@Setter
@Document
//@CompoundIndex(def = "{'user' : 1, 'image': 1, 'slice': 1, created' : -1}")
//@CompoundIndex(def = "{'location':'2d', 'image':1, 'slice':1}")
//@Embeddable
public class PersistentUserPosition extends CytomineSocialDomain {


   // TODO:
   // stateless true //don't store data in memory after read&co. These data don't need to be update.

    //TODO:
    // indexAttributes:[min:Integer.MIN_VALUE, max:Integer.MAX_VALUE],

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    protected Long id;

    @CreatedDate
    protected Date created;

    @LastModifiedDate
    protected Date updated;

//    @NotNull
//    @ManyToOne(fetch = FetchType.LAZY)
//    protected SecUser user;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(nullable = true)
//    @Indexed
//    private ImageInstance image;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(nullable = true)
//    private SliceInstance slice;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(nullable = true)
//    private Project project;

    protected Long user;

    private Long image;

    private Long slice;

    private Long project;

    private String session;

    /**
     * User screen area
     */
    @ElementCollection
    List<List<Double>> location;

    /**
     * User zoom on image
     */
    int zoom;

    Double  rotation;

    /**
     * Whether or not the user has decided to broadcast its position
     */
    boolean broadcast;


    private String imageName;

    public Long computeDateInMillis() {
        return created != null ? created.getTime() - new Date(0).getTime() : null;
    }

    public static JsonObject getDataFromDomain(PersistentUserPosition domain) {
        JsonObject returnArray = new JsonObject();
        PersistentUserPosition position = (PersistentUserPosition)domain;
        returnArray.put("class", domain.getClass());
        returnArray.put("id", domain.getId());
        returnArray.put("created", DateUtils.getTimeToString(domain.created));
        returnArray.put("updated", DateUtils.getTimeToString(domain.updated));
        returnArray.put("user", position.getUser()!=null? position.getUser() : null);
        returnArray.put("image", position.getImage()!=null? position.getImage() : null);
        returnArray.put("slice", position.getSlice()!=null? position.getSlice() : null);
        returnArray.put("project", position.getProject()!=null? position.getProject() : null);
        returnArray.put("zoom", position.getZoom());
        returnArray.put("rotation", position.getRotation());
        returnArray.put("broadcast", position.isBroadcast());
        Polygon polygon = getJtsPolygon(domain.location);
        returnArray.put("location", polygon.toString());
        returnArray.put("x", polygon.getCentroid().getX());
        returnArray.put("y", polygon.getCentroid().getY());
        return returnArray;
    }
//    static Polygon getPolygonFromMongo(List locationList) {
//        GeometryFactory fact = new GeometryFactory();
//        locationList.add(locationList.get(0));
//        List<Coordinate> coordinates = (List<Coordinate>) locationList.stream().map(x -> new Coordinate((Double)((List)x).get(0),(Double)((List)x).get(1))).collect(Collectors.toList());
//        LinearRing linear = new GeometryFactory().createLinearRing(coordinates.toArray(new Coordinate[coordinates.size()]));
//        Polygon poly = new Polygon(linear, null, fact);
//        return poly;
//    }
    public static Polygon getJtsPolygon(GeoJsonPolygon polygon) {
        List<Coordinate> coordinates = polygon.getPoints().stream().map(point -> new Coordinate(point.getX(), point.getY())).collect(Collectors.toList());
        if (coordinates.size() > 1) {
            // finish with the first point
            coordinates.add(coordinates.get(0));
        }
        return new GeometryFactory().createPolygon(coordinates.toArray(Coordinate[]::new));
    }

    public static Polygon getJtsPolygon(List<List<Double>> polygon) {
        List<Coordinate> coordinates = polygon.stream().map(point -> new Coordinate(point.get(0), point.get(1))).collect(Collectors.toList());
        if (coordinates.size() > 1) {
            // finish with the first point
            coordinates.add(coordinates.get(0));
        }
        return new GeometryFactory().createPolygon(coordinates.toArray(Coordinate[]::new));
    }

    @Override
    public JsonObject toJsonObject() {
        return getDataFromDomain(this);
    }
}
