package be.cytomine.domain.processing;

/*
* Copyright (c) 2009-2022. Authors: see NOTICE file.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import be.cytomine.domain.CytomineDomain;
import be.cytomine.utils.JsonObject;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

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
