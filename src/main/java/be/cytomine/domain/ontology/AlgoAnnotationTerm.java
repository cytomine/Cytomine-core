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
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.SecUser;
import be.cytomine.domain.security.UserJob;
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.utils.JsonObject;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

@Entity
@Getter
@Setter
/**
 * Term added to an annotation by a job
 * Annotation can be:
 * -algo annotation (create by a job)
 * -user annotation (create by a real user)
 */
public class AlgoAnnotationTerm extends CytomineDomain implements Serializable {

    /**
     * Term can be add to user or algo annotation
     * Rem: 'AnnotationDomain annotation' diden't work because user and algo annotation
     * are store in different table
     * So we store annotation type and annotation id
     */
    @NotNull
    private String annotationClassName;

    @NotNull
    @Column(name = "annotation_ident")
    private Long annotationIdent;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "term_id", nullable = true)
    private Term term;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "expected_term_id", nullable = true)
    private Term expectedTerm;

    @Min(0)
    @Max(1)
    Double rate;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_job_id", nullable = false)
    private UserJob userJob;

    /**
     * Project for the prediction
     * rem: redundance for optim (we should get it with retrieveAnnotationDomain().project)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = true)
    private Project project;


    public String toString() {
        return annotationClassName + " " + annotationIdent + " with term " + term + " from userjob " + userJob + " and  project " + project;
    }

    /**
     * Set annotation (storing class + id)
     * With groovy, you can do: this.annotation = ...
     * @param annotation Annotation to add
     */
    public void setAnnotation(AnnotationDomain annotation) {
        annotationClassName = annotation.getClass().getName();
        annotationIdent = annotation.getId();
        project = annotation.getProject();
    }
    
    
    


    public CytomineDomain buildDomainFromJson(JsonObject json, EntityManager entityManager) {
        AlgoAnnotationTerm algoAnnotationTerm = this;
        algoAnnotationTerm.id = json.getJSONAttrLong("id",null);

        Long annotationId = json.getJSONAttrLong("annotationIdent", -1L);
        String annotationClassName = json.getJSONAttrStr("annotationClassName");
        AnnotationDomain annotation = AnnotationDomain.getAnnotationDomain(entityManager, annotationId, annotationClassName);

        algoAnnotationTerm.setAnnotation(annotation);

        algoAnnotationTerm.term = (Term)json.getJSONAttrDomain(entityManager, "term", new Term(), false);
        algoAnnotationTerm.expectedTerm = (Term)json.getJSONAttrDomain(entityManager, "expectedTerm", new Term(), false);
        algoAnnotationTerm.userJob = (UserJob) json.getJSONAttrDomain(entityManager, "user", new UserJob(), false);
        algoAnnotationTerm.rate = json.getJSONAttrDouble("rate",0d);

        algoAnnotationTerm.created = json.getJSONAttrDate("created");
        algoAnnotationTerm.updated = json.getJSONAttrDate("updated");

        if(algoAnnotationTerm.term.getOntology()!=annotation.project.getOntology()) {
            throw new WrongArgumentException("Term " +term.getName()+ " from ontology " + term.getOntology().getName() + " is not in ontology from the annotation project");
        }
        return algoAnnotationTerm;
    }

    public SecUser userDomainCreator() {
        return userJob;
    }

    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject returnArray = CytomineDomain.getDataFromDomain(domain);
        AlgoAnnotationTerm annotationTerm = (AlgoAnnotationTerm)domain;
        returnArray.put("annotationIdent", annotationTerm.getAnnotationIdent());
        returnArray.put("annotationClassName", annotationTerm.getAnnotationClassName());
        returnArray.put("annotation", annotationTerm.getAnnotationIdent());

        returnArray.put("term", (annotationTerm.getTerm()!=null ? annotationTerm.getTerm().getId() : null));
        returnArray.put("expectedTerm", (annotationTerm.getExpectedTerm()!=null ? annotationTerm.getExpectedTerm().getId() : null));
        returnArray.put("rate", annotationTerm.getRate());

        returnArray.put("user", (annotationTerm.getUserJob()!=null ? annotationTerm.getUserJob().getId() : null));
        returnArray.put("project", (annotationTerm.getProject()!=null ? annotationTerm.getProject().getId() : null));
        return returnArray;
    }

    @Override
    public String toJSON() {
        return getDataFromDomain(this).toJsonString();
    }

    @Override
    public JsonObject toJsonObject() {
        return getDataFromDomain(this);
    }

    public CytomineDomain container() {
        return project.container();
    }

}
