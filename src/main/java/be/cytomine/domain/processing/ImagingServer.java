package be.cytomine.domain.processing;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.middleware.AmqpQueue;
import be.cytomine.utils.JsonObject;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Entity
@Getter
@Setter
public class ImagingServer extends CytomineDomain {

    @NotNull
    @NotBlank
    @Column(nullable = false, unique = true)
    private String url;

    public CytomineDomain buildDomainFromJson(JsonObject json, EntityManager entityManager) {
        ImagingServer processingServer = (ImagingServer)this;
        processingServer.id = json.getJSONAttrLong("id",null);
        processingServer.url = json.getJSONAttrStr("url", true);
        processingServer.created = json.getJSONAttrDate("created");
        processingServer.updated = json.getJSONAttrDate("updated");
        return processingServer;
    }

    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject returnArray = CytomineDomain.getDataFromDomain(domain);
        ImagingServer processingServer = (ImagingServer)domain;
        returnArray.put("url", processingServer.getUrl());
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

}
