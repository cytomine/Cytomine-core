package be.cytomine.domain.image;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.utils.JsonObject;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;
import java.util.Optional;

@Entity
@Getter
@Setter
public class NestedImageInstance extends ImageInstance {

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    private ImageInstance parent;

    @NotNull
    private Integer x;

    @NotNull
    private Integer y;

    public CytomineDomain buildDomainFromJson(JsonObject json, EntityManager entityManager) {
        NestedImageInstance nestedImageInstance = (NestedImageInstance)super.buildDomainFromJson(this, json, entityManager);
        nestedImageInstance.parent = (ImageInstance)json.getJSONAttrDomain(entityManager, "parent", new ImageInstance(), true);
        nestedImageInstance.x = json.getJSONAttrInteger("x",0);
        nestedImageInstance.y = json.getJSONAttrInteger("y",0);
        return nestedImageInstance;
    }

    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject returnArray = ImageInstance.getDataFromDomain(domain);
        NestedImageInstance nestedImageInstance = (NestedImageInstance) domain;
        returnArray.put("parent", nestedImageInstance.getParentId());
        returnArray.put("x", nestedImageInstance.getX());
        returnArray.put("y", nestedImageInstance.getY());
        return returnArray;
    }

    public Long getParentId() {
        return Optional.ofNullable(parent).map(CytomineDomain::getId).orElse(null);
    }

    @Override
    public JsonObject toJsonObject() {
        return getDataFromDomain(this);
    }

}
