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
import be.cytomine.exceptions.AuthenticationException;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.lang.reflect.Field;
import java.util.*;

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
        if(attributes == null || attributes.isEmpty())
            throw new UsernameNotFoundException("User not known from Identity Provider.");

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
        String username = getSAMLAttributeValue("username", attributes, attributeMap);
        if(username == null){
            log.error("No mapping found for attribute: username.");
            throw new UsernameNotFoundException("No mapping found for attribute: 'username'. The SAML attribute '" + attributeMap.get("username") + "' was not received from idP for that user. Make sure the mapping is done right in the SAML configuration attributeMappingFile. This file can be set in the SHIBBOLETH_ATTRIBUTE_MAPPING_FILE env var.");
        }
        log.debug("SAML username: {}", username);
        User user = userRepository.findByUsernameLikeIgnoreCase(username)
                .orElseGet(() -> {
                    try {
                        return createUserFromSamlResults(username, attributes, attributeMap, registrationId, password);
                    } catch (NoSuchFieldException nsfe){
                        // If the idP replies no group that matches our roles, DENY access.
                        throw new UsernameNotFoundException(nsfe.getMessage());
                    } catch (Exception e) {
                        // Other exception are runtime exceptions.
                        throw new RuntimeException(e);
                    }
                }); //User does not exists in our database

        if (!user.getEnabled()) {
            throw new DisabledException("Disabled user");
        }
        return createSpringSecurityUser(username, user);
    }

    private String getSAMLAttributeValue(String attribute, LinkedHashMap<String, List<Object>> samlAttributes, Map<String, String> attributeMap){
        try{
            return (String) samlAttributes.get(attributeMap.get(attribute)).get(0);
        } catch (Exception e){
            log.warn("No mapping for attribute '"+attribute+"'");
            return null;
        }
    }

    private boolean hasSAMLAttributeContaining(String groupAttribute, String groupName, LinkedHashMap<String, List<Object>> samlAttributes, Map<String, String> attributeMap){
        if(samlAttributes != null && attributeMap != null && groupAttribute != null && attributeMap.containsKey(groupAttribute)){
            String mappedGroupAttribute = attributeMap.get(groupAttribute);
            String mappedGroupName = attributeMap.get(groupName);
            log.debug("Looking for any value of attribute '" + mappedGroupAttribute + "' that contains the substring '" + mappedGroupName + "'");
            return samlAttributes.get(mappedGroupAttribute).stream().anyMatch(
                        groupValue -> ((String)groupValue).contains(mappedGroupName)
                    );
        } else {
            return false;
        }
    }

    public User createUserFromSamlResults(String username, LinkedHashMap<String, List<Object>> samlAttributes, Map<String, String> attributeMap, String registrationId, String password) throws NoSuchFieldException {

            if(username != null && !username.isBlank()) {
                log.debug("creating user from saml results with username: {}", username);
                String firstname = getSAMLAttributeValue("firstname", samlAttributes, attributeMap);
                String lastname = getSAMLAttributeValue("lastname", samlAttributes, attributeMap);
                String mail = getSAMLAttributeValue("mail", samlAttributes, attributeMap);

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


                    if (hasSAMLAttributeContaining("group", "admin_role", samlAttributes, attributeMap)) {
                        // We need to have both roles assigned, so admin can open an admin session and a user session
                        userRoles.add(secRoleRepository.getAdmin());
                        userRoles.add(secRoleRepository.getUser());
                        log.debug("Found. User has been promoted to ROLE_ADMIN and ROLE_USER.");
                    } else if (hasSAMLAttributeContaining("group", "user_role", samlAttributes, attributeMap)) {
                        userRoles.add(secRoleRepository.getUser());
                        log.debug("Found. User has been promoted toROLE_USER.");
                    } else if (hasSAMLAttributeContaining("group", "guest_role", samlAttributes, attributeMap)) {
                        userRoles.add(secRoleRepository.getGuest());
                        log.debug("Found. User has been promoted to ROLE_ADMIN and ROLE_GUEST.");
                    } else {
                        throw new NoSuchFieldException("user group doesn't match our mapped user roles");
                    }
                } else {
                    throw new NoSuchFieldException("No mapping found for attribute: 'group'. The SAML attribute '" + attributeMap.get("group") + "' was not received from idP for that user. Make sure the mapping is done right in the SAML configuration attributeMappingFile. This file can be set in the SHIBBOLETH_ATTRIBUTE_MAPPING_FILE env var.");
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
            } else {
                throw new RuntimeException("Cannot create user with an empty username.");
            }
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return null;
    }
}
