package be.cytomine.domain.ontology;

import be.cytomine.domain.security.User;
import be.cytomine.utils.JsonObject;
import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.io.Serializable;

@Entity
@Data
public class UserAnnotation extends AnnotationDomain implements Serializable {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Override
    public String toJSON() {
        return "annotation json";
    }

    @Override
    public JsonObject toJsonObject() {
        return JsonObject.of("id", id);
    }
}
