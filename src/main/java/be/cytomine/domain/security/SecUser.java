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
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.utils.JsonObject;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.crypto.password.PasswordEncoder;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.*;

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
//    @Pattern(regexp = "^[^\\ ].*[^\\ ]\\$") TODO
    protected String username;

    @NotNull
    @NotBlank
    @Column(name = "`password`", nullable = false)
    protected String password;

    @Transient
    String newPassword = null;

    @NotBlank
    @Column(nullable = true)
    protected String publicKey;

    @NotBlank
    @Column(nullable = true)
    protected String privateKey;

    protected Boolean enabled = true;

    protected Boolean accountExpired = false;

    protected Boolean accountLocked = false;

    protected Boolean passwordExpired = false;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "sec_user_sec_role",
            joinColumns = { @JoinColumn(name = "sec_user_id") },
            inverseJoinColumns = { @JoinColumn(name = "sec_role_id") }
    )
    private Set<SecRole> roles = new HashSet<>();

    @NotBlank
    @Column(nullable = true)
    protected String origin;

    public SecUser() {
    }

    public String humanUsername() {
        return username;
    }

    public void generateKeys() {
        String privateKey = UUID.randomUUID().toString();
        String publicKey = UUID.randomUUID().toString();
        this.setPrivateKey(privateKey);
        this.setPublicKey(publicKey);
    }

    public Boolean isAlgo() {
        return false;
    }

    public String toString() {
        return username;
    }

    public void encodePassword(PasswordEncoder passwordEncoder) {
        byte minLength = 8;
        if(password.length() < minLength) {
            throw new WrongArgumentException("Your password must have at least $minLength characters!");
        }
        password = passwordEncoder.encode(password);
    }

    /**
     * Define fields available for JSON response
     * @param domain Domain source for json value
     * @return Map with fields (keys) and their values
     */
    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject returnArray = CytomineDomain.getDataFromDomain(domain);
        SecUser user = (SecUser)domain;
        returnArray.put("username", user.username);
        returnArray.put("origin", user.origin);
        returnArray.put("algo", user.isAlgo());
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
