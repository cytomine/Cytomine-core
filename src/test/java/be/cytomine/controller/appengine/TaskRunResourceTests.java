package be.cytomine.controller.appengine;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @Value("${application.appEngine.apiBasePath}")
    private String apiBasePath;

    private final static WireMockServer wireMockServer = new WireMockServer(8888);

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

        String mockResponse = """
                {
                  "task": {
                    "name": "string",
                    "namespace": "string",
                    "version": "string",
                    "description": "string",
                    "authors": [
                      {
                        "first_name": "string",
                        "last_name": "string",
                        "organization": "string",
                        "email": "string",
                        "is_contact": true
                      }
                    ]
                  },
                  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                  "state": "created"
                }""";

        configureFor("localhost", 8888);
        stubFor(WireMock.post(urlEqualTo(apiBasePath + "tasks/" + taskId + "/runs"))
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
        configureFor("localhost", 8888);
        stubFor(WireMock.put(urlEqualTo(apiBasePath + appEngineUriSection))
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
        /*
         * The queryBody should be an array but it appears that the mock mvc class does not support haveing
         * an array at the root of the json string. Because object is just forwarded to the App Engine
         * and we mock the response to be what it should be, I will just pass a single object here to avoid the
         * error and it should have no consequence unless we start sniffing the json object in the core endpoint
         * for provisioning.
         **/
        String queryBody = "[{\"value\": 0, \"param_name\": \"" + paramName + "\"}]";
        String mockResponse = "[{\"value\": 0, \"param_name\": \"" + paramName + "\", \"task_run_id\": \"" + taskRunId + "\"}]";
        String appEngineUriSection = "task-runs/" + taskRunId + "/input-provisions";
        configureFor("localhost", 8888);
        stubFor(WireMock.put(urlEqualTo(apiBasePath + appEngineUriSection))
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

    @Test
    @Transactional
    public void get_task_run() throws Exception {
        TaskRun taskRun = builder.given_a_not_persisted_task_run();
        taskRunRepository.saveAndFlush(taskRun);
        UUID taskRunId = taskRun.getTaskRunId();
        String mockResponse = getTaskRunBody(taskRunId);
        String appEngineUriSection = "task-runs/" + taskRunId;
        configureFor("localhost", 8888);
        stubFor(WireMock.get(urlEqualTo(apiBasePath + appEngineUriSection))
                .willReturn(
                        aResponse().withBody(mockResponse)
                )
        );

        restTaskRunControllerMockMvc.perform(get("/api/app-engine/project/" + taskRun.getProject().getId() + "/" + appEngineUriSection)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("id").value(taskRunId.toString()))
                .andExpect(jsonPath("task").exists())
                .andExpect(jsonPath("state").exists());
    }

    @Test
    @Transactional
    public void post_state_action() throws Exception {
        TaskRun taskRun = builder.given_a_not_persisted_task_run();
        taskRunRepository.saveAndFlush(taskRun);
        UUID taskRunId = taskRun.getTaskRunId();
        String queryBody = "{\"desired\": \"running\"}";
        String mockResponse = getTaskRunBody(taskRunId);
        String appEngineUriSection = "task-runs/" + taskRunId + "/state-actions";
        configureFor("localhost", 8888);
        stubFor(WireMock.post(urlEqualTo(apiBasePath + appEngineUriSection))
                .willReturn(
                        aResponse().withBody(mockResponse)
                )
        );

        restTaskRunControllerMockMvc.perform(post("/api/app-engine/project/" + taskRun.getProject().getId() + "/" + appEngineUriSection)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(queryBody))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("id").value(taskRunId.toString()))
                .andExpect(jsonPath("task").exists())
                .andExpect(jsonPath("state").exists());
    }

    protected String getTaskRunBody(UUID taskRunId) {
        return "{\n" +
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
            "  \"id\": \"" + taskRunId.toString() + "\",\n" +
            "  \"state\": \"CREATED\",\n" +
            "  \"created_at\": \"2023-12-20T10:49:21.272Z\",\n" +
            "  \"updated_at\": \"2023-12-20T10:49:21.272Z\",\n" +
            "  \"last_state_transition_at\": \"2023-12-20T10:49:21.272Z\"\n" +
            "}";
    }
}


