package be.cytomine.controller;

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
import be.cytomine.domain.security.ForgotPasswordToken;
import be.cytomine.domain.security.User;
import be.cytomine.dto.auth.LoginVM;
import be.cytomine.repository.security.ForgotPasswordTokenRepository;
import be.cytomine.repository.security.UserRepository;
import be.cytomine.config.security.JWTFilter;
import be.cytomine.security.jwt.TokenProvider;
import be.cytomine.utils.JsonObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
class LoginControllerTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TokenProvider tokenProvider;

    @Autowired
    private MockMvc loginControllerMockMvc;

    @Autowired
    private BasicInstanceBuilder builder;

    @Test
    @Transactional
    void authorize_valide_credentials() throws Exception {
        User user = new User();
        user.setUsername("user-jwt-controller");
        user.setEmail("user-jwt-controller@example.com");
        user.setEnabled(true);
        user.setPassword(passwordEncoder.encode("test"));
        user.generateKeys();
        user.setLastname("lastname");
        user.setFirstname("firstname");
        user.setOrigin("origin");
        userRepository.saveAndFlush(user);

        LoginVM login = new LoginVM();
        login.setUsername("user-jwt-controller");
        login.setPassword("test");
        loginControllerMockMvc
            .perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON).content(JsonObject.toJsonString(login)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isString())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(header().string("Authorization", not(nullValue())))
            .andExpect(header().string("Authorization", not(is(emptyString()))));
    }

    @Test
    @Transactional
    void authorize_remember_me() throws Exception {
        User user = new User();
        user.setUsername("user-jwt-controller-remember-me");
        user.setEmail("user-jwt-controller-remember-me@example.com");
        user.setEnabled(true);
        user.setPassword(passwordEncoder.encode("test"));
        user.generateKeys();
        user.setLastname("lastname");
        user.setFirstname("firstname");
        user.setOrigin("origin");
        userRepository.saveAndFlush(user);

        LoginVM login = new LoginVM();
        login.setUsername("user-jwt-controller-remember-me");
        login.setPassword("test");
        login.setRememberMe(true);
        loginControllerMockMvc
            .perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON).content(JsonObject.toJsonString(login)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isString())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(header().string("Authorization", not(nullValue())))
            .andExpect(header().string("Authorization", not(is(emptyString()))));
    }

    @Test
    @Transactional
    void authorize_fails_with_bad_password() throws Exception {
        User user = new User();
        user.setUsername("user-jwt-controller");
        user.setEmail("user-jwt-controller@example.com");
        user.setEnabled(true);
        user.setPassword(passwordEncoder.encode("test"));
        user.generateKeys();
        user.setLastname("lastname");
        user.setFirstname("firstname");
        user.setOrigin("origin");
        userRepository.saveAndFlush(user);

        LoginVM login = new LoginVM();
        login.setUsername("user-jwt-controller");
        login.setPassword("badPassword");
        loginControllerMockMvc
                .perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON).content(JsonObject.toJsonString(login)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(header().doesNotExist("Authorization"));
    }


    @Test
    void authorize_fails_with_bad_credential() throws Exception {
        LoginVM login = new LoginVM();
        login.setUsername("wrong-user");
        login.setPassword("wrong password");
        loginControllerMockMvc
            .perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON).content(JsonObject.toJsonString(login)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.token").doesNotExist())
            .andExpect(header().doesNotExist("Authorization"));
    }

    @Test
    @Transactional
    void authorize_fails_with_disabled_user() throws Exception {
        User user = new User();
        user.setUsername("user-disabled");
        user.setEmail("user-disabled@example.com");
        user.setEnabled(true);
        user.setPassword(passwordEncoder.encode("test"));
        user.generateKeys();
        user.setLastname("lastname");
        user.setFirstname("firstname");
        user.setOrigin("origin");
        user.setEnabled(false);
        userRepository.saveAndFlush(user);

        LoginVM login = new LoginVM();
        login.setUsername("user-disabled");
        login.setPassword("test");
        loginControllerMockMvc
                .perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON).content(JsonObject.toJsonString(login)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(header().doesNotExist("Authorization"));
    }


    @Test
    @Transactional
    void authorize_fails_with_locked_user() throws Exception {
        User user = new User();
        user.setUsername("user-locked");
        user.setEmail("user-locked@example.com");
        user.setEnabled(true);
        user.setPassword(passwordEncoder.encode("test"));
        user.generateKeys();
        user.setLastname("lastname");
        user.setFirstname("firstname");
        user.setOrigin("origin");
        user.setAccountLocked(true);
        userRepository.saveAndFlush(user);

        LoginVM login = new LoginVM();
        login.setUsername("user-locked");
        login.setPassword("test");
        loginControllerMockMvc
                .perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON).content(JsonObject.toJsonString(login)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(header().doesNotExist("Authorization"));
    }


    @Test
    @Transactional
    void authorize_fails_with_expired_user() throws Exception {
        User user = new User();
        user.setUsername("user-expired");
        user.setEmail("user-expired@example.com");
        user.setEnabled(true);
        user.setPassword(passwordEncoder.encode("test"));
        user.generateKeys();
        user.setLastname("lastname");
        user.setFirstname("firstname");
        user.setOrigin("origin");
        user.setAccountExpired(true);
        userRepository.saveAndFlush(user);

        LoginVM login = new LoginVM();
        login.setUsername("user-expired");
        login.setPassword("test");
        loginControllerMockMvc
                .perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON).content(JsonObject.toJsonString(login)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(header().doesNotExist("Authorization"));
    }

    @Test
    @Transactional
    void authorize_workflow() throws Exception {
        User user = new User();
        user.setUsername("user-jwt-controller-workflow");
        user.setEmail("user-jwt-controller-workflow@example.com");
        user.setEnabled(true);
        user.setPassword(passwordEncoder.encode("test"));
        user.generateKeys();
        user.setLastname("lastname");
        user.setFirstname("firstname");
        user.setOrigin("origin");
        userRepository.saveAndFlush(user);

        LoginVM login = new LoginVM();
        login.setUsername("user-jwt-controller-workflow");
        login.setPassword("test");
        MvcResult mvcResult = loginControllerMockMvc
                .perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON).content(JsonObject.toJsonString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(header().string("Authorization", not(nullValue())))
                .andExpect(header().string("Authorization", not(is(emptyString())))).andReturn();
        JsonObject jsonObject = JsonObject.toJsonObject(mvcResult.getResponse().getContentAsString());

        String token = jsonObject.getJSONAttrStr("token");
        assertThat(tokenProvider.validateToken(token)).isTrue();


        loginControllerMockMvc.perform(get("/api/user/current.json")
                        .header(JWTFilter.AUTHORIZATION_HEADER, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("user-jwt-controller-workflow"));
    }


    @Test
    @Transactional
    void authorize_workflow_locked_user() throws Exception {
        User user = new User();
        user.setUsername("user-locked-request");
        user.setEmail("user-locked-request@example.com");
        user.setEnabled(true);
        user.setPassword(passwordEncoder.encode("test"));
        user.generateKeys();
        user.setLastname("lastname");
        user.setFirstname("firstname");
        user.setOrigin("origin");
        user = userRepository.saveAndFlush(user);

        LoginVM login = new LoginVM();
        login.setUsername("user-locked-request");
        login.setPassword("test");
        MvcResult mvcResult = loginControllerMockMvc
                .perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON).content(JsonObject.toJsonString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(header().string("Authorization", not(nullValue())))
                .andExpect(header().string("Authorization", not(is(emptyString())))).andReturn();
        JsonObject jsonObject = JsonObject.toJsonObject(mvcResult.getResponse().getContentAsString());

        String token = jsonObject.getJSONAttrStr("token");
        assertThat(tokenProvider.validateToken(token)).isTrue();

        // locked user now...
        user.setAccountLocked(true);
        userRepository.save(user);

        loginControllerMockMvc.perform(get("/api/user/current.json")
                        .header(JWTFilter.AUTHORIZATION_HEADER, "Bearer " + token))
                .andExpect(status().isForbidden());
    }


    @Test
    @Transactional
    void authorize_workflow_user_expired() throws Exception {
        User user = new User();
        user.setUsername("user-expired-request");
        user.setEmail("user-expired-request@example.com");
        user.setEnabled(true);
        user.setPassword(passwordEncoder.encode("test"));
        user.generateKeys();
        user.setLastname("lastname");
        user.setFirstname("firstname");
        user.setOrigin("origin");
        user = userRepository.saveAndFlush(user);

        LoginVM login = new LoginVM();
        login.setUsername("user-expired-request");
        login.setPassword("test");
        MvcResult mvcResult = loginControllerMockMvc
                .perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON).content(JsonObject.toJsonString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(header().string("Authorization", not(nullValue())))
                .andExpect(header().string("Authorization", not(is(emptyString())))).andReturn();
        JsonObject jsonObject = JsonObject.toJsonObject(mvcResult.getResponse().getContentAsString());

        String token = jsonObject.getJSONAttrStr("token");
        assertThat(tokenProvider.validateToken(token)).isTrue();

        // expired user now...
        user.setAccountExpired(true);
        userRepository.save(user);

        loginControllerMockMvc.perform(get("/api/user/current.json")
                        .header(JWTFilter.AUTHORIZATION_HEADER, "Bearer " + token))
                .andExpect(status().isForbidden());
    }


    @Test
    @Transactional
    void authorize_workflow_user_disabled() throws Exception {
        User user = new User();
        user.setUsername("user-disabled-request");
        user.setEmail("user-disabled-request@example.com");
        user.setEnabled(true);
        user.setPassword(passwordEncoder.encode("test"));
        user.generateKeys();
        user.setLastname("lastname");
        user.setFirstname("firstname");
        user.setOrigin("origin");
        user = userRepository.saveAndFlush(user);

        LoginVM login = new LoginVM();
        login.setUsername("user-disabled-request");
        login.setPassword("test");
        MvcResult mvcResult = loginControllerMockMvc
                .perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON).content(JsonObject.toJsonString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(header().string("Authorization", not(nullValue())))
                .andExpect(header().string("Authorization", not(is(emptyString())))).andReturn();
        JsonObject jsonObject = JsonObject.toJsonObject(mvcResult.getResponse().getContentAsString());

        String token = jsonObject.getJSONAttrStr("token");
        assertThat(tokenProvider.validateToken(token)).isTrue();

        // expired user now...
        user.setEnabled(false);
        userRepository.save(user);

        loginControllerMockMvc.perform(get("/api/user/current.json")
                        .header(JWTFilter.AUTHORIZATION_HEADER, "Bearer " + token))
                .andExpect(status().isForbidden());
    }



    @Test
    @Transactional
    @WithMockUser("superadmin")
    void build_token() throws Exception {

        User user = builder.given_default_user();

        MvcResult mvcResult = loginControllerMockMvc
                .perform(post("/api/token.json").contentType(MediaType.APPLICATION_JSON)
                        .content(JsonObject.of("username", user.getUsername(), "validity", "60").toJsonString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.token").isNotEmpty()).andReturn();
        JsonObject jsonObject = JsonObject.toJsonObject(mvcResult.getResponse().getContentAsString());

        String token = jsonObject.getJSONAttrStr("token");


        loginControllerMockMvc.perform(post("/login/loginWithToken")
                        .param("username", user.getUsername()).param("tokenKey", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isString())
                    .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();
        jsonObject = JsonObject.toJsonObject(mvcResult.getResponse().getContentAsString());


        loginControllerMockMvc.perform(get("/api/user/current.json")
                        .header(JWTFilter.AUTHORIZATION_HEADER, "Bearer " + jsonObject.getJSONAttrStr("token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("user"));
    }

    @Autowired
    ForgotPasswordTokenRepository forgotPasswordTokenRepository;

    @Test
    @Transactional
    void forgot_password_token() throws Exception {

        User user = builder.given_default_user();

        forgotPasswordTokenRepository.deleteAll();

        loginControllerMockMvc
                    .perform(post("/login/forgotPassword").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .content("j_username=" + user.getUsername()));

        assertThat(forgotPasswordTokenRepository.findAll()).hasSize(1);

        ForgotPasswordToken token = forgotPasswordTokenRepository.findAll().get(0);

        MvcResult mvcResult = loginControllerMockMvc.perform(post("/login/loginWithToken")
                        .param("username", user.getUsername()).param("tokenKey", token.getTokenKey()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();
        JsonObject jsonObject = JsonObject.toJsonObject(mvcResult.getResponse().getContentAsString());

        loginControllerMockMvc.perform(get("/api/user/current.json")
                        .header(JWTFilter.AUTHORIZATION_HEADER, "Bearer " + jsonObject.getJSONAttrStr("token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("user"));
    }
}
