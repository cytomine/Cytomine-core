package be.cytomine.security.saml;


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
import be.cytomine.config.properties.Saml2Properties;
import be.cytomine.domain.security.Language;
import be.cytomine.domain.security.SecRole;
import be.cytomine.domain.security.SecUserSecRole;
import be.cytomine.domain.security.User;
import be.cytomine.repository.security.SecRoleRepository;
import be.cytomine.repository.security.UserRepository;
import be.cytomine.service.image.server.StorageService;
import be.cytomine.utils.ResourcesUtils;
import be.cytomine.utils.SecurityUtils;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static be.cytomine.security.DomainUserDetailsService.createSpringSecurityUser;

@Component
@RequiredArgsConstructor
@Transactional
@Slf4j
@Data
public class CasSamlUserDetailsService implements UserDetailsService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SecRoleRepository secRoleRepository;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private ApplicationProperties applicationProperties;
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private StorageService storageService;


    public UserDetails loadUserBySaml2AuthPrincipal(DefaultSaml2AuthenticatedPrincipal saml2AuthenticatedPrincipal, String password)
            throws UsernameNotFoundException, NoSuchFieldException, IllegalAccessException {

        //Attributes from saml auth server
        LinkedHashMap<String, List<Object>> attributes = (LinkedHashMap<String, List<Object>>) saml2AuthenticatedPrincipal.getAttributes();
        log.debug("SAML attributes: {}", attributes.toString());
        String registrationId = saml2AuthenticatedPrincipal.getRelyingPartyRegistrationId();
        log.debug("SAML registrationId: {}", registrationId);
        Map<String, String> attributeMap;
        Saml2Properties saml2ApplicationProperties = applicationProperties.getAuthentication().getSaml2();
        try {
            //Here we get the corresponding configuration for the registrationId , it is a field in Saml2Properties class
            Field idpConfigField = saml2ApplicationProperties.getClass().getDeclaredField(registrationId);
            idpConfigField.setAccessible(true);
            Map<String, String> registrationConfigMap = (Map<String, String>) idpConfigField.get(saml2ApplicationProperties);
            log.debug("Got this SAML configuration: {} for registrationId: {}", registrationConfigMap.toString(), registrationId);
            //Local mapping of attributes
            attributeMap = ResourcesUtils.getPropertiesMap(registrationConfigMap.get("attributeMappingFile"));
            log.debug("SAML attribute map: {} for registrationId: {}", attributeMap, registrationId);
        } catch (NoSuchFieldException e) {
            log.error("no configuration with name: {} found in application.authentication.saml2", registrationId, e.getMessage());
            throw e;
        }
        String username;
        try {
            username = (String) attributes.get(attributeMap.get("username")).get(0);
        } catch (Exception e) {
            log.error("no mapping found for attribute: username", e.getMessage());
            throw e;
        }
        log.debug("SAML username: {}", username);
        User user = userRepository.findByUsernameLikeIgnoreCase(username)
                .orElseGet(() -> {
                    try {
                        return createUserFromSamlResults(username, attributes, attributeMap, registrationId, password);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }); //User does not exists in our database

        if (!user.getEnabled()) {
            throw new DisabledException("Disabled user");
        }
        return createSpringSecurityUser(username, user);
    }

    public User createUserFromSamlResults(String username, LinkedHashMap<String, List<Object>> samlAttributes, Map<String, String> attributeMap, String registrationId, String password) throws NoSuchFieldException {

        log.debug("creating user from saml results with username: {}", username);
        String firstname;
        String lastname;
        String mail;
        try {
            firstname = (String) samlAttributes.get(attributeMap.get("firstname")).get(0);
            lastname = (String) samlAttributes.get(attributeMap.get("lastname")).get(0);
            mail = (String) samlAttributes.get(attributeMap.get("mail")).get(0);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw e;
        }

        User user = new User();
        user.setUsername(username);
        user.setLastname(lastname);
        user.setFirstname(firstname);
        user.setEmail(mail);
        user.setEnabled(true);
        user.setPassword(password);
        user.setOrigin(registrationId);
        user.setLanguage(Language.valueOf(applicationProperties.getDefaultLanguage()));
        user.generateKeys();

        user = userRepository.save(user);
        log.debug("user created: {}", user.getUsername());

        String group_attribute = attributeMap.get("group");
        List<SecRole> userRoles = new ArrayList<>();

        if (samlAttributes.containsKey(group_attribute)) {
            log.debug("group attribute found: {}", group_attribute);
            String group = (String) samlAttributes.get(group_attribute).get(0);
            log.debug("user group: {}", group);

            if (attributeMap.containsKey("admin_role") && group.equals(attributeMap.get("admin_role"))) {
                // We need to have both roles assigned, so admin can open an admin session and a user session
                userRoles.add(secRoleRepository.getAdmin());
                userRoles.add(secRoleRepository.getUser());
            } else if (attributeMap.containsKey("user_role") && group.equals(attributeMap.get("user_role"))) {
                userRoles.add(secRoleRepository.getUser());
            } else if (attributeMap.containsKey("guest_role") && group.equals(attributeMap.get("guest_role"))) {
                userRoles.add(secRoleRepository.getGuest());
            } else {
                throw new NoSuchFieldException("user group doesn't match our mapped user roles");
            }
        } else {
            throw new NoSuchFieldException("no group attribute found for group: " + group_attribute);
        }


        for (SecRole userRole : userRoles) {
            SecUserSecRole secUsersecRole = new SecUserSecRole();
            secUsersecRole.setSecUser(user);
            secUsersecRole.setSecRole(userRole);
            entityManager.persist(secUsersecRole);
            entityManager.flush();
        }
        entityManager.refresh(user);
        User finalUser = userRepository.findByUsernameLikeIgnoreCase(user.getUsername()).get();
        log.debug("user roles: {} assigned to user: {}", finalUser.getRoles().toString(), finalUser.getUsername());

        if (finalUser.getRoles().stream().anyMatch(userRole -> userRole.getAuthority().equals("ROLE_ADMIN") || userRole.getAuthority().equals("ROLE_USER"))) {
            log.debug("creating storage for user: {}", finalUser.getUsername());
            SecurityUtils.doWithAuth(applicationContext, "admin", () -> storageService.createStorage(finalUser));
        }

        return user;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return null;
    }
}
