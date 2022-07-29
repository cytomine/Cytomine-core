package be.cytomine.domain.image.group;

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
import be.cytomine.domain.project.Project;
import be.cytomine.utils.JsonObject;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;

@Entity
@Getter
@Setter
public class ImageGroup extends CytomineDomain {

    private String name;

    @ManyToOne(fetch = FetchType.EAGER)
    private Project project;

    public CytomineDomain buildDomainFromJson(JsonObject json, EntityManager entityManager) {
        ImageGroup imageGroup = this;

        imageGroup.id = json.getJSONAttrLong("id", null);
        imageGroup.created = json.getJSONAttrDate("created");
        imageGroup.updated = json.getJSONAttrDate("updated");

        imageGroup.name = json.getJSONAttrStr("name", null);
        imageGroup.project = (Project) json.getJSONAttrDomain(entityManager, "project", new Project(), true);

        return imageGroup;
    }

    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject returnArray = CytomineDomain.getDataFromDomain(domain);
        ImageGroup imageGroup = (ImageGroup) domain;

        returnArray.put("name", imageGroup.getName());
        returnArray.put("project", imageGroup.getProject());

        return returnArray;
    }

    @Override
    public JsonObject toJsonObject() {
        return getDataFromDomain(this);
    }
}
