package be.cytomine.domain.social;

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
import be.cytomine.domain.CytomineSocialDomain;
import be.cytomine.domain.ontology.Ontology;
import be.cytomine.domain.ontology.RelationTerm;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.utils.JsonObject;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;

import static be.cytomine.domain.ontology.RelationTerm.PARENT;

//@Entity
@Getter
@Setter
@Document
//@CompoundIndex(def = "{'user' : 1, 'created' : -1}")
public class PersistentConnection extends CytomineSocialDomain {

    protected Long id;

    @CreatedDate
    protected Date created;

    @LastModifiedDate
    protected Date updated;

    @NotNull
    protected Long user;

    private Long project;

    private String session;

    @Override
    public JsonObject toJsonObject() {
        throw new WrongArgumentException("getDataFromDomain is not implemented for this class");
    }

    @Override
    public String toString() {
        return "PersistentConnection{" +
                "id=" + id +
                ", created=" + created +
                ", updated=" + updated +
                ", user=" + user +
                ", project=" + project +
                ", session='" + session + '\'' +
                '}';
    }
}
