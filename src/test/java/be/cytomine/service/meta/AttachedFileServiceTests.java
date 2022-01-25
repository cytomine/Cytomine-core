package be.cytomine.service.meta;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.domain.meta.AttachedFile;
import be.cytomine.domain.project.Project;
import be.cytomine.exceptions.ObjectNotFoundException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(username = "superadmin")
@Transactional
public class AttachedFileServiceTests {

    @Autowired
    AttachedFileService attachedFileService;

    @Autowired
    BasicInstanceBuilder builder;

    @Test
    public void list_attached_file() {
        AttachedFile attachedFile = builder.given_a_attached_file(builder.given_a_project());
        assertThat(attachedFileService.list()).contains(attachedFile);
    }

    @Test
    public void list_attached_file_for_domain() {
        Project project = builder.given_a_project();
        AttachedFile attachedFile = builder.given_a_attached_file(project);
        assertThat(attachedFileService.findAllByDomain(project)).contains(attachedFile);
    }

    @Test
    public void list_attached_file_for_domain_that_do_not_exists() {
        Assertions.assertThrows(ObjectNotFoundException.class, () -> {
            attachedFileService.findAllByDomain(Project.class.getName(), 0L);
        });
    }

    @Test
    public void find_by_id() {
        AttachedFile attachedFile = builder.given_a_attached_file(builder.given_a_project());
        assertThat(attachedFileService.findById(attachedFile.getId())).isPresent();
    }

    @Test
    public void find_by_id_that_do_not_exists() {
        assertThat(attachedFileService.findById(0L)).isEmpty();
    }

    @Test
    public void create_attached_file() throws ClassNotFoundException {
        Project project = builder.given_a_project();
        AttachedFile attachedFile =
                attachedFileService.create("test.txt", new String("hello").getBytes(), "test", project.getId(), project.getClass().getName());
        assertThat(attachedFile).isNotNull();
    }

    @Test
    public void delete_attached_file() {
        AttachedFile attachedFile = builder.given_a_attached_file(builder.given_a_project());
        attachedFileService.delete(attachedFile, null, null, false);
        assertThat(attachedFileService.findById(attachedFile.getId())).isEmpty();
    }

}
