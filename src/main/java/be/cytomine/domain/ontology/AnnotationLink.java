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
import be.cytomine.domain.image.ImageInstance;
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
public class AnnotationLink extends CytomineDomain {

    String annotationClassName;

    Long annotationIdent;

    @ManyToOne(fetch = FetchType.LAZY)
    AnnotationGroup group;

    @ManyToOne(fetch = FetchType.LAZY)
    ImageInstance image;

    public CytomineDomain buildDomainFromJson(JsonObject json, EntityManager entityManager) {
        AnnotationLink annotationLink = this;

        annotationLink.id = json.getJSONAttrLong("id", null);
        annotationLink.created = json.getJSONAttrDate("created");
        annotationLink.updated = json.getJSONAttrDate("updated");

        annotationLink.annotationClassName = json.getJSONAttrStr("annotationClassName", true);
        annotationLink.annotationIdent = json.getJSONAttrLong("annotationIdent", null);
        annotationLink.group = (AnnotationGroup) json.getJSONAttrDomain(entityManager, "group", new AnnotationGroup(), true);
        annotationLink.image = (ImageInstance) json.getJSONAttrDomain(entityManager, "image", new ImageInstance(), true);

        return annotationLink;
    }

    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject returnArray = CytomineDomain.getDataFromDomain(domain);
        AnnotationLink annotationLink = (AnnotationLink) domain;

        returnArray.put("annotationClassName", annotationLink.getAnnotationClassName());
        returnArray.put("annotationIdent", annotationLink.getAnnotationIdent());
        returnArray.put("group", annotationLink.getGroup().getId());
        returnArray.put("image", annotationLink.getImage().getId());

        return returnArray;
    }

    @Override
    public JsonObject toJsonObject() {
        return getDataFromDomain(this);
    }

    @Override
    public CytomineDomain container() {
        return group.container();
    }
}
