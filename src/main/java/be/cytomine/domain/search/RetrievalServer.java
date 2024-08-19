package be.cytomine.domain.search;

/*
 * Copyright (c) 2009-2023. Authors: see NOTICE file.
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
import be.cytomine.service.UrlApi;
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
public class RetrievalServer extends CytomineDomain {

    @NotNull
    @NotBlank
    @Column(nullable = false, unique = true)
    private String name;

    @NotBlank
    private String url;

    @Override
    public JsonObject toJsonObject() {
        return null;
    }

    public CytomineDomain buildDomainFromJson(JsonObject json, EntityManager entityManager) {
        RetrievalServer retrievalServer = this;

        retrievalServer.id = json.getJSONAttrLong("id", null);
        retrievalServer.name = json.getJSONAttrStr("name", true);
        retrievalServer.url = json.getJSONAttrStr("host", true);

        retrievalServer.created = json.getJSONAttrDate("created");
        retrievalServer.updated = json.getJSONAttrDate("updated");

        return retrievalServer;
    }

    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject returnArray = CytomineDomain.getDataFromDomain(domain);
        RetrievalServer retrievalServer = (RetrievalServer) domain;
        returnArray.put("name", retrievalServer.getName());
        returnArray.put("url", retrievalServer.getUrl());

        return returnArray;
    }

    public String getInternalUrl() {
        return UrlApi.isUsingHttpInternally() ? this.url.replace("https", "http") : this.url;
    }
}
