package be.cytomine.api.controller.social;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.image.SliceInstance;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.SecUser;
import be.cytomine.domain.security.User;
import be.cytomine.domain.social.LastConnection;
import be.cytomine.domain.social.PersistentImageConsultation;
import be.cytomine.domain.social.PersistentProjectConnection;
import be.cytomine.repositorynosql.social.LastConnectionRepository;
import be.cytomine.repositorynosql.social.PersistentImageConsultationRepository;
import be.cytomine.repositorynosql.social.PersistentProjectConnectionRepository;
import be.cytomine.service.database.SequenceService;
import be.cytomine.service.social.ImageConsultationService;
import be.cytomine.service.social.ProjectConnectionService;
import be.cytomine.utils.JsonObject;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_SUPER_ADMIN", username = "superadmin")
@Transactional
public class ProjectConnectionResourceTests {

    @Autowired
    private EntityManager em;

    @Autowired
    private BasicInstanceBuilder builder;

    @Autowired
    private MockMvc restProjectConnectionControllerMockMvc;


    @Autowired
    PersistentProjectConnectionRepository persistentProjectConnectionRepository;

    @Autowired
    LastConnectionRepository lastConnectionRepository;

    @Autowired
    ProjectConnectionService projectConnectionService;

    @Autowired
    SequenceService sequenceService;

    @BeforeEach
    public void cleanDB() {
        persistentProjectConnectionRepository.deleteAll();
        lastConnectionRepository.deleteAll();
    }


    PersistentProjectConnection given_a_persistent_connection_in_project(User user, Project project) {
        PersistentProjectConnection connection = projectConnectionService.add(user, project, "xxx", "linux", "chrome", "123");
        return connection;
    }

    PersistentProjectConnection given_a_persistent_connection_in_project(User user, Project project, Date created) {
        PersistentProjectConnection connection = projectConnectionService.add(user, project, "xxx", "linux", "chrome", "123", created);
        return connection;
    }


    @Test
    @Transactional
    public void add_connection() throws Exception {
        User user = builder.given_superadmin();
        Project project = builder.given_a_project();

        // {"project":"7577928","os":"Linux","browser":"chrome","browserVersion":"97.0.4692"}
        JsonObject jsonObject = new JsonObject();
        jsonObject.put("project", project.getId());
        jsonObject.put("os", "Linux");
        jsonObject.put("browser", "chrome");
        jsonObject.put("browserVersion", "97.0.4692");
        
        restProjectConnectionControllerMockMvc.perform(post("/api/project/{id}/userconnection.json", project.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonObject.toJsonString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.class").value("be.cytomine.domain.social.PersistentProjectConnection"))
                .andExpect(jsonPath("$.user").value(user.getId()))
                .andExpect(jsonPath("$.project").value(project.getId()))
                .andExpect(jsonPath("$.browser").exists())
                .andExpect(jsonPath("$.browserVersion").exists());
        
    }




    @Test
    @Transactional
    public void get_connection_by_user_and_project() throws Exception {
        User user = builder.given_a_user();
        Project project1 = builder.given_a_project();

        given_a_persistent_connection_in_project(user, project1, DateUtils.addSeconds(new Date(), -3));
        given_a_persistent_connection_in_project(user, project1, DateUtils.addSeconds(new Date(), -2));

        restProjectConnectionControllerMockMvc.perform(get("/api/project/{project}/userconnection/{user}.json", project1.getId(), user.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(2))));
    }

    @Test
    @Transactional
    public void get_connection_by_project() throws Exception {
        User user = builder.given_a_user();
        User anotherUser = builder.given_a_user();
        Project project1 = builder.given_a_project();

        given_a_persistent_connection_in_project(user, project1, DateUtils.addSeconds(new Date(), -3));
        given_a_persistent_connection_in_project(anotherUser, project1, DateUtils.addSeconds(new Date(), -2));
        given_a_persistent_connection_in_project(user, project1, DateUtils.addSeconds(new Date(), -1));

        restProjectConnectionControllerMockMvc.perform(get("/api/project/{project}/lastConnection.json", project1.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(2))));
    }


    @Test
    @Transactional
    public void get_last_connection_by_project() throws Exception {
        User user = builder.given_a_user();
        Project project1 = builder.given_a_project();

        given_a_persistent_connection_in_project(user, project1, DateUtils.addSeconds(new Date(), -3));
        given_a_persistent_connection_in_project(user, project1, DateUtils.addSeconds(new Date(), -2));

        restProjectConnectionControllerMockMvc.perform(get("/api/project/{project}/lastConnection.json", project1.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(1))))
                .andExpect(jsonPath("$.collection[0].user").value(user.getId()));
    }

    @Test
    @Transactional
    public void get_last_connection_by_project_by_user() throws Exception {
        User user = builder.given_a_user();
        Project project1 = builder.given_a_project();

        given_a_persistent_connection_in_project(user, project1, DateUtils.addSeconds(new Date(), -3));
        given_a_persistent_connection_in_project(user, project1, DateUtils.addSeconds(new Date(), -2));

        restProjectConnectionControllerMockMvc.perform(get("/api/project/{project}/lastConnection/{user}.json", project1.getId(), user.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(equalTo(1))))
                .andExpect(jsonPath("$.collection[0].user").value(user.getId()));
    }

    @Test
    @Transactional
    public void get_number_connection_by_project() throws Exception {
        User user = builder.given_a_user();
        Project project1 = builder.given_a_project();

        given_a_persistent_connection_in_project(user, project1, DateUtils.addSeconds(new Date(), -3));
        given_a_persistent_connection_in_project(user, project1, DateUtils.addSeconds(new Date(), -2));

        restProjectConnectionControllerMockMvc.perform(get("/api/project/{project}/connectionFrequency.json", project1.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[0].frequency").value(2));
    }

    @Test
    @Transactional
    public void get_number_connection_by_project_heatmap() throws Exception {
        User user = builder.given_a_user();
        Project project1 = builder.given_a_project();

        given_a_persistent_connection_in_project(user, project1, DateUtils.addSeconds(new Date(), -3));
        given_a_persistent_connection_in_project(user, project1, DateUtils.addSeconds(new Date(), -2));

        restProjectConnectionControllerMockMvc.perform(get("/api/project/{project}/connectionFrequency.json", project1.getId())
                        .param("heatmap", "true"))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    public void get_number_connection_by_project_with_period() throws Exception {
        User user = builder.given_a_user();
        Project project1 = builder.given_a_project();

        given_a_persistent_connection_in_project(user, project1, DateUtils.addSeconds(new Date(), -3));
        given_a_persistent_connection_in_project(user, project1, DateUtils.addSeconds(new Date(), -2));

        restProjectConnectionControllerMockMvc.perform(get("/api/project/{project}/connectionFrequency.json", project1.getId())
                        .param("period", "week"))
                .andDo(print())
                .andExpect(status().isOk());
    }


    @Test
    @Transactional
    public void get_number_connection_frequency_by_user() throws Exception {
        User user = builder.given_a_user();
        Project project1 = builder.given_a_project();

        given_a_persistent_connection_in_project(user, project1, DateUtils.addSeconds(new Date(), -3));
        given_a_persistent_connection_in_project(user, project1, DateUtils.addSeconds(new Date(), -2));

        restProjectConnectionControllerMockMvc.perform(get("/api/project/{project}/connectionFrequency/{user}.json", project1.getId(), user.getId()))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    public void get_number_connection_frequency_by_user_with_heatmap() throws Exception {
        User user = builder.given_a_user();
        Project project1 = builder.given_a_project();

        given_a_persistent_connection_in_project(user, project1, DateUtils.addSeconds(new Date(), -3));
        given_a_persistent_connection_in_project(user, project1, DateUtils.addSeconds(new Date(), -2));

        restProjectConnectionControllerMockMvc.perform(get("/api/project/{project}/connectionFrequency/{user}.json", project1.getId(), user.getId())
                        .param("heatmap", "true"))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    public void get_number_connection_frequency_by_user_with_period() throws Exception {
        User user = builder.given_a_user();
        Project project1 = builder.given_a_project();

        given_a_persistent_connection_in_project(user, project1, DateUtils.addSeconds(new Date(), -3));
        given_a_persistent_connection_in_project(user, project1, DateUtils.addSeconds(new Date(), -2));

        restProjectConnectionControllerMockMvc.perform(get("/api/project/{project}/connectionFrequency/{user}.json", project1.getId(), user.getId())
                        .param("period", "week"))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    public void get_connection_frequency() throws Exception {
        User user = builder.given_a_user();
        Project project1 = builder.given_a_project();

        given_a_persistent_connection_in_project(user, project1, DateUtils.addSeconds(new Date(), -3));
        given_a_persistent_connection_in_project(user, project1, DateUtils.addSeconds(new Date(), -2));

        restProjectConnectionControllerMockMvc.perform(get("/api/connectionFrequency.json")
                        .param("period", "week"))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    public void average_econnection() throws Exception {
        User user = builder.given_a_user();
        Project project1 = builder.given_a_project();

        given_a_persistent_connection_in_project(user, project1, DateUtils.addSeconds(new Date(), -3));
        given_a_persistent_connection_in_project(user, project1, DateUtils.addSeconds(new Date(), -2));

        restProjectConnectionControllerMockMvc.perform(get("/api/averageConnections.json")
                .param("period", "week"))
                .andDo(print())
                .andExpect(status().isOk());

    }

    @Test
    @Transactional
    public void user_connection_count() throws Exception {
        User user = builder.given_a_user();
        Project project1 = builder.given_a_project();

        given_a_persistent_connection_in_project(user, project1, DateUtils.addSeconds(new Date(), -3));
        given_a_persistent_connection_in_project(user, project1, DateUtils.addSeconds(new Date(), -2));

        restProjectConnectionControllerMockMvc.perform(get("/api/project/{project}/userconnection/count.json", project1.getId()))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    public void connection_history() throws Exception {
        User user = builder.given_a_user();
        Project project1 = builder.given_a_project();

        given_a_persistent_connection_in_project(user, project1, DateUtils.addSeconds(new Date(), -3));
        given_a_persistent_connection_in_project(user, project1, DateUtils.addSeconds(new Date(), -2));

        restProjectConnectionControllerMockMvc.perform(get("/api/project/{project}/connectionHistory/{user}.json", project1.getId(), user.getId()))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    public void connection_history_with_export_csv() throws Exception {
        User user = builder.given_a_user();
        Project project1 = builder.given_a_project();

        given_a_persistent_connection_in_project(user, project1, DateUtils.addSeconds(new Date(), -3));
        given_a_persistent_connection_in_project(user, project1, DateUtils.addSeconds(new Date(), -2));

        restProjectConnectionControllerMockMvc.perform(get("/api/project/{project}/connectionHistory/{user}.json", project1.getId(), user.getId())
                        .param("export", "csv"))
                .andDo(print())
                .andExpect(status().isOk());
    }


    @Test
    @Transactional
    public void activity_detail() throws Exception {
        User user = builder.given_a_user();
        Project project1 = builder.given_a_project();

        PersistentProjectConnection connection = given_a_persistent_connection_in_project(user, project1, DateUtils.addSeconds(new Date(), -3));

        restProjectConnectionControllerMockMvc.perform(get("/api/projectConnection/{id}.json", connection.getId()))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    public void activity_detail_with_export_csv() throws Exception {
        User user = builder.given_a_user();
        Project project1 = builder.given_a_project();

        PersistentProjectConnection connection = given_a_persistent_connection_in_project(user, project1, DateUtils.addSeconds(new Date(), -3));

        restProjectConnectionControllerMockMvc.perform(get("/api/projectConnection/{id}.json", connection.getId())
                        .param("export", "csv"))
                .andDo(print())
                .andExpect(status().isOk());
    }

}
