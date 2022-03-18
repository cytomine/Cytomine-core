package be.cytomine.api.controller;

import be.cytomine.CytomineCoreApplication;
import be.cytomine.domain.security.User;
import be.cytomine.dto.LoginVM;
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
class LoginControllerTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TokenProvider tokenProvider;

    @Autowired
    private MockMvc mockMvc;

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
        mockMvc
            .perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON).content(JsonObject.toJsonString(login)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id_token").isString())
            .andExpect(jsonPath("$.id_token").isNotEmpty())
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
        mockMvc
            .perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON).content(JsonObject.toJsonString(login)))
                .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id_token").isString())
            .andExpect(jsonPath("$.id_token").isNotEmpty())
            .andExpect(header().string("Authorization", not(nullValue())))
            .andExpect(header().string("Authorization", not(is(emptyString()))));
    }

    @Test
    void authorize_fails_with_bad_credential() throws Exception {
        LoginVM login = new LoginVM();
        login.setUsername("wrong-user");
        login.setPassword("wrong password");
        mockMvc
            .perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON).content(JsonObject.toJsonString(login)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.id_token").doesNotExist())
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
        mockMvc
                .perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON).content(JsonObject.toJsonString(login)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.id_token").doesNotExist())
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
        login.setUsername("user-disabled");
        login.setPassword("test");
        mockMvc
                .perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON).content(JsonObject.toJsonString(login)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.id_token").doesNotExist())
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
        mockMvc
                .perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON).content(JsonObject.toJsonString(login)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.id_token").doesNotExist())
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
        MvcResult mvcResult = mockMvc
                .perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON).content(JsonObject.toJsonString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id_token").isString())
                .andExpect(jsonPath("$.id_token").isNotEmpty())
                .andExpect(header().string("Authorization", not(nullValue())))
                .andExpect(header().string("Authorization", not(is(emptyString())))).andReturn();
        JsonObject jsonObject = JsonObject.toJsonObject(mvcResult.getResponse().getContentAsString());

        String token = jsonObject.getJSONAttrStr("id_token");
        assertThat(tokenProvider.validateToken(token)).isTrue();


        mockMvc.perform(get("/api/user/current.json")
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
        MvcResult mvcResult = mockMvc
                .perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON).content(JsonObject.toJsonString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id_token").isString())
                .andExpect(jsonPath("$.id_token").isNotEmpty())
                .andExpect(header().string("Authorization", not(nullValue())))
                .andExpect(header().string("Authorization", not(is(emptyString())))).andReturn();
        JsonObject jsonObject = JsonObject.toJsonObject(mvcResult.getResponse().getContentAsString());

        String token = jsonObject.getJSONAttrStr("id_token");
        assertThat(tokenProvider.validateToken(token)).isTrue();

        // locked user now...
        user.setAccountLocked(true);
        userRepository.save(user);

        mockMvc.perform(get("/api/user/current.json")
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
        MvcResult mvcResult = mockMvc
                .perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON).content(JsonObject.toJsonString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id_token").isString())
                .andExpect(jsonPath("$.id_token").isNotEmpty())
                .andExpect(header().string("Authorization", not(nullValue())))
                .andExpect(header().string("Authorization", not(is(emptyString())))).andReturn();
        JsonObject jsonObject = JsonObject.toJsonObject(mvcResult.getResponse().getContentAsString());

        String token = jsonObject.getJSONAttrStr("id_token");
        assertThat(tokenProvider.validateToken(token)).isTrue();

        // expired user now...
        user.setAccountExpired(true);
        userRepository.save(user);

        mockMvc.perform(get("/api/user/current.json")
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
        MvcResult mvcResult = mockMvc
                .perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON).content(JsonObject.toJsonString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id_token").isString())
                .andExpect(jsonPath("$.id_token").isNotEmpty())
                .andExpect(header().string("Authorization", not(nullValue())))
                .andExpect(header().string("Authorization", not(is(emptyString())))).andReturn();
        JsonObject jsonObject = JsonObject.toJsonObject(mvcResult.getResponse().getContentAsString());

        String token = jsonObject.getJSONAttrStr("id_token");
        assertThat(tokenProvider.validateToken(token)).isTrue();

        // expired user now...
        user.setEnabled(false);
        userRepository.save(user);

        mockMvc.perform(get("/api/user/current.json")
                        .header(JWTFilter.AUTHORIZATION_HEADER, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

}
