package be.cytomine.service.middleware;

import be.cytomine.CytomineCoreApplication;
import be.cytomine.service.appengine.AppEngineService;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;

import javax.transaction.Transactional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_SUPER_ADMIN", username = "superadmin")
@Transactional
public class AppEngineServiceTests {

    @Autowired
    AppEngineService appEngineService;

    private static WireMockServer wireMockServer = new WireMockServer(8888);

    @Value("${application.appEngine.apiBasePath}")
    private String apiBasePath;

    @BeforeAll
    public static void beforeAll() {
        wireMockServer.start();
    }


    @AfterAll
    public static void afterAll() {
        try {
            wireMockServer.stop();
        } catch (Exception e) {
        }
    }

    @Test
    void get_task() {
        configureFor("localhost", 8888);
        stubFor(get(urlEqualTo(apiBasePath + "task"))
                .willReturn(
                        aResponse().withBody(
                                """
                                        {"a":"b", "c":2}
                                        """
                        )
                )
        );
        ResponseEntity<String> response = appEngineService.get("task");
        assertThat(response).isNotNull();
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
    }
}
