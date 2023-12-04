package be.cytomine.api.controller;

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
import be.cytomine.config.security.JWTFilter;
import be.cytomine.domain.meta.Configuration;
import be.cytomine.domain.security.ForgotPasswordToken;
import be.cytomine.domain.security.User;
import be.cytomine.dto.LoginVM;
import be.cytomine.repository.meta.ConfigurationRepository;
import be.cytomine.repository.security.ForgotPasswordTokenRepository;
import be.cytomine.repository.security.UserRepository;
import be.cytomine.security.jwt.TokenProvider;
import be.cytomine.service.meta.ConfigurationService;
import be.cytomine.utils.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
class LoginControllerLDAPTests {

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

    @Autowired
    private ConfigurationRepository configurationRepository;

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("application.authentication.ldap.enabled", () -> true);
    }

    @BeforeEach
    public void init() {
        configurationRepository.deleteAll();
        configurationRepository.findAll();
        builder.given_a_configuration(ConfigurationService.CONFIG_KEY_LDAP_SERVER, BasicInstanceBuilder.LDAP_URL);
        builder.given_a_configuration(ConfigurationService.CONFIG_KEY_LDAP_PRINCIPAL, "CN=admin,OU=users,DC=mtr,DC=com");
        builder.given_a_configuration(ConfigurationService.CONFIG_KEY_LDAP_PASSWORD, "itachi");
        builder.given_a_configuration(ConfigurationService.CONFIG_KEY_LDAP_SEARCH, "OU=users,DC=mtr,DC=com");
        builder.given_a_configuration(ConfigurationService.CONFIG_KEY_LDAP_ATTRIBUTES, "cn,sn,givenname,mail");
    }
    @Test
    @Transactional
    void authorize_valide_credentials_with_user_only_in_local_database() throws Exception {
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
    void authorize_valide_credentials_with_user_in_ldap_and_local_database() throws Exception {
        User user = new User();
        user.setUsername("jdoeLDAP");
        user.setEmail("user-jwt-controller@example.com");
        user.setEnabled(true);
        user.setPassword(passwordEncoder.encode("test"));
        user.generateKeys();
        user.setLastname("lastname");
        user.setFirstname("firstname");
        user.setOrigin("origin");
        userRepository.saveAndFlush(user);

        LoginVM login = new LoginVM();
        login.setUsername("jdoeLDAP");
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
    void authorize_valide_credentials_with_existing_user_not_in_local_database() throws Exception {
        LoginVM login = new LoginVM();
        login.setUsername("jdoeLDAP");
        login.setPassword("goodPassword");
        loginControllerMockMvc
                .perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON).content(JsonObject.toJsonString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(header().string("Authorization", not(nullValue())))
                .andExpect(header().string("Authorization", not(is(emptyString()))));
    }


    @Test
    void authorize_fails_with_bad_credential() throws Exception {
        LoginVM login = new LoginVM();
        login.setUsername("jdoeLDAP");
        login.setPassword("wrong password");
        loginControllerMockMvc
            .perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON).content(JsonObject.toJsonString(login)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.token").doesNotExist())
            .andExpect(header().doesNotExist("Authorization"));
    }

    @Test
    @Transactional
    void authorize_workflow() throws Exception {

        assertThat(userRepository.findByUsernameLikeIgnoreCase("jdoeLDAP")).isEmpty();

        LoginVM login = new LoginVM();
        login.setUsername("jdoeLDAP");
        login.setPassword("goodPassword");
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
                .andExpect(jsonPath("$.username").value("jdoeLDAP"));
    }

}
