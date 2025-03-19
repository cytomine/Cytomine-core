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
import be.cytomine.utils.SecurityUtils;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Getter
@Setter
@DiscriminatorValue("be.cytomine.domain.security.User")
public class User extends SecUser {

    @NotNull
    @NotBlank
    @Column(nullable = false)
    protected String firstname;

    @NotNull
    @NotBlank
    @Column(nullable = false)
    protected String lastname;

    @NotNull
    @NotBlank
    @Column(nullable = false)
    @Email
    @Size(min = 5, max = 254)
    protected String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    protected Language language;

    @Column(nullable = true)
    protected Boolean isDeveloper = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    protected User creator;

    public User() {
        super();
    }

    @PrePersist
    public void beforeCreate() {
        language = Language.ENGLISH;
    }

    @PreUpdate
    public void beforeUpdate() {

    }

    /**
     * Username of the human user back to this user
     * If User => humanUsername is username
     * If Algo => humanUsername is user that launch algo username
     */
    public String humanUsername() {
        return username;
    }

    public String toString() {
        return firstname + " " + lastname;
    }

    /**
     * Check if user is a job
     */
    public Boolean isAlgo() {
        return false;
    }

    public CytomineDomain buildDomainFromJson(JsonObject json, EntityManager entityManager) {
        User user = (User)this;
        user.id = json.getJSONAttrLong("id",null);
        user.username = json.getJSONAttrStr("username");
        user.firstname = json.getJSONAttrStr("firstname");
        user.lastname = json.getJSONAttrStr("lastname");
        user.email = json.getJSONAttrStr("email");
        user.language = Language.findByCode(json.getJSONAttrStr("language", "ENGLISH"));
        if(user.language == null) {
            user.language = Language.valueOf(json.getJSONAttrStr("language", "ENGLISH"));
        }
        user.origin = json.getJSONAttrStr("origin");
        user.isDeveloper = json.getJSONAttrBoolean("isDeveloper", false);
        if (json.containsKey("password") && user.password != null) {
            user.newPassword = json.getJSONAttrStr("password"); //user is updated
        } else if (json.containsKey("password")) {
            user.password = json.getJSONAttrStr("password"); //user is created
        }
        user.created = json.getJSONAttrDate("created");
        user.updated = json.getJSONAttrDate("updated");
        user.enabled = json.getJSONAttrBoolean("enabled", true);

        if (user.getPublicKey() == null || user.getPrivateKey() == null || "".equals(json.get("publicKey")) || "".equals(json.get("privateKey"))) {
            user.generateKeys();
        }
        return user;
    }

    /**
     * Define fields available for JSON response
     * @param domain Domain source for json value
     * @return Map with fields (keys) and their values
     */
    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject returnArray = SecUser.getDataFromDomain(domain);
        User user = (User)domain;
        returnArray.put("firstname", user.firstname);
        returnArray.put("lastname", user.lastname);
        returnArray.put("email", user.email);
        returnArray.put("language", (user.language!=null? user.language.toString() : null));
        if (SecurityUtils.getCurrentUserLogin().isPresent() && SecurityUtils.getCurrentUserLogin().get().equals(user.getUsername())) {
            returnArray.put("publicKey", ((User)domain).getPublicKey());
            returnArray.put("privateKey", ((User)domain).getPrivateKey());
            returnArray.put("passwordExpired", ((User)domain).getPasswordExpired());
        }
        returnArray.put("isDeveloper", user.isDeveloper);
        returnArray.put("enabled", user.enabled);
        returnArray.put("user", user.creator);
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

    @Override
    public SecUser userDomainCreator() {
        return this;
    }
}
