package be.cytomine.domain.meta;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.exceptions.CytomineMethodNotYetImplementedException;
import be.cytomine.utils.JsonObject;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Entity
@Getter
@Setter
public class Property extends CytomineDomain {

    @NotNull
    @NotBlank
    private String key;

    @NotBlank
    private String value;

    @NotNull
    @NotBlank
    private String domainClassName;

    @NotNull
    private Long domainIdent;

    /**
     * Set annotation (storing class + id)
     * With groovy, you can do: this.annotation = ...
     * @param domain to add
     */
    public void setDomain(CytomineDomain domain) {
        domainClassName = domain.getClass().getName();
        domainIdent = domain.getId();
    }

    public CytomineDomain buildDomainFromJson(JsonObject json, EntityManager entityManager) {
        Property property = (Property)this;
        property.id = json.getJSONAttrLong("id",null);

        Long id = json.getJSONAttrLong("domainIdent",-1l);
        if (id == -1) {
            id = json.getJSONAttrLong("domain",-1l);
        }
        property.domainIdent = id;
        property.domainClassName = json.getJSONAttrStr("domainClassName", true);
        property.key = json.getJSONAttrStr("key", true);
        property.value = json.getJSONAttrStr("value", true);
        property.created = json.getJSONAttrDate("created");
        property.updated = json.getJSONAttrDate("updated");
        return property;
    }

    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject returnArray = CytomineDomain.getDataFromDomain(domain);
        Property property = (Property)domain;
        returnArray.put("domainIdent", property.getDomainIdent());
        returnArray.put("domainClassName", property.getDomainClassName());
        returnArray.put("key", property.getKey());
        returnArray.put("value", property.getValue());
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
        throw new CytomineMethodNotYetImplementedException("todo :/");
        //return retrieveCytomineDomain()?.container();

        //thow an exception in this method. Make these kind of objects implement PartialContainer interface
        // returning id/classname of parent. In ACL security, read the real container

        // return a object implementing an interface?
        // Problem: if class name = abstract image => we expect storage

        // throw an exception that could be catched to read the real domain?
    }
}
