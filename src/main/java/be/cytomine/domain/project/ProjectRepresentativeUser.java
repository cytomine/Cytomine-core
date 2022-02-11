package be.cytomine.domain.project;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.GenericCytomineDomainContainer;
import be.cytomine.domain.meta.Tag;
import be.cytomine.domain.meta.TagDomainAssociation;
import be.cytomine.domain.security.SecUser;
import be.cytomine.domain.security.User;
import be.cytomine.utils.JsonObject;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@Getter
@Setter
public class ProjectRepresentativeUser extends CytomineDomain {

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;


    public CytomineDomain buildDomainFromJson(JsonObject json, EntityManager entityManager) {
        ProjectRepresentativeUser projectRepresentativeUser = (ProjectRepresentativeUser)this;
        projectRepresentativeUser.setId(json.getJSONAttrLong("id",null));
        projectRepresentativeUser.setProject((Project) json.getJSONAttrDomain(entityManager, "project", new Project(), true));
        projectRepresentativeUser.setUser((User) json.getJSONAttrDomain(entityManager, "user", new SecUser(), true));
        return projectRepresentativeUser;
    }

    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject returnArray = CytomineDomain.getDataFromDomain(domain);
        ProjectRepresentativeUser projectRepresentativeUser = (ProjectRepresentativeUser)domain;
        returnArray.put("project", projectRepresentativeUser.getProject().getId());
        returnArray.put("user", projectRepresentativeUser.getUser().getId());
        return returnArray;
    }

    @Override
    public String toJSON() {
        return toJsonObject().toJsonString();
    }

    @Override
    public JsonObject toJsonObject() {
        return getDataFromDomain(this);
    }

    public CytomineDomain container() {
        return project;
    }
}