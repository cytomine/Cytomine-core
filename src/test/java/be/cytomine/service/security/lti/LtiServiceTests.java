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

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.config.properties.LtiConsumerProperties;
import be.cytomine.config.properties.LtiProperties;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.SecRole;
import be.cytomine.domain.security.SecUserSecRole;
import be.cytomine.domain.security.User;
import be.cytomine.dto.LoginWithRedirection;
import be.cytomine.exceptions.AlreadyExistException;
import be.cytomine.exceptions.ForbiddenException;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.repository.image.server.StorageRepository;
import be.cytomine.repository.security.SecRoleRepository;
import be.cytomine.repository.security.SecUserSecRoleRepository;
import be.cytomine.repository.security.UserRepository;
import be.cytomine.service.CommandService;
import be.cytomine.service.PermissionService;
import be.cytomine.service.command.TransactionService;
import be.cytomine.service.security.SecUserSecRoleService;
import be.cytomine.service.security.SecUserService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.JsonObject;
import org.imsglobal.lti.launch.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.test.context.support.WithMockUser;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_SUPER_ADMIN", username = "superadmin")
@Transactional
public class LtiServiceTests {

    @Autowired
    LtiService ltiService;

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    EntityManager entityManager;

    @Autowired
    StorageRepository storageRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    SecUserService secUserService;


    private final static LtiConsumerProperties LTI_CONSUMER_PROPERTIES =
            new LtiConsumerProperties("consumerKey", "consumerName", "consumerSecret", "consumerUsernameParameter");

    private final static LtiProperties LTI_PROPERTIES = new LtiProperties(true, List.of(LTI_CONSUMER_PROPERTIES));

    JsonObject given_a_lti_request_params() {
        String username = BasicInstanceBuilder.randomString();
        JsonObject params = new JsonObject();
        params.put("tool_consumer_instance_name", "consumerName");
        params.put("oauth_consumer_key", "consumerKey");
        params.put("lti_version", "1.2.3");
        params.put("oauth_version", "2");
        params.put("consumerUsernameParameter", username);
        params.put("lis_person_contact_email_primary", username+"@email.com");
        params.put("lis_person_name_given", "userFirstName");
        params.put("lis_person_name_family", "userLastName");
        return params;
    }

    LtiOauthVerifier given_a_lti_verifier(boolean responseSuccess, HttpServletRequest request) throws LtiVerificationException {

        LtiVerificationResult ltiVerificationResult = Mockito.mock(LtiVerificationResult.class);
        Mockito.when(ltiVerificationResult.getSuccess()).thenReturn(responseSuccess);
        Mockito.when(ltiVerificationResult.getError()).thenReturn(Mockito.mock(LtiError.class));

        LtiOauthVerifier ltiOauthVerifier = Mockito.mock(LtiOauthVerifier.class);
        Mockito.when(ltiOauthVerifier.verify(request, "consumerSecret")).thenReturn(ltiVerificationResult);
        return ltiOauthVerifier;
    }


    @Test
    void lti_verification_user_not_exists_ok() throws LtiVerificationException {

        JsonObject params = given_a_lti_request_params();
        String username = params.getJSONAttrStr("consumerUsernameParameter");

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        LtiOauthVerifier ltiOauthVerifier = given_a_lti_verifier(true, request);


        LoginWithRedirection redirection =
                ltiService.verifyAndRedirect(params, request, LTI_PROPERTIES, ltiOauthVerifier);


        assertThat(redirection.getRedirection()).isEqualTo("/");

        // user has been created
        User user = userRepository.findByUsernameLikeIgnoreCase(username)
                .orElseThrow(() -> new ObjectNotFoundException("User", username));
        assertThat(user.getLastname()).isEqualTo("userLastName");
        assertThat(user.getFirstname()).isEqualTo("userFirstName");
        assertThat(user.getRoles().stream().map(x -> x.getAuthority())).hasSize(1).contains("ROLE_GUEST");
    }


    @Test
    void lti_verification_user_already_exists() throws LtiVerificationException {

        JsonObject params = given_a_lti_request_params();
        String username = params.getJSONAttrStr("consumerUsernameParameter");

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        LtiOauthVerifier ltiOauthVerifier = given_a_lti_verifier(true, request);
        User userInDatabase = builder.given_a_user(username);

        LoginWithRedirection redirection =
                ltiService.verifyAndRedirect(params, request, LTI_PROPERTIES, ltiOauthVerifier);


        assertThat(redirection.getRedirection()).isEqualTo("/");

        // user has been created
        User user = userRepository.findByUsernameLikeIgnoreCase(username)
                .orElseThrow(() -> new ObjectNotFoundException("User", username));
        assertThat(user.getLastname()).isEqualTo(userInDatabase.getLastname());
        assertThat(user.getFirstname()).isEqualTo(userInDatabase.getFirstname());
        assertThat(user.getEmail()).isEqualTo(userInDatabase.getEmail());
        assertThat(user.getRoles().stream().map(x -> x.getAuthority())).contains("ROLE_USER");
    }


    @Test
    void lti_untrusted_consumer_is_refused() throws LtiVerificationException {
        JsonObject params = given_a_lti_request_params();
        params.put("oauth_consumer_key", "WRONG");

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        LtiOauthVerifier ltiOauthVerifier = given_a_lti_verifier(true, request);

        Assertions.assertThrows(WrongArgumentException.class,
                () -> {ltiService.verifyAndRedirect(params, request, LTI_PROPERTIES, ltiOauthVerifier);},
                 "Untrusted LTI Consumer"
        );
    }


    @Test
    void lti_consumer_with_no_private_key_is_refused() throws LtiVerificationException {
        JsonObject params = given_a_lti_request_params();

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        LtiOauthVerifier ltiOauthVerifier = given_a_lti_verifier(true, request);

        LtiConsumerProperties properties =
                new LtiConsumerProperties("consumerKey", "consumerName", null, "consumerUsernameParameter");

        LtiProperties propertiesWithConsumerPrivateKeyNull = new LtiProperties(true, List.of(properties));


        Assertions.assertThrows(WrongArgumentException.class,
                () -> {ltiService.verifyAndRedirect(params, request, propertiesWithConsumerPrivateKeyNull, ltiOauthVerifier);},
                "Untrusted LTI Consumer, no private key"
        );
    }

    @Test
    void lti_verification_failed() throws LtiVerificationException {
        JsonObject params = given_a_lti_request_params();

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        LtiOauthVerifier ltiOauthVerifier = given_a_lti_verifier(false, request);

        Assertions.assertThrows(WrongArgumentException.class,
                () -> {ltiService.verifyAndRedirect(params, request, LTI_PROPERTIES, ltiOauthVerifier);},
                "LTI verification failed"
        );
    }

    @Test
    void lti_verification_username_extracted_from_lis_person_sourcedid_param() throws LtiVerificationException {
        String username = BasicInstanceBuilder.randomString();
        JsonObject params = given_a_lti_request_params();
        params.withChange("lis_person_sourcedid", username);

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        LtiOauthVerifier ltiOauthVerifier = given_a_lti_verifier(true, request);


        LtiConsumerProperties properties =
                new LtiConsumerProperties("consumerKey", "consumerName", "consumerSecret", null);

        LtiProperties ltiProperties = new LtiProperties(true, List.of(properties));


        LoginWithRedirection redirection =
                ltiService.verifyAndRedirect(params, request, ltiProperties, ltiOauthVerifier);

        assertThat(redirection.getRedirection()).isEqualTo("/");

        // user has been created
        User user = userRepository.findByUsernameLikeIgnoreCase(username)
                .orElseThrow(() -> new ObjectNotFoundException("User", username));
        assertThat(user.getLastname()).isEqualTo("userLastName");
        assertThat(user.getFirstname()).isEqualTo("userFirstName");
        assertThat(user.getRoles().stream().map(x -> x.getAuthority())).hasSize(1).contains("ROLE_GUEST");
    }

    @Test
    void lit_verification_with_no_username_refused() throws LtiVerificationException {
        JsonObject params = given_a_lti_request_params();
        params.remove("consumerUsernameParameter");

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        LtiOauthVerifier ltiOauthVerifier = given_a_lti_verifier(true, request);

        Assertions.assertThrows(WrongArgumentException.class,
                () -> {ltiService.verifyAndRedirect(params, request, LTI_PROPERTIES, ltiOauthVerifier);},
                "username parameter is empty."
        );
    }

    @Test
    void lit_verification_with_no_email_refused() throws LtiVerificationException {
        JsonObject params = given_a_lti_request_params();
        params.remove("lis_person_contact_email_primary");

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        LtiOauthVerifier ltiOauthVerifier = given_a_lti_verifier(true, request);

        Assertions.assertThrows(WrongArgumentException.class,
                () -> {ltiService.verifyAndRedirect(params, request, LTI_PROPERTIES, ltiOauthVerifier);},
                "Email not found."
        );
    }

    @Test
    void lti_verification_user_not_in_project_add_user_in_this_project() throws LtiVerificationException {
        JsonObject params = given_a_lti_request_params();
        String username = params.getJSONAttrStr("consumerUsernameParameter");

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        LtiOauthVerifier ltiOauthVerifier = given_a_lti_verifier(true, request);
        User userInDatabase = builder.given_a_user(username);

        Project project = builder.given_a_project();
        params.put("custom_redirect", "#/project/"+project.getId()+"/images");

        LoginWithRedirection redirection =
                ltiService.verifyAndRedirect(params, request, LTI_PROPERTIES, ltiOauthVerifier);

        assertThat(redirection.getRedirection()).isEqualTo("#/project/"+project.getId()+"/images");

        // user has been created
        User user = userRepository.findByUsernameLikeIgnoreCase(username)
                .orElseThrow(() -> new ObjectNotFoundException("User", username));
        assertThat(secUserService.listUsers(project)).contains(user);
        assertThat(secUserService.listAdmins(project)).doesNotContain(user);
    }

    @Test
    void lti_verification_user_already_in_project_has_no_effect() throws LtiVerificationException {
        JsonObject params = given_a_lti_request_params();
        String username = params.getJSONAttrStr("consumerUsernameParameter");

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        LtiOauthVerifier ltiOauthVerifier = given_a_lti_verifier(true, request);
        User userInDatabase = builder.given_a_user(username);

        Project project = builder.given_a_project();
        builder.addUserToProject(project, username);

        params.put("custom_redirect", "#/project/"+project.getId()+"/images");

        LoginWithRedirection redirection =
                ltiService.verifyAndRedirect(params, request, LTI_PROPERTIES, ltiOauthVerifier);

        assertThat(redirection.getRedirection()).isEqualTo("#/project/"+project.getId()+"/images");

        // user has been created
        User user = userRepository.findByUsernameLikeIgnoreCase(username)
                .orElseThrow(() -> new ObjectNotFoundException("User", username));
        assertThat(secUserService.listUsers(project)).contains(user);
        assertThat(secUserService.listAdmins(project)).contains(user);
    }

    @Test
    void lti_verification_user_is_instructor() throws LtiVerificationException {

        JsonObject params = given_a_lti_request_params();
        params.put("roles", "Instructor");
        String username = params.getJSONAttrStr("consumerUsernameParameter");

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        LtiOauthVerifier ltiOauthVerifier = given_a_lti_verifier(true, request);


        LoginWithRedirection redirection =
                ltiService.verifyAndRedirect(params, request, LTI_PROPERTIES, ltiOauthVerifier);


        assertThat(redirection.getRedirection()).isEqualTo("/");

        // user has been created
        User user = userRepository.findByUsernameLikeIgnoreCase(username)
                .orElseThrow(() -> new ObjectNotFoundException("User", username));
        assertThat(user.getRoles().stream().map(x -> x.getAuthority())).contains("ROLE_USER");
    }

    @Test
    void lti_verification_user_is_instructor_add_user_in_project_as_admin() throws LtiVerificationException {
        JsonObject params = given_a_lti_request_params();
        params.put("roles", "Instructor");
        String username = params.getJSONAttrStr("consumerUsernameParameter");

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        LtiOauthVerifier ltiOauthVerifier = given_a_lti_verifier(true, request);
        User userInDatabase = builder.given_a_user(username);

        Project project = builder.given_a_project();
        params.put("custom_redirect", "#/project/"+project.getId()+"/images");

        LoginWithRedirection redirection =
                ltiService.verifyAndRedirect(params, request, LTI_PROPERTIES, ltiOauthVerifier);

        assertThat(redirection.getRedirection()).isEqualTo( "#/project/"+project.getId()+"/images");

        // user has been created
        User user = userRepository.findByUsernameLikeIgnoreCase(username)
                .orElseThrow(() -> new ObjectNotFoundException("User", username));
        assertThat(secUserService.listAdmins(project)).contains(user);
    }


    @Test
    void create_user_role_if_user_is_instructor() {
        User user = builder.given_a_guest();

        assertThat(user.getRoles().stream().map(SecRole::getAuthority)).containsExactly("ROLE_GUEST");
        ltiService.createUserRoleIfUserIsInstructor(List.of("Instructor"), user);
        assertThat(user.getRoles().stream().map(SecRole::getAuthority)).containsExactlyInAnyOrder("ROLE_GUEST", "ROLE_USER");
    }

    @Test
    void ignore_user_role_if_instructor_is_has_already_user_role() {
        User user = builder.given_a_user();

        assertThat(user.getRoles().stream().map(SecRole::getAuthority)).containsExactlyInAnyOrder( "ROLE_USER");
        ltiService.createUserRoleIfUserIsInstructor(List.of("Instructor"), user);
        assertThat(user.getRoles().stream().map(SecRole::getAuthority)).containsExactlyInAnyOrder("ROLE_USER");
    }

    @Test
    void do_not_add_user_role_if_user_is_not_instructor() {
        User user = builder.given_a_guest();

        assertThat(user.getRoles().stream().map(SecRole::getAuthority)).containsExactly("ROLE_GUEST");
        ltiService.createUserRoleIfUserIsInstructor(List.of("NotInstructor"), user);
        assertThat(user.getRoles().stream().map(SecRole::getAuthority)).containsExactly("ROLE_GUEST");
    }


    @Test
    void create_unexisting_user_with_guest_role() {

        String username = BasicInstanceBuilder.randomString();
        String firstname = BasicInstanceBuilder.randomString();
        String lastname = BasicInstanceBuilder.randomString();
        String email = BasicInstanceBuilder.randomString() + "@email.com";

        User user = ltiService
                .createUnexistingUser(username, firstname, lastname, email);

        assertThat(user).isNotNull();
        assertThat(user.getUsername()).isEqualTo(username);
        assertThat(user.getFirstname()).isEqualTo(firstname);
        assertThat(user.getLastname()).isEqualTo(lastname);
        assertThat(user.getEmail()).isEqualTo(email);
        assertThat(user.getOrigin()).isEqualTo("LTI");
        assertThat(user.getPassword()).hasSize(UUID.randomUUID().toString().length());
        assertThat(user.getRoles().stream().map(SecRole::getAuthority)).containsExactly("ROLE_GUEST");

        assertThat(storageRepository.findAllByUser(user)).hasSize(1);
    }


    @Test
    void create_unexisting_user_with_existing_username_is_refused() {
        User user = builder.given_a_user();
        Assertions.assertThrows(AlreadyExistException.class, () -> {
            ltiService
                    .createUnexistingUser(user.getUsername(), user.getFirstname(), user.getLastname(), BasicInstanceBuilder.randomString()+"@email.com");
        });
    }

    @Test
    void extract_project_id() {
        assertThat(LtiService.extractProjectId("/tabs-image-42-1")).isEqualTo(42);
        assertThat(LtiService.extractProjectId("/tabs-images-42")).isEqualTo(42);
        assertThat(LtiService.extractProjectId("/tabs-dashboard-42")).isEqualTo(42);
        assertThat(LtiService.extractProjectId("/tabs-annotations-42")).isEqualTo(42);
        assertThat(LtiService.extractProjectId("/tabs-dashboard-42")).isEqualTo(42);
        assertThat(LtiService.extractProjectId("/tabs-annotationproperties-42-5")).isEqualTo(42);
        assertThat(LtiService.extractProjectId("/tabs-config-42")).isEqualTo(42);
        assertThat(LtiService.extractProjectId("/tabs-usersconfig-42")).isEqualTo(42);

        assertThat(LtiService.extractProjectId("#/project/42/images")).isEqualTo(42);
        assertThat(LtiService.extractProjectId("#/project/42/image/123")).isEqualTo(42);
    }


}
