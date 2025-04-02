package be.cytomine.controller.security;

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
import be.cytomine.domain.security.User;
import be.cytomine.security.jwt.TokenProvider;
import be.cytomine.security.jwt.TokenType;
import be.cytomine.service.CurrentRoleService;
import be.cytomine.utils.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
class SwitchUserControllerTests {

    @Autowired
    private TokenProvider tokenProvider;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BasicInstanceBuilder builder;

    @Autowired
    private CurrentRoleService currentRoleService;

    @AfterEach
    public void closeAdminSession() {
        currentRoleService.clearAllAdminSession();
    }

    @Test
    @Transactional
    void switch_user_workflow() throws Exception {
        User admin = builder.given_default_admin();
        User user = builder.given_default_user();

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "admin",
                "test-password",
                List.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        String jwt = tokenProvider.createToken(authentication, TokenType.SESSION);

        mockMvc.perform(get("/api/user/current.json")
                        .header(JWTFilter.AUTHORIZATION_HEADER, "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.isSwitched").value(false))
                .andExpect(jsonPath("$.realUser").doesNotHaveJsonPath());

        mockMvc.perform(get("/session/admin/open.json")
                .header(JWTFilter.AUTHORIZATION_HEADER, "Bearer " + jwt))
                .andExpect(status().isOk());

        MvcResult mvcResult = mockMvc
                .perform(post("/api/login/impersonate")
                        .header(JWTFilter.AUTHORIZATION_HEADER, "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .content("username=user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id_token").exists())
                .andExpect(jsonPath("$.validity").exists()).andReturn();

        JsonObject jsonObject = JsonObject.toJsonObject(mvcResult.getResponse().getContentAsString());

        String impersonateToken = jsonObject.getJSONAttrStr("id_token");

        mockMvc.perform(get("/api/user/current.json")
                        .header(JWTFilter.AUTHORIZATION_HEADER, "Bearer " + impersonateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("user"))
                .andExpect(jsonPath("$.isSwitched").value(true))
                .andExpect(jsonPath("$.realUser").value("admin"));
    }

    @Test
    @Transactional
    void switch_user_workflow_refuse_for_simple_user() throws Exception {
        User user = builder.given_default_user();

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "user",
                "test-password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
        String jwt = tokenProvider.createToken(authentication, TokenType.SESSION);

        mockMvc.perform(post("/api/login/impersonate")
                        .header(JWTFilter.AUTHORIZATION_HEADER, "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .content("username=user"))
                .andExpect(jsonPath("$.id_token").doesNotHaveJsonPath())
                .andExpect(status().isForbidden()).andReturn();
    }

    @Test
    @Transactional
    @WithMockUser(username = "admin")
    void switch_user_workflow_refuse_for_admin_with_no_admin_session() throws Exception {
        mockMvc.perform(post("/api/login/impersonate")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .content("username=user"))
                .andExpect(jsonPath("$.id_token").doesNotHaveJsonPath())
                .andExpect(status().isForbidden()).andReturn();
    }
}
