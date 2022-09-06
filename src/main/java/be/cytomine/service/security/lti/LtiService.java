package be.cytomine.service.security.lti;

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
import be.cytomine.config.properties.LtiConsumerProperties;
import be.cytomine.config.properties.LtiProperties;
import be.cytomine.domain.security.*;
import be.cytomine.exceptions.*;
import be.cytomine.repository.security.*;
import be.cytomine.service.image.server.StorageService;
import be.cytomine.service.project.ProjectService;
import be.cytomine.service.security.SecUserService;
import be.cytomine.utils.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.imsglobal.lti.launch.LtiOauthVerifier;
import org.imsglobal.lti.launch.LtiVerificationException;
import org.imsglobal.lti.launch.LtiVerificationResult;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@Transactional
@AllArgsConstructor
public class LtiService {

    private final UserRepository userRepository;

    private final Environment env;

    private final ApplicationContext applicationContext;

    private final ApplicationProperties applicationProperties;

    private final SecRoleRepository secRoleRepository;

    private final EntityManager entityManager;

    private final StorageService storageService;

    private final SecUserService secUserService;

    private final ProjectService projectService;

    public String verifyAndRedirect(JsonObject params, HttpServletRequest request, LtiProperties ltiProperties, LtiOauthVerifier ltiOauthVerifier) throws LtiVerificationException {
        String consumerName = params.getJSONAttrStr("tool_consumer_instance_name");
        log.info("loginWithLTI by {}", consumerName);

        String aAuthConsumerKey = params.getJSONAttrStr("oauth_consumer_key");
        log.debug("oauth_consumer_key = {}", aAuthConsumerKey);

        LtiConsumerProperties consumer = ltiProperties.getConsumers().stream().filter(x -> x.getKey().equals(aAuthConsumerKey)).findFirst()
                .orElseThrow(() -> new WrongArgumentException("Untrusted LTI Consumer"));

        String privateKey = consumer.getSecret();

        if (privateKey==null) {
            throw new WrongArgumentException("Untrusted LTI Consumer, no private key");
        }

        log.debug("lti version : = {}", params.getJSONAttrStr("lti_version"));
        log.debug("oauth_version = {}", params.getJSONAttrStr("oauth_version"));

        // check LTI/Oauth validity
        LtiVerificationResult ltiResult = ltiOauthVerifier.verify(request, privateKey);

        if (!ltiResult.getSuccess()) {
            throw new WrongArgumentException("LTI verification failed");
        }

        String username = consumer.getUsernameParameter()!=null ? params.getJSONAttrStr(consumer.getUsernameParameter())
                : params.getJSONAttrStr("lis_person_sourcedid");

        if(username == null || username.isEmpty()) {
            throw new WrongArgumentException("username parameter is empty.");
        }

        String firstname = params.getJSONAttrStr("lis_person_name_given", username);
        String lastname = params.getJSONAttrStr("lis_person_name_family", consumer.getName());

        //if valid, check if all the need value are set
        if(firstname==null || lastname==null) {
            throw new WrongArgumentException("Not enough information for LTI connexion. Parameters are " + params.toJsonString());
        }

        if(params.isMissing("lis_person_contact_email_primary")) {
            throw new WrongArgumentException("Email not found.");
        }

        final List<String> roles = !params.isMissing("roles") ? Arrays.asList(params.getJSONAttrStr("roles").split(",")) : new ArrayList<>();

        String email = params.getJSONAttrStr("lis_person_contact_email_primary");

        log.info("loginWithLTI :{} {} {} {}", firstname, lastname, email, roles);

        Optional<User> optionalUser = userRepository.findByUsernameLikeIgnoreCase(username);

        if(optionalUser.isEmpty() && StringUtils.isBlank(email)) {
            throw new WrongArgumentException("Not enough information to create a LTI profil");
        }


        final String redirection = params.getJSONAttrStr("custom_redirect", "/");

        try {
            SecurityUtils.doWithAuth(applicationContext, "superadmin", () -> {
                User userFromLti;
                if (optionalUser.isEmpty()) {
                    log.info("LTI connexion. Create new user " + username);
                    userFromLti = createUnexistingUser(username, firstname, lastname, email);
                } else {
                    userFromLti = optionalUser.get();
                }

                createUserRoleIfUserIsInstructor(roles, userFromLti);
                log.info("redirect to " + params.getJSONAttrStr("custom_redirect",""));

                if (!params.isMissing("custom_redirect")) {
                    log.debug("redirection wanted is {}", redirection);
                    Long projectId = extractProjectId(redirection);
                    if (projectId != null) {
                        log.info("Project id = " + projectId);
                        secUserService.addUserToProject(userFromLti, projectService.get(projectId), roles.contains("Instructor"));
                    }
                }
            });
        } finally {
            SecurityUtils.reauthenticate(applicationContext, username, null);
        }
        return redirection;
    }

    void createUserRoleIfUserIsInstructor(List<String> roles, User userFromLti) {
        SecRole ROLE_USER = secRoleRepository.getUser();
        if (roles.contains("Instructor") && !userFromLti.getRoles().contains(ROLE_USER)) {
            SecUserSecRole secUsersecRole = new SecUserSecRole();
            secUsersecRole.setSecUser(userFromLti);
            secUsersecRole.setSecRole(ROLE_USER);

            entityManager.persist(secUsersecRole);
            entityManager.flush();
            entityManager.refresh(userFromLti);

            userRepository.findByUsernameLikeIgnoreCase(userFromLti.getUsername()).get();
        }
    }

    User createUnexistingUser(String username, String firstname, String lastname, String email) {
        if (userRepository.findByUsernameLikeIgnoreCase(username).isPresent()) {
            throw new AlreadyExistException("User with username " + username);
        }

        User userFromLti;
        User user = new User();
        user.setUsername(username);
        user.setLastname(lastname);
        user.setFirstname(firstname);
        user.setEmail(email);
        user.setEnabled(true);
        user.setPassword(UUID.randomUUID().toString());
        user.setOrigin("LTI");
        user.setLanguage(Language.valueOf(applicationProperties.getDefaultLanguage()));
        user.generateKeys();

        user = userRepository.save(user);

        SecRole guestRole = secRoleRepository.getGuest();

        SecUserSecRole secUsersecRole = new SecUserSecRole();
        secUsersecRole.setSecUser(user);
        secUsersecRole.setSecRole(guestRole);

        entityManager.persist(secUsersecRole);
        entityManager.flush();
        entityManager.refresh(user);

        User finalUser = userRepository.findByUsernameLikeIgnoreCase(user.getUsername()).get();

        SecurityUtils.doWithAuth(applicationContext, "admin", () -> storageService.createStorage(finalUser));

        userFromLti = finalUser;
        return userFromLti;
    }

    static Long extractProjectId(String redirection) {
        Long projectId = null;
        if (redirection.contains("tabs-image-")) {
            projectId = Long.parseLong(redirection.split("tabs-image-")[1].split("-")[0]);
        } else if (redirection.contains("tabs-images-")) {
            projectId = Long.parseLong(redirection.split("tabs-images-")[1]);
        } else if (redirection.contains("tabs-dashboard-")) {
            projectId = Long.parseLong(redirection.split("tabs-dashboard-")[1]);
        } else if (redirection.contains("tabs-annotations-")) {
            projectId = Long.parseLong(redirection.split("tabs-annotations-")[1]);
        } else if (redirection.contains("tabs-annotationproperties-")) {
            projectId = Long.parseLong(redirection.split("tabs-annotationproperties-")[1].split("-")[0]);
        } else if (redirection.contains("tabs-config-")) {
            projectId = Long.parseLong(redirection.split("tabs-config-")[1]);
        } else if (redirection.contains("tabs-usersconfig-")) {
            projectId = Long.parseLong(redirection.split("tabs-usersconfig-")[1]);
        } else {

            String matched;

            Pattern p1 = Pattern.compile("#\\/project\\/\\d+\\/images");//. represents single character
            Matcher m1 = p1.matcher(redirection);

            if (m1.matches()) {
                matched = m1.group(0);
                projectId = Long.parseLong(matched.split("#/project/")[1].split("/images")[0]);
            }

            Pattern p2 = Pattern.compile("#\\/project\\/\\d+\\/image\\/\\d+");//. represents single character
            Matcher m2 = p2.matcher(redirection);

            if (m2.matches()) {
                matched = m2.group(0);
                projectId = Long.parseLong(matched.split("#/project/")[1].split("/image/")[0]);
            }
        }
        return projectId;
    }
}
