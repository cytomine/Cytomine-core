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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Optional;

@Entity
@Getter
@Setter
public class ImageFilter extends CytomineDomain {

    @NotNull
    @NotBlank
    @Column(nullable = false, unique = true)
    private String name;

    @NotNull
    @NotBlank
    private String baseUrl;

    @ManyToOne
    private ImagingServer imagingServer;

    public CytomineDomain buildDomainFromJson(JsonObject json, EntityManager entityManager) {
        ImageFilter processingServer = (ImageFilter)this;
        processingServer.id = json.getJSONAttrLong("id",null);
        processingServer.name = json.getJSONAttrStr("name", true);
        processingServer.baseUrl = json.getJSONAttrStr("baseUrl", true);
        processingServer.imagingServer = (ImagingServer) json.getJSONAttrDomain(entityManager, "imagingServer", new ImagingServer(), false);
        processingServer.created = json.getJSONAttrDate("created");
        processingServer.updated = json.getJSONAttrDate("updated");
        return processingServer;
    }

    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject returnArray = CytomineDomain.getDataFromDomain(domain);
        ImageFilter processingServer = (ImageFilter)domain;
        returnArray.put("name", processingServer.getName());
        returnArray.put("baseUrl", processingServer.getBaseUrl());
        returnArray.put("imagingServer", Optional.ofNullable(processingServer.getImagingServer()).map(CytomineDomain::getId).orElse(null));
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
