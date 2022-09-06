package be.cytomine.security;

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

import be.cytomine.config.properties.ApplicationProperties;
import be.cytomine.domain.security.*;
import be.cytomine.exceptions.ForbiddenException;
import be.cytomine.repository.security.SecRoleRepository;
import be.cytomine.repository.security.SecUserRepository;
import be.cytomine.repository.security.SecUserSecRoleRepository;
import be.cytomine.repository.security.UserRepository;
import be.cytomine.security.ldap.LdapClient;
import be.cytomine.service.image.server.StorageService;
import be.cytomine.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.naming.NamingException;
import javax.persistence.EntityManager;
import java.util.*;

import static be.cytomine.security.DomainUserDetailsService.createSpringSecurityUser;

/**
 * Authenticate a user from the database.
 */
@Component("casLdapUserDetailsService")
@RequiredArgsConstructor
@Transactional
public class CASLdapUserDetailsService implements UserDetailsService {

    private final Logger log = LoggerFactory.getLogger(CASLdapUserDetailsService.class);

    private final LdapClient ldapClient;
    private final UserRepository userRepository;

    private final SecRoleRepository secRoleRepository;

    private final Environment env;

    private final EntityManager entityManager;

    private final ApplicationProperties applicationProperties;

    private final ApplicationContext applicationContext;

    private final StorageService storageService;

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        log.info("ldap search for {}", username);

        User user = userRepository.findByUsernameLikeIgnoreCase(username)
                .orElseGet(() -> {
                    try {
                        Map<String, Object> ldapResults = ldapClient.getUserInfo(env.getProperty("ldap.search", "NO_LDAP_SEARCH"), username, Arrays.stream(env.getProperty("ldap.attributes").split(",")).toList());
                        log.debug("ldap results: " + ldapResults);

                        if (ldapResults==null) {
                            throw new UsernameNotFoundException("Username " + username + " not found in ldap");
                        }
                        return createUserFromLdapResults(username, ldapResults);
                    } catch (NamingException e) {
                        throw new RuntimeException(e);
                    }
                }); //User does not exists in our database

        if(!user.getEnabled()) {
            throw new DisabledException("Disabled user");
        }

        return createSpringSecurityUser(username, user);
    }

    public User createUserFromLdapResults(String username, Map<String, Object> ldapResponse) {
        String firstname;
        String lastname;
        String mail;
        try {
            firstname = (String)ldapResponse.get("givenname");
            lastname = (String)ldapResponse.get("sn");
            mail = (String)ldapResponse.get("mail");
        } catch(Exception e){
            log.error(e.getMessage());
            e.printStackTrace();
            throw e;
        }

        User user = new User();
        user.setUsername(username);
        user.setLastname(lastname);
        user.setFirstname(firstname);
        user.setEmail(mail);
        user.setEnabled(true);
        user.setPassword(UUID.randomUUID().toString());
        user.setOrigin("LDAP");
        user.setLanguage(Language.valueOf(applicationProperties.getDefaultLanguage()));
        user.generateKeys();

        user = userRepository.save(user);


        SecRole userRole = secRoleRepository.getGuest();

        SecUserSecRole secUsersecRole = new SecUserSecRole();
        secUsersecRole.setSecUser(user);
        secUsersecRole.setSecRole(userRole);

        entityManager.persist(secUsersecRole);
        entityManager.flush();
        entityManager.refresh(user);

        User finalUser = userRepository.findByUsernameLikeIgnoreCase(user.getUsername()).get();
        SecurityUtils.doWithAuth(applicationContext, "admin", () -> storageService.createStorage(finalUser));
        return user;
    }
}
