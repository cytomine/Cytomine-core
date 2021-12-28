package be.cytomine.domain.meta;

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
public class Configuration extends CytomineDomain {

    @NotNull
    @NotBlank
    @Column(nullable = false, unique = true)
    @Pattern(regexp = "^[^.]+$") // any char except a dot
    private String key;

    @NotNull
    @NotBlank
    private String value;

    @NotNull
    @Enumerated(EnumType.STRING)
    private ConfigurationReadingRole readingRole;

    public CytomineDomain buildDomainFromJson(JsonObject json, EntityManager entityManager) {
        Configuration configuration = (Configuration)this;
        configuration.id = json.getJSONAttrLong("id",null);
        configuration.key = json.getJSONAttrStr("name", true);
        configuration.value = json.getJSONAttrStr("value", true);
        configuration.readingRole = ConfigurationReadingRole.valueOf(json.getJSONAttrStr("readingRole",true));
        configuration.created = json.getJSONAttrDate("created");
        configuration.updated = json.getJSONAttrDate("updated");
        return configuration;
    }

    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject returnArray = CytomineDomain.getDataFromDomain(domain);
        Configuration configuration = (Configuration)domain;
        returnArray.put("key", configuration.getKey());
        returnArray.put("value", configuration.getValue());
        returnArray.put("readingRole", configuration.getReadingRole().name());
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
