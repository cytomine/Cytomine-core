package be.cytomine.domain.social;

import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.image.SliceInstance;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.SecUser;
import be.cytomine.utils.DateUtils;
import be.cytomine.utils.JsonObject;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.geo.GeoJsonPolygon;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static be.cytomine.domain.social.PersistentUserPosition.getJtsPolygon;

@Entity
@Getter
@Setter
@Document
//@CompoundIndex(def = "{'user' : 1, 'image': 1, 'slice': 1, 'created' : -1}")
//@CompoundIndex(def = "{'location':'2d', 'image':1, 'slice':1}")
//@CompoundIndex(def = "{'created':1, 'expireAfterSeconds': 60}")
public class LastUserPosition {


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

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    protected SecUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = true)
    @Indexed
    private ImageInstance image;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = true)
    private SliceInstance slice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = true)
    private Project project;

    private String session;

    private String imageName;

    /**
     * User screen area
     */


    GeoJsonPolygon location;

    /**
     * User zoom on image
     */
    int zoom;

    Double  rotation;

    /**
     * Whether or not the user has decided to broadcast its position
     */
    boolean broadcast;


    public Long computeDateInMillis() {
        return created != null ? created.getTime() - new Date(0).getTime() : null;
    }

    public static JsonObject getDataFromDomain(LastUserPosition domain) {
        JsonObject returnArray = new JsonObject();
        LastUserPosition position = (LastUserPosition)domain;
        returnArray.put("class", domain.getClass());
        returnArray.put("id", domain.getId());
        returnArray.put("created", DateUtils.getTimeToString(domain.created));
        returnArray.put("updated", DateUtils.getTimeToString(domain.updated));
        returnArray.put("user", position.getUser()!=null? position.getUser().getId() : null);
        returnArray.put("image", position.getImage()!=null? position.getImage().getId() : null);
        returnArray.put("slice", position.getSlice()!=null? position.getSlice().getId() : null);
        returnArray.put("project", position.getProject()!=null? position.getProject().getId() : null);
        returnArray.put("zoom", position.getZoom());
        returnArray.put("rotation", position.getRotation());
        returnArray.put("broadcast", position.isBroadcast());
        Polygon polygon = getJtsPolygon(domain.location);
        returnArray.put("location", polygon.toString());
        returnArray.put("x", polygon.getCentroid().getX());
        returnArray.put("y", polygon.getCentroid().getY());
        return returnArray;
    }

}
