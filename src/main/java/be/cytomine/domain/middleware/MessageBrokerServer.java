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
public class MessageBrokerServer extends CytomineDomain {

    @NotNull
    @NotBlank
    @Column(nullable = false, unique = true)
    private String name;

    @NotNull
    @NotBlank
    private String host;

    @NotNull
    private Integer port = 22;


    public CytomineDomain buildDomainFromJson(JsonObject json, EntityManager entityManager) {
        MessageBrokerServer processingServer = (MessageBrokerServer)this;
        processingServer.id = json.getJSONAttrLong("id",null);
        processingServer.name = json.getJSONAttrStr("name", true);
        processingServer.host = json.getJSONAttrStr("host", true);
        processingServer.port = json.getJSONAttrInteger("name", null);
        processingServer.created = json.getJSONAttrDate("created");
        processingServer.updated = json.getJSONAttrDate("updated");
        return processingServer;
    }

    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject returnArray = CytomineDomain.getDataFromDomain(domain);
        MessageBrokerServer processingServer = (MessageBrokerServer)domain;
        returnArray.put("name", processingServer.getName());
        returnArray.put("host", processingServer.getHost());
        returnArray.put("port", processingServer.getPort());
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
