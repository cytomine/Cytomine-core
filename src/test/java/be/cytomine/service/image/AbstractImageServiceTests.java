package be.cytomine.service.image;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.domain.image.AbstractImage;
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.image.UploadedFile;
import be.cytomine.domain.image.server.Storage;
import be.cytomine.domain.ontology.Ontology;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.repository.image.UploadedFileRepository;
import be.cytomine.service.CommandService;
import be.cytomine.service.command.TransactionService;
import be.cytomine.service.search.UploadedFileSearchParameter;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.filters.SearchOperation;
import be.cytomine.utils.filters.SearchParameterEntry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.fail;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;


@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(username = "superadmin")
@Transactional
public class AbstractImageServiceTests {

    @Autowired
    AbstractImageService abstractImageService;

    @Autowired
    UploadedFileRepository uploadedFileRepository;

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    CommandService commandService;

    @Autowired
    TransactionService transactionService;

    @Test
    void list_all_image_by_filters() {
        AbstractImage abstractImage1 = builder.given_an_abstract_image();
        abstractImage1.setOriginalFilename("karamazov");
        abstractImage1.setWidth(800);
        builder.persistAndReturn(abstractImage1);
        AbstractImage abstractImage2 = builder.given_an_abstract_image();
        abstractImage2.setOriginalFilename("deray");
        abstractImage2.setWidth(2048);
        builder.persistAndReturn(abstractImage2);

        Page<AbstractImage> images = null;
        images = abstractImageService.list(null, new ArrayList<>(List.of(new SearchParameterEntry("originalFilename", SearchOperation.ilike, "kara"))), Pageable.unpaged());
        assertThat(images.getContent()).contains(abstractImage1);
        assertThat(images.getContent()).doesNotContain(abstractImage2);

        images = abstractImageService.list(null, new ArrayList<>(List.of(new SearchParameterEntry("width", SearchOperation.gte, 1024))), Pageable.unpaged());
        assertThat(images.getContent()).contains(abstractImage2);
        assertThat(images.getContent()).doesNotContain(abstractImage1);

        images = abstractImageService.list(null, new ArrayList<>(
                List.of(new SearchParameterEntry("width", SearchOperation.lte, 800),
                        new SearchParameterEntry("originalFilename", SearchOperation.ilike, "kara"))
        ), Pageable.unpaged());
        assertThat(images.getContent()).contains(abstractImage1);
        assertThat(images.getContent()).doesNotContain(abstractImage2);
    }

    @Test
    void list_all_image_by_project() {
        AbstractImage abstractImageInProject = builder.given_an_abstract_image();
        builder.persistAndReturn(abstractImageInProject);
        AbstractImage abstractImageNotInProject = builder.given_an_abstract_image();
        builder.persistAndReturn(abstractImageNotInProject);

        Project project = builder.given_a_project();
        builder.given_an_image_instance(abstractImageInProject, project);

        Page<AbstractImage> images = null;
        images = abstractImageService.list(project, new ArrayList<>(), Pageable.unpaged());
        assertThat(images.getContent()).contains(abstractImageInProject);
        assertThat(images.getContent()).doesNotContain(abstractImageNotInProject);

    }

    @Test
    @WithMockUser(username = "list_all_image_by_user_storage")
    void list_all_image_by_user_storage() {
        User user = builder.given_a_user("list_all_image_by_user_storage");
        Storage storage = builder.given_a_storage(user);
        UploadedFile uploadedFile = builder.given_a_uploaded_file();
        uploadedFile.setStorage(storage);

        AbstractImage abstractImageFromUserStorage = builder.given_an_abstract_image();
        abstractImageFromUserStorage.setOriginalFilename("match");
        abstractImageFromUserStorage.setUploadedFile(uploadedFile);
        builder.persistAndReturn(abstractImageFromUserStorage);

        AbstractImage abstractImageFromAnotherStorage = builder.given_an_abstract_image();
        abstractImageFromAnotherStorage.setOriginalFilename("match");
        builder.persistAndReturn(abstractImageFromAnotherStorage);

        Page<AbstractImage> images = null;
        images = abstractImageService.list(null, new ArrayList<>(List.of(new SearchParameterEntry("originalFilename", SearchOperation.ilike, "match"))), Pageable.unpaged());
        assertThat(images.getContent()).contains(abstractImageFromUserStorage);
        assertThat(images.getContent()).doesNotContain(abstractImageFromAnotherStorage);
    }

}
