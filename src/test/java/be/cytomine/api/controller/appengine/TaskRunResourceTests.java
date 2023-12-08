package be.cytomine.api.controller.appengine;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.domain.appengine.TaskRun;
import be.cytomine.repository.appengine.TaskRunRepository;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(username = "superadmin")
@Transactional
public class TaskRunResourceTests {

    @Autowired
    private EntityManager em;

    @Autowired
    private TaskRunRepository taskRunRepository;

    @Autowired
    private BasicInstanceBuilder builder;

    @Autowired
    private MockMvc restTaskRunControllerMockMvc;

    private static WireMockServer wireMockServer = new WireMockServer(8889);

    @BeforeAll
    public static void beforeAll() {
        wireMockServer.start();
    }

    @AfterAll
    public static void afterAll() {
        try {
            wireMockServer.stop();
        } catch (Exception e) {}
    }

    @Test
    @Transactional
    public void add_valid_task_run() throws Exception {
        TaskRun taskRun = builder.given_a_not_persisted_task_run();

        String taskId = UUID.randomUUID().toString();

        String mockResponse = "{\n" +
                "  \"task\": {\n" +
                "    \"name\": \"string\",\n" +
                "    \"namespace\": \"string\",\n" +
                "    \"version\": \"string\",\n" +
                "    \"description\": \"string\",\n" +
                "    \"authors\": [\n" +
                "      {\n" +
                "        \"first_name\": \"string\",\n" +
                "        \"last_name\": \"string\",\n" +
                "        \"organization\": \"string\",\n" +
                "        \"email\": \"string\",\n" +
                "        \"is_contact\": true\n" +
                "      }\n" +
                "    ]\n" +
                "  },\n" +
                "  \"id\": \"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\n" +
                "  \"state\": \"created\"\n" +
                "}";

        configureFor("localhost", 8889);
        stubFor(WireMock.post(urlEqualTo("/api/v1/tasks/" + taskId + "/runs"))
                .willReturn(
                        aResponse().withBody(mockResponse)
                )
        );

        restTaskRunControllerMockMvc.perform(post("/api/app-engine/project/" + taskRun.getProject().getId() + "/tasks/"+ taskId + "/runs"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("3fa85f64-5717-4562-b3fc-2c963f66afa6"))
                .andExpect(jsonPath("$.task").exists())
                .andExpect(jsonPath("$.state").exists());
    }

    @Test
    @Transactional
    public void single_param_provision_of_a_task() throws Exception {
        TaskRun taskRun = builder.given_a_not_persisted_task_run();
        taskRunRepository.saveAndFlush(taskRun);
        UUID taskRunId = taskRun.getTaskRunId();

        String paramName = "my_param";
        String queryBody = "{\"value\": 0, \"param_name\": \"" + paramName + "\"}";
        String mockResponse = "{\"value\": 0, \"param_name\": \"" + paramName + "\", \"task_run_id\": \"" + taskRunId + "\"}";
        String appEngineUriSection = "task-runs/" + taskRunId + "/input-provisions/" + paramName;
        configureFor("localhost", 8889);
        stubFor(WireMock.put(urlEqualTo("/api/v1/" + appEngineUriSection))
                .willReturn(
                        aResponse().withBody(mockResponse)
                )
        );

        restTaskRunControllerMockMvc.perform(put("/api/app-engine/project/" + taskRun.getProject().getId() + "/" + appEngineUriSection)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(queryBody))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.task_run_id").value(taskRunId.toString()))
                .andExpect(jsonPath("$.param_name").exists())
                .andExpect(jsonPath("$.value").exists());
    }

    @Test
    @Transactional
    public void batch_param_provision_of_a_task() throws Exception {
        TaskRun taskRun = builder.given_a_not_persisted_task_run();
        taskRunRepository.saveAndFlush(taskRun);
        UUID taskRunId = taskRun.getTaskRunId();

        String paramName = "my_param";
        /**
         * The queryBody should be an array but it appears that the mock mvc class does not support haveing
         * an array at the root of the json string. Because object is just forwarded to the App Engine
         * and we mock the response to be what it should be, I will just pass a single object here to avoid the
         * error and it should have no consequence unless we start sniffing the json object in the core endpoint
         * for provisioning.
         **/
        String queryBody = "{\"value\": 0, \"param_name\": \"" + paramName + "\"}";
        String mockResponse = "[{\"value\": 0, \"param_name\": \"" + paramName + "\", \"task_run_id\": \"" + taskRunId + "\"}]";
        String appEngineUriSection = "task-runs/" + taskRunId + "/input-provisions";
        configureFor("localhost", 8889);
        stubFor(WireMock.put(urlEqualTo("/api/v1/" + appEngineUriSection))
                .willReturn(
                        aResponse().withBody(mockResponse)
                )
        );

        restTaskRunControllerMockMvc.perform(put("/api/app-engine/project/" + taskRun.getProject().getId() + "/" + appEngineUriSection)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(queryBody))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("[0].task_run_id").value(taskRunId.toString()))
                .andExpect(jsonPath("[0].param_name").exists())
                .andExpect(jsonPath("[0].value").exists());
    }
}
