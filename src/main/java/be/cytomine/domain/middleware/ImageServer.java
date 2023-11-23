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
import be.cytomine.service.UrlApi;
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
public class ImageServer extends CytomineDomain {

    @NotNull
    @NotBlank
    @Column(nullable = false, unique = true)
    private String name;

    @NotBlank
    private String url;

    private String basePath;

    @NotNull
    private Boolean available;

    public CytomineDomain buildDomainFromJson(JsonObject json, EntityManager entityManager) {
        ImageServer imageServer = this;
        imageServer.id = json.getJSONAttrLong("id",null);
        imageServer.name = json.getJSONAttrStr("name", true);
        imageServer.url = json.getJSONAttrStr("host", true);
        imageServer.basePath = json.getJSONAttrStr("exchange", true);
        imageServer.available = json.getJSONAttrBoolean("available", true);

        imageServer.created = json.getJSONAttrDate("created");
        imageServer.updated = json.getJSONAttrDate("updated");
        return imageServer;
    }

    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject returnArray = CytomineDomain.getDataFromDomain(domain);
        ImageServer imageServer = (ImageServer)domain;
        returnArray.put("name", imageServer.getName());
        returnArray.put("url", imageServer.getUrl());
        returnArray.put("basePath", imageServer.getBasePath());
        returnArray.put("available", imageServer.getAvailable());
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

    @Override
    public CytomineDomain container() {
        return this;
    }

    public String getInternalUrl() {
        return UrlApi.isUsingHttpInternally() ? this.url.replace("https", "http") : this.url;
    }

}
