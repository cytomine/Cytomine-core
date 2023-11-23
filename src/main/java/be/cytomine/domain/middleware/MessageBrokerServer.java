package be.cytomine.domain.middleware;

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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

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
