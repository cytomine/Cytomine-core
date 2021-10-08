package be.cytomine.domain.ontology;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.project.Project;
import be.cytomine.utils.JsonObject;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class AnnotationDomain extends CytomineDomain implements Serializable {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "project_id", nullable = false)
    protected Project project;

    protected Double area;


    @Override
    public String toJSON() {
        return null;
    }

    @Override
    public JsonObject toJsonObject() {
        return null;
    }
}
