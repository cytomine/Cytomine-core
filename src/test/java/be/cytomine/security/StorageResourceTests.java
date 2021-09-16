package be.cytomine.security;

import be.cytomine.CytomineCoreApplication;
import be.cytomine.domain.image.server.Storage;
import be.cytomine.domain.security.User;
import be.cytomine.repository.image.server.StorageRepository;
import be.cytomine.repository.security.SecUserRepository;
import be.cytomine.repository.security.UserRepository;
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
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_ADMIN", username = "admin")
public class StorageResourceTests {

    @Autowired
    private EntityManager em;

    @Autowired
    private StorageRepository storageRepository;

    @Autowired
    private SecUserRepository secUserRepository;

    @Autowired
    private MockMvc restStorageControllerMockMvc;

    @Test
    @Transactional
    public void it_shows_existing_user_by_id_with_success() throws Exception {

        restStorageControllerMockMvc.perform(get("/api/storage"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.collection[?(@.name=='admin storage')].user").value(secUserRepository.findByUsernameLikeIgnoreCase("admin").get().getId().intValue()));
    }

}
