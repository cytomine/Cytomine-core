package be.cytomine.domain.processing;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.utils.JsonObject;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Entity
@Getter
@Setter
public class SoftwareUserRepository extends CytomineDomain {

    @NotNull
    @NotBlank
    private String provider;

    @NotNull
    @NotBlank
    private String username;

    @NotNull
    @NotBlank
    private String dockerUsername;

    private String prefix;

    public CytomineDomain buildDomainFromJson(JsonObject json, EntityManager entityManager) {
        SoftwareUserRepository softwareUserRepository = (SoftwareUserRepository)this;
        softwareUserRepository.id = json.getJSONAttrLong("provider",null);
        softwareUserRepository.provider = json.getJSONAttrStr("name", true);
        softwareUserRepository.username = json.getJSONAttrStr("username", true);
        softwareUserRepository.dockerUsername = json.getJSONAttrStr("dockerUsername", true);
        softwareUserRepository.prefix = json.getJSONAttrStr("prefix", false);
        softwareUserRepository.created = json.getJSONAttrDate("created");
        softwareUserRepository.updated = json.getJSONAttrDate("updated");
        return softwareUserRepository;
    }

    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject returnArray = CytomineDomain.getDataFromDomain(domain);
        SoftwareUserRepository softwareUserRepository = (SoftwareUserRepository)domain;
        returnArray.put("provider", softwareUserRepository.getProvider());
        returnArray.put("username", softwareUserRepository.getUsername());
        returnArray.put("dockerUsername", softwareUserRepository.getDockerUsername());
        returnArray.put("prefix", softwareUserRepository.getPrefix());
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
