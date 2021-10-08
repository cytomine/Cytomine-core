package be.cytomine.domain.security;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.utils.JsonObject;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Entity
@Getter
@Setter
public class SecRole extends CytomineDomain implements Serializable {

    @NotNull
    @NotBlank
    @Column(unique = true)
    private String authority;

    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject returnArray = CytomineDomain.getDataFromDomain(domain);
        SecRole role = (SecRole)domain;
        returnArray.put("authority", role.authority);
        return returnArray;
    }

    @Override
    public String toJSON() {
        return getDataFromDomain(this).toJsonString();
    }

    @Override
    public JsonObject toJsonObject() {
        return getDataFromDomain(this);
    }
}
