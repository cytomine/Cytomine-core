package be.cytomine.domain.image.server;

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
import be.cytomine.domain.security.User;
import be.cytomine.utils.JsonObject;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Getter
@Setter
public class Storage extends CytomineDomain {

    @NotNull
    @NotBlank
    @Column(nullable = false)
    protected String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    protected User user;

//    @PrePersist
//    public void beforeCreate() {
//        super.beforeInsert();
//    }
//
//    @PreUpdate
//    public void beforeUpdate() {
//        super.beforeUpdate();
//    }

    public CytomineDomain buildDomainFromJson(JsonObject json, EntityManager entityManager) {
        Storage storage = (Storage)this;
        storage.id = json.getJSONAttrLong("id",null);
        storage.name = json.getJSONAttrStr("name", true);
        storage.user = (User)json.getJSONAttrDomain(entityManager, "user", new User(), true);
        storage.created = json.getJSONAttrDate("created");
        storage.updated = json.getJSONAttrDate("updated");
        return storage;
    }

    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject returnArray = CytomineDomain.getDataFromDomain(domain);
        Storage storage = (Storage)domain;
        returnArray.put("name", storage.getName());
        returnArray.put("user", (storage.getUser()!=null ? storage.getUser().getId() : null));
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
        return this;
    }
}
