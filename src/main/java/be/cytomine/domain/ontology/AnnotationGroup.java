package be.cytomine.domain.ontology;

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
import be.cytomine.domain.image.group.ImageGroup;
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
public class AnnotationGroup extends CytomineDomain {

    @ManyToOne(fetch = FetchType.LAZY)
    Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    ImageGroup imageGroup;

    String type;

    public CytomineDomain buildDomainFromJson(JsonObject json, EntityManager entityManager) {
        AnnotationGroup annotationGroup = this;

        annotationGroup.id = json.getJSONAttrLong("id", null);
        annotationGroup.created = json.getJSONAttrDate("created");
        annotationGroup.updated = json.getJSONAttrDate("updated");

        annotationGroup.project = (Project) json.getJSONAttrDomain(entityManager, "project", new Project(), true);
        annotationGroup.imageGroup = (ImageGroup) json.getJSONAttrDomain(entityManager, "imageGroup", new ImageGroup(), true);
        annotationGroup.type = json.getJSONAttrStr("type", "SAME_OBJECT");

        return annotationGroup;
    }

    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject returnArray = CytomineDomain.getDataFromDomain(domain);
        AnnotationGroup annotationGroup = (AnnotationGroup) domain;

        returnArray.put("project", annotationGroup.getProject().getId());
        returnArray.put("imageGroup", annotationGroup.getImageGroup().getId());
        returnArray.put("type", annotationGroup.getType());

        return returnArray;
    }

    @Override
    public JsonObject toJsonObject() {
        return getDataFromDomain(this);
    }

    @Override
    public CytomineDomain container() {
        return imageGroup.container();
    }
}
