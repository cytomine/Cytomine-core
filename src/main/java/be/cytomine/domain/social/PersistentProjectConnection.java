package be.cytomine.domain.social;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.ontology.Ontology;
import be.cytomine.domain.ontology.Term;
import be.cytomine.domain.security.User;
import be.cytomine.utils.DateUtils;
import be.cytomine.utils.JsonObject;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
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
@Entity
@CompoundIndex(def = "{'project' : 1, 'created' : -1}")
public class PersistentProjectConnection  implements Cloneable {

        // TODO:
//    version false
//    stateless true //don't store data in memory after read&co. These data don't need to be update.



    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    protected String id;

    protected Date created;

    @Transient
    protected Date updated;


    @NotNull
    Long user;

    @NotNull
    @Indexed
    Long project;

    Long time;

    String session;

    String os;

    String browser;

    String browserVersion;

    Integer countViewedImages;

    Integer countCreatedAnnotations;

    @Transient
    Map<String, Object> extraProperties = new LinkedHashMap<>();

    public static JsonObject getDataFromDomain(PersistentProjectConnection domain) {
        JsonObject returnArray = new JsonObject();
        PersistentProjectConnection connection = (PersistentProjectConnection)domain;
        returnArray.put("class", domain.getClass());
        returnArray.put("id", domain.getId());
        returnArray.put("created", DateUtils.getTimeToString(domain.created));
        returnArray.put("updated", DateUtils.getTimeToString(domain.updated));
        returnArray.put("user", connection.getUser());
        returnArray.put("project", connection.getProject());
        returnArray.put("time", connection.getTime());
        returnArray.put("os", connection.getOs());
        returnArray.put("browser", connection.getBrowser());
        returnArray.put("browserVersion", connection.getBrowserVersion());
        returnArray.put("countViewedImages", connection.getCountViewedImages());
        returnArray.put("countCreatedAnnotations", connection.getCountCreatedAnnotations());
        returnArray.putAll(connection.getExtraProperties());
        return returnArray;
    }

    @Override
    public Object clone() {
        PersistentProjectConnection result = new PersistentProjectConnection();
        result.user = user;
        result.project = project;
        result.time = time;
        result.os = os;
        result.browser = browser;
        result.browserVersion = browserVersion;
        result.countViewedImages = countViewedImages;
        result.countCreatedAnnotations = countCreatedAnnotations;
        result.id = id;
        result.created = created;
        return result;
    }

    public void propertyMissing(String name, Object value) {
        extraProperties.put(name, value);
    }


    public Long computeDateInMillis() {
        return created != null ? created.getTime() - new Date(0).getTime() : null;
    }

    @Override
    public String toString() {
        return "PersistentProjectConnection{" +
                "id='" + id + '\'' +
                ", created=" + created +
                ", createdTime=" + (created!=null? created.getTime() : null) +
                ", user=" + user +
                ", project=" + project +
                ", time=" + time +
                '}';
    }
}
