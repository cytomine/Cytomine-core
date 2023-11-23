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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParameterConstraint extends CytomineDomain {

    @NotNull
    @NotBlank
    @Column(nullable = false, unique = false)
    private String name;

    @NotNull
    @NotBlank
    private String expression;

    @NotNull
    @NotBlank
    private String dataType;



    public CytomineDomain buildDomainFromJson(JsonObject json, EntityManager entityManager) {
        ParameterConstraint parameterConstraint = (ParameterConstraint)this;
        parameterConstraint.id = json.getJSONAttrLong("id",null);
        parameterConstraint.name = json.getJSONAttrStr("name");
        parameterConstraint.expression = json.getJSONAttrStr("expression");
        parameterConstraint.dataType = json.getJSONAttrStr("dataType");
        parameterConstraint.created = json.getJSONAttrDate("created");
        parameterConstraint.updated = json.getJSONAttrDate("updated");
        return parameterConstraint;
    }

    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject returnArray = CytomineDomain.getDataFromDomain(domain);
        ParameterConstraint parameterConstraint = (ParameterConstraint)domain;
        returnArray.put("name", parameterConstraint.getName());
        returnArray.put("expression", parameterConstraint.getExpression());
        returnArray.put("dataType", parameterConstraint.getDataType());
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
