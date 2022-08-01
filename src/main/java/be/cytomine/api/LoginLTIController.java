package be.cytomine.api;

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

import be.cytomine.api.controller.RestCytomineController;
import be.cytomine.config.properties.ApplicationProperties;
import be.cytomine.config.properties.LtiConsumerProperties;
import be.cytomine.config.properties.LtiProperties;
import be.cytomine.config.security.JWTFilter;
import be.cytomine.domain.security.*;
import be.cytomine.dto.LoginVM;
import be.cytomine.exceptions.ForbiddenException;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.repository.security.AuthWithTokenRepository;
import be.cytomine.repository.security.ForgotPasswordTokenRepository;
import be.cytomine.repository.security.SecRoleRepository;
import be.cytomine.repository.security.UserRepository;
import be.cytomine.security.CustomUserDetailsAuthenticationProvider;
import be.cytomine.security.jwt.TokenProvider;
import be.cytomine.security.jwt.TokenType;
import be.cytomine.security.ldap.LdapIdentityAuthenticationProvider;
import be.cytomine.service.image.server.StorageService;
import be.cytomine.service.project.ProjectService;
import be.cytomine.service.security.SecUserService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.service.utils.NotificationService;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.SecurityUtils;
import be.cytomine.utils.StringUtils;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.imsglobal.lti.launch.LtiOauthVerifier;
import org.imsglobal.lti.launch.LtiVerificationException;
import org.imsglobal.lti.launch.LtiVerificationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import javax.mail.MessagingException;
import javax.persistence.EntityManager;
import javax.validation.Valid;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("")
@AllArgsConstructor
public class LoginLTIController extends RestCytomineController {

    private final UserRepository userRepository;

    private final Environment env;

    private final LtiProperties ltiProperties;

    private final ApplicationContext applicationContext;

    private final ApplicationProperties applicationProperties;

    private final SecRoleRepository secRoleRepository;

    private final EntityManager entityManager;

    private final StorageService storageService;

    private final SecUserSecRole secUserSecRole;

    private final SecUserService secUserService;

    private final ProjectService projectService;

//            "/login/loginWithLTI" (controller: "login") {
//        action = [POST:"loginWithLTI"]
//    }

    @PostMapping("/login/loginWithLTI")
    public RedirectView authorize(

    ) throws IOException, LtiVerificationException {
        JsonObject params = super.mergeQueryParamsAndBodyParams();

        String consumerName = params.getJSONAttrStr("tool_consumer_instance_name");
        log.info("loginWithLTI by {}", consumerName);

        String aAuthConsumerKey = params.getJSONAttrStr("oauth_consumer_key");
        log.info("oauth_consumer_key = {}", aAuthConsumerKey);

        LtiConsumerProperties consumer = ltiProperties.getConsumers().stream().filter(x -> x.getKey().equals(aAuthConsumerKey)).findFirst()
                .orElseThrow(() -> new WrongArgumentException("Untrusted LTI Consumer"));

        String privateKey = consumer.getSecret();

        log.info("lti version : = {}", params.getJSONAttrStr("lti_version"));
        log.info("oauth_version = {}", params.getJSONAttrStr("oauth_version"));

        // check LTI/Oauth validity
        LtiOauthVerifier ltiOauthVerifier = new LtiOauthVerifier();
        LtiVerificationResult ltiResult = ltiOauthVerifier.verify(request, privateKey);

        if (!ltiResult.getSuccess()) {
            throw new WrongArgumentException("LTI verification failed");
        }

        String username = consumer.getUsernameParameter()!=null ? params.getJSONAttrStr(consumer.getUsernameParameter())
                : params.getJSONAttrStr("lis_person_sourcedid");

        if(username == null || username.isEmpty()) {
            throw new WrongArgumentException("username parameter, "+ StringUtils.getBlankIfNull(consumer.getUsernameParameter())+", is empty.");
        }

        String firstname = params.getJSONAttrStr("lis_person_name_given", username);
        String lastname = params.getJSONAttrStr("lis_person_name_family", consumer.getName());

        //if valid, check if all the need value are set
        if(firstname==null || lastname==null) {
            throw new WrongArgumentException("Not enough information for LTI connexion. Parameters are " + params.toJsonString());
        }

        if(params.isMissing("lis_person_contact_email_primary")) {
            throw new WrongArgumentException("Email not found. Parameters are : " + params.toJsonString());
        }

        // TODO: check expiration
//        def df = "dd.MM.yyyy HH:mm:ss,S"
//        def dateTime1 = new Date()
//        def dateTime2 = new Date().parse(df, "01.09.2022 00:00:00,000000000")
//        if(dateTime1 > dateTime2){
//            String expiredLicense = "Licence for LTI Cytomine's corporate module is expired and the LTI feature are deactivated.\nContact Cytomine company or use another authentication way."
//            log.error expiredLicense
//            response([success: false, message: expiredLicense], 400)
//            return
//        } else {
//            log.info "LTI Cytomine's corporate module still active until $dateTime2"
//        }

        final List<String> roles = !params.isMissing("roles") ? Arrays.asList(params.getJSONAttrStr("roles").split(",")) : new ArrayList<>();

        String email = params.getJSONAttrStr("lis_person_contact_email_primary");

        log.info("loginWithLTI :{} {} {} {}", firstname, lastname, email, roles);

        Optional<User> optionalUser = userRepository.findByUsernameLikeIgnoreCase(username);

        if(optionalUser.isEmpty() && StringUtils.isBlank(email)) {
            throw new WrongArgumentException("Not enough information to create a LTI profil");
        }


        final String redirection = params.getJSONAttrStr("custom_redirect", "/");

        SecurityUtils.doWithAuth(applicationContext, "superadmin", () -> {
            User userFromLti;
            if (optionalUser.isEmpty()) {
                log.info("LTI connexion. Create new user " + username);

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
            } else {
                userFromLti = optionalUser.get();
            }

            SecRole userRole = secRoleRepository.getUser();

            if (roles.contains("Instructor") && !userFromLti.getRoles().contains(userRole)) {
                SecUserSecRole secUsersecRole = new SecUserSecRole();
                secUsersecRole.setSecUser(userFromLti);
                secUsersecRole.setSecRole(userRole);

                entityManager.persist(secUsersecRole);
                entityManager.flush();
                entityManager.refresh(userFromLti);

                userRepository.findByUsernameLikeIgnoreCase(userFromLti.getUsername()).get();
            }

            log.info("redirect to " + params.getJSONAttrStr("custom_redirect",""));

            if (!params.isMissing("custom_redirect")) {

                log.info("redirection wanted is {}", redirection);
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

                    Pattern p1 = Pattern.compile("/#\\/project\\/\\d+\\/images/");//. represents single character
                    Matcher m1 = p1.matcher(redirection);

                    if (m1.matches()) {
                        matched = m1.group(0);
                        projectId = Long.parseLong(matched.split("#/project/")[1].split("/images")[0]);
                    }

                    Pattern p2 = Pattern.compile("/#\\/project\\/\\d+\\/image\\/\\d+/");//. represents single character
                    Matcher m2 = p2.matcher(redirection);

                    if (m2.matches()) {
                        matched = m2.group(0);
                        projectId = Long.parseLong(matched.split("#/project/")[1].split("/image/")[0]);
                    }
                }
                if (projectId != null) {
                    log.info("Project id = " + projectId);
                    if (roles.contains("Instructor")) {
                        secUserService.addUserToProject(userFromLti, projectService.get(projectId), true);
                    } else {
                        secUserService.addUserToProject(userFromLti, projectService.get(projectId), false);
                    }
                }
            }
        });



        SecurityUtils.reauthenticate(applicationContext, username, null);

        ///////////////////////////////////////////////////////////////

        TODO: WRITE TESTS + MOCK

        return new RedirectView(redirection);
    }

//    def loginWithLTI() {



//    }
}
