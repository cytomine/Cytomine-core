package be.cytomine.api.controller;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.domain.command.Command;
import be.cytomine.domain.ontology.Ontology;
import be.cytomine.domain.project.Project;
import be.cytomine.repository.command.CommandRepository;
import be.cytomine.repository.ontology.OntologyRepository;
import be.cytomine.service.utils.TaskService;
import be.cytomine.utils.JsonObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
public class TaskControllerTests {

    @Autowired
    private BasicInstanceBuilder builder;

    @Autowired
    private MockMvc restCommandControllerMockMvc;

    @Test
    @Transactional
    @WithMockUser(username = "superadmin")
    public void task_workflow() throws Exception {
        Project project = builder.given_a_project();
        MvcResult response = restCommandControllerMockMvc.perform(post("/api/task.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonObject.of("project", project.getId()).toJsonString()))
                .andDo(print())
                .andExpect(status().isOk()).andReturn();

        JsonObject responseObject = JsonObject.toJsonObject(response.getResponse().getContentAsString());
        Integer id = (Integer)((Map<String,Object>)responseObject.get("task")).get("id");

        restCommandControllerMockMvc.perform(get("/api/task/{id}.json", id))
                .andDo(print())
                .andExpect(status().isOk());

        restCommandControllerMockMvc.perform(get("/api/project/{project}/task/comment.json", project.getId()))
                .andDo(print())
                .andExpect(status().isOk());

    }
}
