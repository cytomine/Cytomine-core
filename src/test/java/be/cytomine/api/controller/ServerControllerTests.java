package be.cytomine.api.controller;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.domain.meta.Configuration;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.social.LastConnection;
import be.cytomine.repositorynosql.social.LastConnectionRepository;
import be.cytomine.utils.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
public class ServerControllerTests {

    @Autowired
    private EntityManager em;

    @Autowired
    private BasicInstanceBuilder builder;

    @Autowired
    private MockMvc restConfigurationControllerMockMvc;

    @Autowired
    private LastConnectionRepository lastConnectionRepository;

    @BeforeEach
    public void before() {
        lastConnectionRepository.deleteAll();
    }

    @Test
    @Transactional
    @WithMockUser(username = "superadmin")
    public void ping_as_auth() throws Exception {
        restConfigurationControllerMockMvc.perform(post("/server/ping.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"project\": null}"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alive").value(true))
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.version").hasJsonPath())
                .andExpect(jsonPath("$.serverURL").hasJsonPath())
                .andExpect(jsonPath("$.serverID").hasJsonPath())
                .andExpect(jsonPath("$.user").value(builder.given_superadmin().getId()));

        List<LastConnection> lastConnection = lastConnectionRepository.findByUserOrderByCreatedDesc(builder.given_superadmin().getId());
        assertThat(lastConnection).hasSize(1);
        assertThat(lastConnection.get(0).getProject()).isNull();
    }

    @Test
    @Transactional
    @WithMockUser(username = "superadmin")
    public void ping_with_project() throws Exception {
        Project project = builder.given_a_project();
        restConfigurationControllerMockMvc.perform(post("/server/ping.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"project\": "+project.getId()+"}"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alive").value(true))
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.version").hasJsonPath())
                .andExpect(jsonPath("$.serverURL").hasJsonPath())
                .andExpect(jsonPath("$.serverID").hasJsonPath())
                .andExpect(jsonPath("$.user").value(builder.given_superadmin().getId()));

        List<LastConnection> lastConnection = lastConnectionRepository.findByUserOrderByCreatedDesc(builder.given_superadmin().getId());
        assertThat(lastConnection).hasSize(1);
        assertThat(lastConnection.get(0).getProject()).isEqualTo(project.getId());

    }

    @Test
    @Transactional
    public void ping_as_unauth() throws Exception {
        restConfigurationControllerMockMvc.perform(post("/server/ping.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"project\": null}"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alive").value(true))
                .andExpect(jsonPath("$.authenticated").value(false))
                .andExpect(jsonPath("$.version").hasJsonPath())
                .andExpect(jsonPath("$.serverURL").hasJsonPath())
                .andExpect(jsonPath("$.serverID").hasJsonPath())
                .andExpect(jsonPath("$.user").doesNotExist());

        assertThat(lastConnectionRepository.count()).isEqualTo(0);
    }

    @Test
    @Transactional
    public void check_status() throws Exception {
        restConfigurationControllerMockMvc.perform(get("/status.json"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alive").value(true))
                .andExpect(jsonPath("$.version").hasJsonPath())
                .andExpect(jsonPath("$.serverURL").hasJsonPath());
    }

}
