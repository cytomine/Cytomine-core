package be.cytomine.domain.ontology;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.project.Project;
import be.cytomine.utils.JsonObject;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Entity
@Getter
@Setter
public class Track extends CytomineDomain {

    @NotNull
    @NotBlank
    @Column(nullable = false)
    private String name;

    private String color;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_instance_id", nullable = true)
    private ImageInstance image;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = true)
    private Project project;
    
    
    public CytomineDomain buildDomainFromJson(JsonObject json, EntityManager entityManager) {
        Track track = this;
        track.id = json.getJSONAttrLong("id",null);
        track.name = json.getJSONAttrStr("name");
        track.color = json.getJSONAttrStr("color");

        track.image = (ImageInstance)json.getJSONAttrDomain(entityManager, "image", new ImageInstance(), true);
        track.project = (Project)json.getJSONAttrDomain(entityManager, "project", new Project(), true);

        track.created = json.getJSONAttrDate("created");
        track.updated = json.getJSONAttrDate("updated");
        return track;
    }

    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject returnArray = CytomineDomain.getDataFromDomain(domain);
        Track track = (Track)domain;
        returnArray.put("name", track.getName());
        returnArray.put("color", track.getColor());
        returnArray.put("image", track.getImage()!=null ? track.getImage().getId() : null);
        returnArray.put("project", track.getProject()!=null ? track.getProject().getId() : null);
        return returnArray;
    }

    @Override
    public String toJSON() {
        return getDataFromDomain(this).toJsonString();
    }

    @Override
    public JsonObject toJsonObject() {
        return getDataFromDomain(this);
    }

    public CytomineDomain container() {
        return image.container();
    }
}
