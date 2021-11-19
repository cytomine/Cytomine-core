package be.cytomine.security;

import be.cytomine.CytomineCoreApplication;
import be.cytomine.domain.security.User;
import be.cytomine.utils.JsonObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.Random;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_ADMIN", username = "admin")
public class UserResourceTests {

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restUserControllerMockMvc;

    @Test
    @Transactional
    public void it_shows_existing_user_by_id_with_success() throws Exception {
        User user = given_a_user();

        restUserControllerMockMvc.perform(get("/api/user/{id}.json", user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.username").value(user.getUsername()))
                .andExpect(jsonPath("$.lastname").value(user.getLastname()));
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "ROLE_ADMIN", username = "admin")
    public void show_current_user() throws Exception {
        User user = given_a_user();

        restUserControllerMockMvc.perform(get("/api/user/current.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"));
    }


    @Test
    @Transactional
    public void it_shows_existing_user_by_username_with_success() throws Exception {
        User user = given_a_user();

        restUserControllerMockMvc.perform(get("/api/user/{id}.json", user.getUsername()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.username").value(user.getUsername()))
                .andExpect(jsonPath("$.lastname").value(user.getLastname()));
    }


    @Test
    @Transactional
    public void it_hides_sensible_user_fields() throws Exception {
        User user = given_a_user();

        restUserControllerMockMvc.perform(get("/api/user/{id}.json", user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.privateKey").doesNotExist());
    }

    @Test
    @Transactional
    public void it_fails_to_show_unexisting_user() throws Exception {
        restUserControllerMockMvc.perform(get("/api/user/{id}.json", -1))
                .andExpect(status().isNotFound());
    }


    @Test
    @Transactional
    public void it_create_valid_user_with_success() throws Exception {

        User user = given_a_user_not_persisted();

        JsonObject json = User.getDataFromDomain(user);
        json.put("password", "abracAdabra"); //password is removed from json generation

        restUserControllerMockMvc.perform(post("/api/user.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.toJsonString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.user.id").exists())
                .andExpect(jsonPath("$.user.username").value(user.getUsername()))
                .andExpect(jsonPath("$.user.lastname").value(user.getLastname()))
                .andExpect(jsonPath("$.user.password").doesNotExist());
    }

    @Test
    @Transactional
    public void it_fails_to_create_user_with_already_existing_username() throws Exception {

        User alreadyExistingUser = given_a_user();
        User user = given_a_user_not_persisted();
        user.setUsername(alreadyExistingUser.getUsername());

        then_it_return_this_status_when_creating_user(user, 409);
    }

    @Test
    @Transactional
    public void it_fails_to_create_user_with_invalid_username() throws Exception {

        User user = given_a_user_not_persisted();
        user.setUsername(" invalid ");

        then_it_return_this_status_when_creating_user(user, 400);
    }

    @Test
    @Transactional
    public void it_edit_valid_user_with_success() throws Exception {

        User user = given_a_user();
        user.setLastname("NewLastname");
        user.setEmail(UUID.randomUUID() + "@newmail.com");
        restUserControllerMockMvc.perform(put("/api/user/{id}.json", user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(user.toJSON()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.lastname").value("NewLastname"))
                .andExpect(jsonPath("$.user.email").value(user.getEmail()));

    }


    @Test
    @Transactional
    public void it_delete_existing_user_with_success() throws Exception {

        User user = given_a_user();

        restUserControllerMockMvc.perform(delete("/api/user/{id}.json", user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(user.toJSON()))
                .andDo(print())
                .andExpect(status().isOk());


        restUserControllerMockMvc.perform(get("/api/user/{id}.json", user.getId()))
                .andExpect(status().isNotFound());

    }



    private void then_it_return_this_status_when_creating_user(User user, int status) throws Exception {
        JsonObject json = User.getDataFromDomain(user);
        json.put("password", "abracAdabra"); //password is removed from json generation
        restUserControllerMockMvc.perform(post("/api/user.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.toJsonString()))
                .andDo(print())
                .andExpect(status().is(status));
    }

    private User given_a_user() {
        //User user2 = new User();
        User user = given_a_user_not_persisted();
        em.persist(user);
        em.flush();
        return user;
    }

    private User given_a_user_not_persisted() {
        //User user2 = new User();
        User user = new User();
        user.setFirstname("firstname");
        user.setLastname("lastname");
        user.setUsername(UUID.randomUUID().toString());
        user.setEmail(UUID.randomUUID() + "@example.com");
        user.setPublicKey(UUID.randomUUID().toString());
        user.setPrivateKey(UUID.randomUUID().toString());
        user.setPassword(UUID.randomUUID().toString());
        user.setOrigin("unkown");
        return user;
    }

}
