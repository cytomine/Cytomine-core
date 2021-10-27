package be.cytomine.domain;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.domain.image.UploadedFile;
import be.cytomine.domain.ontology.Ontology;
import be.cytomine.repository.image.UploadedFileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@Transactional
public class UploadedFileDomainTests {

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    UploadedFileRepository uploadedFileRepository;

    @Autowired
    EntityManager entityManager;

    @Test
    void support_long_array_read() {
        for (UploadedFile uploadedFile : uploadedFileRepository.findAll()) {
            System.out.print(uploadedFile.getId() + " => ");
            if (uploadedFile.getProjects() != null) {
                Arrays.stream(uploadedFile.getProjects()).forEach(System.out::println);
            }
            System.out.println();
        }
    }


    @Test
    void support_long_array_persistence() {
        UploadedFile uploadedFile = builder.given_a_not_persisted_uploaded_file();
        uploadedFile.setProjects(new Long[]{1L,2L,3L});
        uploadedFile = uploadedFileRepository.save(uploadedFile);
        entityManager.flush();
        Long id = uploadedFile.getId();
        //entityManager.detach(uploadedFile);
        entityManager.refresh(uploadedFile);
        System.out.println("id = " + id);
        uploadedFile = entityManager.find(UploadedFile.class, id);
        assertThat(uploadedFile).isNotNull();
        assertThat(uploadedFile.getProjects()).isNotNull();
        assertThat(uploadedFile.getProjects()).contains(1L, 2L, 3L);
    }
}
