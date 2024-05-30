package be.cytomine.api.controller.security;

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

import be.cytomine.CytomineCoreApplication;
import be.cytomine.repository.security.UserRepository;
import be.cytomine.utils.AuthenticationSuccessListener;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.*;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
public class Oauth2ResourceServerTests {

    @Autowired
    private MockMvc allProtectedMockMvc;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    AuthenticationSuccessListener authenticationSuccessListener;

    @Autowired
    ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private UserRepository userRepository;


    @Test
    public void whenNoTokenProvided_thenUnauthorized() throws Exception {
        allProtectedMockMvc.perform(get("/api/project/45.json"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void whenInvalidTokenProvided_thenUnauthorized() throws Exception {
        allProtectedMockMvc.perform(get("/api/project/45.json")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void whenValidTokenProvided_thenNotFoundAsOk() throws Exception {
        // get a valid cytomine access token using password grant from iam microservice
        ResponseEntity<String> response = restTemplate.exchange(
                "http://nginx/iam/realms/cytomine/protocol/openid-connect/token",
                HttpMethod.POST,
                buildPasswordGrantTokenEntity(),
                String.class
        );

        allProtectedMockMvc.perform(get("/api/project/45.json")
                        .header("Authorization", "Bearer " + getValidAccessTokenFrom(response)))
                .andExpect(status().isNotFound());
    }

    @Test
    public void whenAuthenticationSuccessEventPublished_thenUserAddedIfDoesNotExist() {
        // user not already in the database
        String sub = "b74b6c49-bee7-4238-bb3b-580b14af703c";
        Assertions.assertTrue(userRepository.findByReference(sub).isEmpty());

        // given a valid token authentication
        String tokenString = "eyJhbGciOiJSUzI1NiIsInI1ojGFg";
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(10 , ChronoUnit.MINUTES); // still not the funniest thing in Cytomine
        Map<String,Object> headers = new HashMap<>();
        headers.put("kid" , "6mTJJxdp_uUSC9eK8vuCrmMQ1WvgN2oG5xh4GGSskDg");
        headers.put("typ" , "JWT");
        headers.put("alg" , "RS256");
        Map<String,Object> claims = new HashMap<>();
        claims.put("sub" , sub);
        claims.put("name" , "Some User");
        claims.put("preferred_username" , "test_user_from_token");
        Jwt jwt = new Jwt(tokenString , issuedAt ,expiresAt , headers , claims);
        List<GrantedAuthority> authorities = new ArrayList<>();
        GrantedAuthority userRole = new SimpleGrantedAuthority("ROLE_USER");
        authorities.add(userRole);
        JwtAuthenticationToken jwtAuthenticationToken = new JwtAuthenticationToken(jwt , authorities);
        applicationEventPublisher.publishEvent(new AuthenticationSuccessEvent(jwtAuthenticationToken));

        // assert a user with this sub is created
        Assertions.assertTrue(userRepository.findByReference(sub).isPresent());
    }

    private static String getValidAccessTokenFrom(ResponseEntity<String> response) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        return responseBody.get("access_token").asText();
    }

    private static HttpEntity<MultiValueMap<String, String>> buildPasswordGrantTokenEntity() {
        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("grant_type", "password");
        requestBody.add("client_id", "core");
        requestBody.add("username", "admin");
        requestBody.add("password", "password");
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", MediaType.APPLICATION_FORM_URLENCODED_VALUE);

        return new HttpEntity<>(requestBody, headers);
    }

}
