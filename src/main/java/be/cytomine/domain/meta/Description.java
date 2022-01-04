package be.cytomine.domain.meta;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.utils.JsonObject;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.validation.constraints.NotNull;

@Entity
@Getter
@Setter
public class Description extends CytomineDomain {

    @NotNull
    private String data;

    @NotNull
    private String domainClassName;

    @NotNull
    private Long domainIdent;

    @Override
    public String toJSON() {
        return null;
    }

    @Override
    public JsonObject toJsonObject() {
        return null;
    }
}
