package be.cytomine.service.middleware;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.domain.image.server.Storage;
import be.cytomine.domain.middleware.ImageServer;
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.repository.image.server.StorageRepository;
import be.cytomine.service.CommandService;
import be.cytomine.service.PermissionService;
import be.cytomine.service.command.TransactionService;
import be.cytomine.service.image.server.StorageService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.utils.CommandResponse;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import javax.transaction.Transactional;

import java.io.IOException;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.security.acls.domain.BasePermission.*;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_SUPER_ADMIN", username = "superadmin")
@Transactional
public class ImageServerServiceTests {

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    ImageServerService imageServerService;

    private static WireMockServer wireMockServer = new WireMockServer(8888);

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
    void retrieve_storage_spaces() throws IOException {


        ImageServer imageServer = builder.given_an_image_server();
        imageServer.setUrl("http://localhost:8888");
        imageServer = builder.persistAndReturn(imageServer);



        configureFor("localhost", 8888);
        stubFor(get(urlEqualTo("/storage/size.json")).willReturn(aResponse().withBody("" +
                "{\"used\":193396892,\"available\":445132860,\"usedP\":0.302878435,\"hostname\":\"b52416f53249\",\"mount\":\"/data/images\",\"ip\":null}")));

        Map<String, Object> response = imageServerService.storageSpace(imageServer);
        assertThat(response).isNotNull();
        assertThat(response.get("used")).isEqualTo(193396892);
        assertThat(response.get("hostname")).isEqualTo("b52416f53249");

    }

}
