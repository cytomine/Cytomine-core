package be.cytomine.domain.command;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.utils.JsonObject;
import lombok.Data;

import javax.persistence.Entity;

@Entity
@Data
public class Transaction extends CytomineDomain {

    @Override
    public String toJSON() {
        return toJsonObject().toJsonString();
    }

    @Override
    public JsonObject toJsonObject() {
        return CytomineDomain.getDataFromDomain(this);
    }
}
