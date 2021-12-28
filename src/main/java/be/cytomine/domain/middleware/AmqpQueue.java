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
public class AmqpQueue extends CytomineDomain {

    @NotNull
    @NotBlank
    @Column(nullable = false, unique = true)
    private String name;

    @NotBlank
    private String host;

    @Column(nullable = false, unique = true)
    private String exchange;

    public CytomineDomain buildDomainFromJson(JsonObject json, EntityManager entityManager) {
        AmqpQueue amqpQueue = (AmqpQueue)this;
        amqpQueue.id = json.getJSONAttrLong("id",null);
        amqpQueue.name = json.getJSONAttrStr("name");
        amqpQueue.host = json.getJSONAttrStr("host");
        amqpQueue.exchange = json.getJSONAttrStr("exchange");
        amqpQueue.created = json.getJSONAttrDate("created");
        amqpQueue.updated = json.getJSONAttrDate("updated");
        return amqpQueue;
    }

    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject returnArray = CytomineDomain.getDataFromDomain(domain);
        AmqpQueue amqpQueue = (AmqpQueue)domain;
        returnArray.put("name", amqpQueue.getName());
        returnArray.put("host", amqpQueue.getHost());
        returnArray.put("exchange", amqpQueue.getExchange());
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
