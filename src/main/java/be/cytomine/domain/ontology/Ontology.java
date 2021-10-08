package be.cytomine.domain.ontology;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.utils.JsonObject;
import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Set;

@Entity
@Data
public class Ontology extends CytomineDomain {

    @NotNull
    @NotBlank
    @Column(nullable = false)
    protected String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    protected User user;

    @OneToMany(fetch = FetchType.LAZY, mappedBy="ontology")
//    @JoinColumn(referencedColumnName = "ontology_id", nullable = true)
    protected Set<Project> projects;

    @PrePersist
    public void beforeCreate() {
        super.beforeInsert();
    }

    @PreUpdate
    public void beforeUpdate() {
        super.beforeUpdate();
    }

    public CytomineDomain buildDomainFromJson(JsonObject json, EntityManager entityManager) {
        Ontology ontology = (Ontology)this;
        ontology.id = json.getJSONAttrLong("id",null);
        ontology.name = json.getJSONAttrStr("name");
        ontology.user = (User)json.getJSONAttrDomain(entityManager, "user", new User(), true);
        ontology.created = json.getJSONAttrDate("created");
        ontology.updated = json.getJSONAttrDate("updated");
        return ontology;
    }

    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject returnArray = CytomineDomain.getDataFromDomain(domain);
        Ontology ontology = (Ontology)domain;
        returnArray.put("name", ontology.getName());
        returnArray.put("user", (ontology.getUser()!=null ? ontology.getUser().getId() : null));
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
}
