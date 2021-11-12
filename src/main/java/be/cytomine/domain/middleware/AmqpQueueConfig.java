package be.cytomine.domain.middleware;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.utils.JsonObject;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Entity
@Getter
@Setter
public class AmqpQueueConfig extends CytomineDomain {

    @NotNull
    @NotBlank
    @Column(nullable = false, unique = true)
    @Pattern(regexp = "[a-zA-Z0-9-_]+")
    private String name;

    private String defaultValue;

    @NotNull
    Integer index;

    @NotNull
    Boolean isInMap = false;

    @NotNull
    @Enumerated(EnumType.STRING)
    AmqpQueueConfigType type;

    public CytomineDomain buildDomainFromJson(JsonObject json, EntityManager entityManager) {
        AmqpQueueConfig amqpQueue = (AmqpQueueConfig)this;
        amqpQueue.id = json.getJSONAttrLong("id",null);
        amqpQueue.name = json.getJSONAttrStr("name");
        amqpQueue.defaultValue = json.getJSONAttrStr("host");
        amqpQueue.index = json.getJSONAttrInteger("index");
        amqpQueue.isInMap = json.getJSONAttrBoolean("isInMap", false);
        amqpQueue.type = AmqpQueueConfigType.valueOf(json.getJSONAttrStr("type"));
        return amqpQueue;
    }

    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject returnArray = CytomineDomain.getDataFromDomain(domain);
        AmqpQueueConfig amqpQueue = (AmqpQueueConfig)domain;
        returnArray.put("name", amqpQueue.getName());
        returnArray.put("defaultValue", amqpQueue.getDefaultValue());
        returnArray.put("index", amqpQueue.getIndex());
        returnArray.put("isInMap", amqpQueue.getIsInMap());
        returnArray.put("type", amqpQueue.getType());
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
