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
import be.cytomine.domain.image.SliceInstance;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.SecUser;
import be.cytomine.domain.security.UserJob;
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.service.UrlApi;
import be.cytomine.utils.JsonObject;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Getter
@Setter
public class AlgoAnnotation extends AnnotationDomain implements Serializable {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserJob user;

    Integer countReviewedAnnotations = 0;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "annotation_ident", referencedColumnName = "id")
    private List<AlgoAnnotationTerm> algoAnnotationTerms = new ArrayList<>();

    @PrePersist
    public void beforeCreate() {
        super.beforeCreate();
    }

    @PreUpdate
    public void beforeUpdate() {
        super.beforeUpdate();
    }

    @Override
    public List<Term> terms() {
        return algoAnnotationTerms.stream()
                .map(AlgoAnnotationTerm::getTerm).distinct().collect(Collectors.toList());
    }

    /**
     * Get all annotation terms id
     * @return Terms id list
     */
    public List<Long> termsId() {
        return terms().stream().map(CytomineDomain::getId).distinct().collect(Collectors.toList());

    }

    /**
     * Check if annotation is reviewed
     * @return True if annotation is linked with at least one review annotation
     */
    boolean hasReviewedAnnotation() {
        return countReviewedAnnotations > 0;
    }

    @Override
    public boolean isUserAnnotation() {
        return false;
    }

    /**
     * Check if its an algo annotation
     */
    public boolean isAlgoAnnotation() {
        return true;
    }

    /**
     * Check if its a review annotation
     */
    public boolean isReviewedAnnotation() {
        return false;
    }

    @Override
    public boolean isRoiAnnotation() {
        return false;
    }

    /**
     * Get all terms for automatic review
     * If review is done "for all" (without manual user control), we add these term to the new review annotation
     * @return
     */
    public List<Term> termsForReview() {
        return algoAnnotationTerms.stream().filter(x -> x.getUserJob()!=null && x.getUserJob().equals(getUser()))
                .map(AlgoAnnotationTerm::getTerm).distinct().collect(Collectors.toList());
    }

    @Override
    Long getUserId() {
        return user.getId();
    }

    public CytomineDomain buildDomainFromJson(JsonObject json, EntityManager entityManager) {
        AlgoAnnotation annotation = this;
        annotation.id = json.getJSONAttrLong("id",null);
        annotation.created = json.getJSONAttrDate("created");
        annotation.updated = json.getJSONAttrDate("updated");

        annotation.slice = (SliceInstance)json.getJSONAttrDomain(entityManager, "slice", new SliceInstance(), true);
        annotation.image = (ImageInstance)json.getJSONAttrDomain(entityManager, "image", new ImageInstance(), true);
        annotation.project = (Project)json.getJSONAttrDomain(entityManager, "project", new Project(), true);
        annotation.user = (UserJob)json.getJSONAttrDomain(entityManager, "user", new UserJob(), true);

        annotation.geometryCompression = json.getJSONAttrDouble("geometryCompression",0D);

        if (json.containsKey("location") && json.get("location") instanceof Geometry) {
            annotation.location = (Geometry) json.get("location");
        } else {
            try {
                annotation.location = new WKTReader().read(json.getJSONAttrStr("location"));
            }
            catch (ParseException ex) {
                throw new WrongArgumentException(ex.toString());
            }
        }

        if (annotation.location==null) {
            throw new WrongArgumentException("Geometry is null: 0 points");
        }

        if (annotation.location.getNumPoints() < 1) {
            throw new WrongArgumentException("Geometry is empty:" + annotation.location.getNumPoints() + " points");
        }

        return annotation;
    }

    @Override
    public JsonObject toJsonObject() {
        return getDataFromDomain(this);
    }

    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject returnArray = AnnotationDomain.getDataFromDomain(domain);
        AlgoAnnotation annotation = (AlgoAnnotation)domain;
        returnArray.put("cropURL", UrlApi.getAlgoAnnotationCropWithAnnotationId(annotation.getId(), "png"));
        returnArray.put("smallCropURL", UrlApi.getAlgoAnnotationCropWithAnnotationIdWithMaxSize(annotation.getId(), 256, "png"));
        returnArray.put("url", UrlApi.getAlgoAnnotationCropWithAnnotationId(annotation.getId(), "png"));
        returnArray.put("imageURL", UrlApi.getAnnotationURL(annotation.getImage().getProject().getId(), annotation.getImage().getId(), annotation.getId()));
        returnArray.put("reviewed", annotation.hasReviewedAnnotation());
        return returnArray;
    }

    @Override
    public SecUser user() {
        return user;
    }

    @Override
    public SecUser userDomainCreator() {
        return user;
    }
}
