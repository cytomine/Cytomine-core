package be.cytomine.domain.meta;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.security.SecUser;
import be.cytomine.domain.security.User;
import be.cytomine.utils.JsonObject;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Entity
@Getter
@Setter
public class Tag extends CytomineDomain {

    @NotNull
    @NotBlank
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    protected User user;


    public CytomineDomain buildDomainFromJson(JsonObject json, EntityManager entityManager) {
        Tag tag = (Tag)this;
        tag.setId(json.getJSONAttrLong("id",null));
        tag.setName(json.getJSONAttrStr("name", true));
        tag.setUser((User) json.getJSONAttrDomain(entityManager, "user", new User(), true));
        return tag;
    }

    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject returnArray = CytomineDomain.getDataFromDomain(domain);
        Tag tag = (Tag)domain;
        returnArray.put("name", tag.getName());
        returnArray.put("user", tag.getUser().getId());
        returnArray.put("creatorName", tag.getUser().getUsername());
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

    @Override
    public SecUser userDomainCreator() {
        return user;
    }

}
