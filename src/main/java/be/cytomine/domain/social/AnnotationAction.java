package be.cytomine.domain.social;

import be.cytomine.domain.CytomineSocialDomain;
import be.cytomine.utils.DateUtils;
import be.cytomine.utils.JsonObject;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.persistence.*;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Info on user connection for a project
 * ex : User x connect to project y the 2013/01/01 at time y
 */
@Getter
@Setter
@Document
public class AnnotationAction extends CytomineSocialDomain implements Cloneable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    protected Long id;

    protected Date created;

    Long user;

    Long image;

    Long slice;

    Long project;

    String annotationClassName;

    Long annotationIdent;

    Long annotationCreator;

    String action;

    public static JsonObject getDataFromDomain(AnnotationAction domain) {
        JsonObject returnArray = new JsonObject();
        AnnotationAction connection = (AnnotationAction)domain;
        returnArray.put("class", domain.getClass());
        returnArray.put("id", domain.getId());
        returnArray.put("created", DateUtils.getTimeToString(domain.created));

        returnArray.put("user", connection.getUser());
        returnArray.put("image", connection.getImage());
        returnArray.put("slice", connection.getSlice());
        returnArray.put("project", connection.getProject());
        returnArray.put("action", connection.getAction());

        returnArray.put("annotationIdent", connection.getAnnotationIdent());
        returnArray.put("annotationClassName", connection.getAnnotationClassName());
        returnArray.put("annotationCreator", connection.getAnnotationCreator());

        return returnArray;
    }

    @Override
    public JsonObject toJsonObject() {
        return getDataFromDomain(this);
    }

    @Override
    public String toString() {
        return "AnnotationAction{" +
                "id=" + id +
                ", created=" + created +
                ", user=" + user +
                ", image=" + image +
                ", slice=" + slice +
                ", annotationIdent=" + annotationIdent +
                ", action='" + action + '\'' +
                '}';
    }
}
