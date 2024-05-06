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
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

// TODO IAM: refactor
@Entity
@Getter
@Setter
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="class",
        discriminatorType = DiscriminatorType.STRING)
public class SecUser extends CytomineDomain {

    @NotNull
    @NotBlank
    @Column(nullable = false)
    protected String username;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "sec_user_sec_role",
            joinColumns = { @JoinColumn(name = "sec_user_id") },
            inverseJoinColumns = { @JoinColumn(name = "sec_role_id") }
    )
    private Set<SecRole> roles = new HashSet<>();


    /** Deprecated attributes. Kept here for migration **/
    @Deprecated
    protected String password;

    @Deprecated
    protected String publicKey;

    @Deprecated
    protected String privateKey;

    @Deprecated
    protected Boolean enabled = true;

    @Deprecated
    protected Boolean accountExpired = false;

    @Deprecated
    protected Boolean accountLocked = false;

    @Deprecated
    protected Boolean passwordExpired = false;

    @Deprecated
    protected String origin;

    @Deprecated
    public void generateKeys() {
        String privateKey = UUID.randomUUID().toString();
        String publicKey = UUID.randomUUID().toString();
        this.setPrivateKey(privateKey);
        this.setPublicKey(publicKey);
    }

    @Deprecated
    public Boolean isAlgo() {
        return false;
    }

    public String toString() {
        return username;
    }

    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.put("class", domain.getClass());
        jsonObject.put("id", domain.getId());
        return jsonObject;
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
