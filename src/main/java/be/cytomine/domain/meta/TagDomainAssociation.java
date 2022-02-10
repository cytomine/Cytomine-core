package be.cytomine.domain.meta;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.GenericCytomineDomainContainer;
import be.cytomine.utils.JsonObject;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Entity
@Getter
@Setter
public class TagDomainAssociation extends CytomineDomain {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tag_id")
    protected Tag tag;

    @NotNull
    @NotBlank
    private String domainClassName;

    @NotNull
    private Long domainIdent;

    public void setDomain(CytomineDomain domain) {
        domainClassName = domain.getClass().getName();
        domainIdent = domain.getId();
    }

    public CytomineDomain buildDomainFromJson(JsonObject json, EntityManager entityManager) {
        TagDomainAssociation tagDomainAssocitation = (TagDomainAssociation)this;
        tagDomainAssocitation.setId(json.getJSONAttrLong("id",null));
        tagDomainAssocitation.setDomainIdent(json.getJSONAttrLong("domainIdent",-1l));
        tagDomainAssocitation.setDomainClassName(json.getJSONAttrStr("domainClassName"));
        tagDomainAssocitation.setTag((Tag) json.getJSONAttrDomain(entityManager, "tag", new Tag(), true));
        return tagDomainAssocitation;
    }

    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject returnArray = CytomineDomain.getDataFromDomain(domain);
        TagDomainAssociation tagDomainAssocitation = (TagDomainAssociation)domain;
        returnArray.put("domainIdent", tagDomainAssocitation.getDomainIdent());
        returnArray.put("domainClassName", tagDomainAssocitation.getDomainClassName());
        returnArray.put("tag", tagDomainAssocitation.getTag().getId());
        returnArray.put("tagName", tagDomainAssocitation.getTag().getName());
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
        GenericCytomineDomainContainer genericCytomineDomainContainer = new GenericCytomineDomainContainer();
        genericCytomineDomainContainer.setId(domainIdent);
        genericCytomineDomainContainer.setContainerClass(domainClassName);
        return genericCytomineDomainContainer;
    }

}
