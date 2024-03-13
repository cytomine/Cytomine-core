package be.cytomine.domain.security;

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

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Getter
@Setter
public class SecUserSecRole extends CytomineDomain implements Serializable {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sec_user_id", nullable = false)
    private SecUser secUser;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sec_role_id", nullable = false)
    private SecRole secRole;

    Long getSecUserId() {
        return (secUser!=null? secUser.getId() : null);
    }

    Long getSecRoleId() {
        return (secRole!=null? secRole.getId() : null);
    }

    String getSecRoleAuthority() {
        return (secRole!=null? secRole.getAuthority() : null);
    }

    public CytomineDomain buildDomainFromJson(JsonObject json, EntityManager entityManager) {
        SecUserSecRole secUserSecRole = this;
        secUserSecRole.id = json.getJSONAttrLong("id",null);
        secUserSecRole.secUser = (User)json.getJSONAttrDomain(entityManager, "user", new SecUser(), true);
        secUserSecRole.secRole = (SecRole)json.getJSONAttrDomain(entityManager, "role", new SecRole(), true);
        secUserSecRole.created = json.getJSONAttrDate("created");
        secUserSecRole.updated = json.getJSONAttrDate("updated");
        return secUserSecRole;
    }

    /**
     * Define fields available for JSON response
     * @param domain Domain source for json value
     * @return Map with fields (keys) and their values
     */
    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject returnArray = CytomineDomain.getDataFromDomain(domain);
        SecUserSecRole secUserSecRole = (SecUserSecRole)domain;
        returnArray.put("user", secUserSecRole.getSecUserId());
        returnArray.put("role", secUserSecRole.getSecRoleId());
        returnArray.put("authority", secUserSecRole.getSecRoleAuthority());
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
}
