package be.cytomine.domain.middleware;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.utils.JsonObject;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Entity
@Getter
@Setter
public class ImageServer extends CytomineDomain {

    @NotNull
    @NotBlank
    @Column(nullable = false, unique = true)
    private String name;

    @NotBlank
    private String url;

    private String basePath;

    @NotNull
    private Boolean available;

    public CytomineDomain buildDomainFromJson(JsonObject json, EntityManager entityManager) {
        ImageServer imageServer = this;
        imageServer.id = json.getJSONAttrLong("id",null);
        imageServer.name = json.getJSONAttrStr("name", true);
        imageServer.url = json.getJSONAttrStr("host", true);
        imageServer.basePath = json.getJSONAttrStr("exchange", true);
        imageServer.available = json.getJSONAttrBoolean("available", true);

        imageServer.created = json.getJSONAttrDate("created");
        imageServer.updated = json.getJSONAttrDate("updated");
        return imageServer;
    }

    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject returnArray = CytomineDomain.getDataFromDomain(domain);
        ImageServer imageServer = (ImageServer)domain;
        returnArray.put("name", imageServer.getName());
        returnArray.put("url", imageServer.getUrl());
        returnArray.put("basePath", imageServer.getBasePath());
        returnArray.put("available", imageServer.getAvailable());
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
    public CytomineDomain container() {
        return this;
    }

    public String getInternalUrl(boolean useHTTPInternally) {
        return useHTTPInternally ? this.url.replace("https", "http") : this.url;
    }

}
