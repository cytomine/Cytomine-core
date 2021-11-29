package be.cytomine.service.image;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.domain.image.AbstractImage;
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.image.UploadedFile;
import be.cytomine.domain.image.server.Storage;
import be.cytomine.domain.ontology.Ontology;
import be.cytomine.domain.project.Project;
import be.cytomine.exceptions.AlreadyExistException;
import be.cytomine.exceptions.ForbiddenException;
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.repository.image.AbstractImageRepository;
import be.cytomine.repository.image.UploadedFileRepository;
import be.cytomine.service.CommandService;
import be.cytomine.service.command.TransactionService;
import be.cytomine.service.search.UploadedFileSearchParameter;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.filters.SearchOperation;
import be.cytomine.utils.filters.SearchParameterEntry;
import liquibase.pro.packaged.A;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.fail;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;


@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_SUPER_ADMIN", username = "superadmin")
@Transactional
public class UploadedFileServiceTests {

    @Autowired
    UploadedFileService uploadedFileService;

    @Autowired
    UploadedFileRepository uploadedFileRepository;

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    CommandService commandService;

    @Autowired
    TransactionService transactionService;

    @Autowired
    EntityManager entityManager;

    @Autowired
    AbstractImageRepository abstractImageRepository;

    @Test
    void list_all_uploadedFile_with_success() {
        UploadedFile uploadedFile1 = builder.given_a_uploaded_file();
        UploadedFile uploadedFile2 = builder.given_a_uploaded_file();
        assertThat(uploadedFile1).isIn(uploadedFileService.list(Pageable.unpaged()));
        assertThat(uploadedFile2).isIn(uploadedFileService.list(Pageable.unpaged()));

        assertThat(uploadedFileService.list(Pageable.ofSize(1)).getContent()).asList().hasSize(1);
        assertThat(uploadedFileService.list(Pageable.ofSize(2)).getContent()).asList().hasSize(2);
    }

    @Test
    void list_user_uploadedFile_with_success() {
        UploadedFile uploadedFile1 = builder.given_a_uploaded_file();
        UploadedFile uploadedFileNotSameUser = builder.given_a_uploaded_file();
        uploadedFileNotSameUser.setUser(builder.given_a_user());
        builder.persistAndReturn(uploadedFileNotSameUser);

        assertThat(uploadedFile1).isIn(uploadedFileService.list(builder.given_superadmin(), null, false, Pageable.unpaged()));
        assertThat(uploadedFileNotSameUser).isNotIn(uploadedFileService.list(builder.given_superadmin(), null, false, Pageable.unpaged()));

        assertThat(uploadedFileService.list(Pageable.ofSize(1)).getContent()).asList().hasSize(1);
        assertThat(uploadedFileService.list(Pageable.ofSize(2)).getContent()).asList().hasSize(2);
    }


    @Test
    void search_uploadedFile_by_user() {
        UploadedFile uploadedFile1 = builder.given_a_uploaded_file();
        UploadedFile uploadedFile2 = builder.given_a_uploaded_file();
        UploadedFile uploadedFileNotSameUser = builder.given_a_uploaded_file();
        uploadedFileNotSameUser.setUser(builder.given_a_user());
        builder.persistAndReturn(uploadedFileNotSameUser);

        List<SearchParameterEntry> searchParameter = new ArrayList<>();
        searchParameter.add(new SearchParameterEntry("user", SearchOperation.in, List.of(builder.given_superadmin().getId())));

        List<Map<String, Object>> list = uploadedFileService.list(searchParameter, "created", "desc");

        assertThat(list.size()).isGreaterThanOrEqualTo(2);
        assertThat(list.stream().map(x -> x.get("id"))).contains(uploadedFile1.getId());
        assertThat(list.stream().map(x -> x.get("id"))).contains(uploadedFile2.getId());
        assertThat(list.stream().map(x -> x.get("id"))).doesNotContain(uploadedFileNotSameUser.getId());
    }

    @Test
    void search_uploadedFile_by_original_file_name() {
        UploadedFile uploadedFile1 = builder.given_a_uploaded_file();
        uploadedFile1.setOriginalFilename("redIsDead");
        builder.persistAndReturn(uploadedFile1);
        UploadedFile uploadedFile2 = builder.given_a_uploaded_file();
        uploadedFile2.setOriginalFilename("deadline");
        builder.persistAndReturn(uploadedFile2);
        UploadedFile uploadedFileNoMatch = builder.given_a_uploaded_file();
        uploadedFileNoMatch.setOriginalFilename("veracruz");
        builder.persistAndReturn(uploadedFileNoMatch);

        List<SearchParameterEntry> searchParameter = new ArrayList<>();
        searchParameter.add(new SearchParameterEntry("originalFilename", SearchOperation.ilike, "dead"));

        List<Map<String, Object>> list = uploadedFileService.list(searchParameter, "created", "desc");

        assertThat(list.size()).isGreaterThanOrEqualTo(2);
        assertThat(list.stream().map(x -> x.get("id"))).contains(uploadedFile1.getId());
        assertThat(list.stream().map(x -> x.get("id"))).contains(uploadedFile2.getId());
        assertThat(list.stream().map(x -> x.get("id"))).doesNotContain(uploadedFileNoMatch.getId());
    }


    @Test
    void list_uploaded_file_with_hierarchical_tree() {
        UploadedFile uploadedFileToAdd = builder.given_a_uploaded_file();
        uploadedFileToAdd.setOriginalFilename("parent");
        builder.persistAndReturn(uploadedFileToAdd);


        UploadedFile uploadedfileChildToAdd = builder.given_a_uploaded_file();
        uploadedfileChildToAdd.setOriginalFilename("child");
        uploadedfileChildToAdd.setParent(uploadedFileToAdd);
        builder.persistAndReturn(uploadedfileChildToAdd);


        UploadedFile uploadedfileSubChildToAdd = builder.given_a_uploaded_file();
        uploadedfileSubChildToAdd.setParent(uploadedfileChildToAdd);
        builder.persistAndReturn(uploadedfileSubChildToAdd);


        Page<UploadedFile> list = uploadedFileService.list(builder.given_superadmin(), null, true, Pageable.unpaged());
        assertThat(list.getContent()).contains(uploadedFileToAdd);
        assertThat(list.getContent()).doesNotContain(uploadedfileChildToAdd, uploadedfileSubChildToAdd);

        list = uploadedFileService.list(builder.given_superadmin(), uploadedFileToAdd.getId(), false, Pageable.unpaged());
        assertThat(list.getContent()).contains(uploadedfileChildToAdd);
        assertThat(list.getContent()).doesNotContain(uploadedFileToAdd, uploadedfileSubChildToAdd);


        List<Map<String, Object>> maps = uploadedFileService.listHierarchicalTree(builder.given_superadmin(), uploadedFileToAdd.getId());
        assertThat(maps.stream().filter(x -> x.get("id").equals(uploadedFileToAdd.getId()))).hasSize(1);
        assertThat(maps.stream().filter(x -> x.get("id").equals(uploadedfileChildToAdd.getId()))).hasSize(1);
        assertThat(maps.stream().filter(x -> x.get("id").equals(uploadedfileSubChildToAdd.getId()))).hasSize(1);

    }

    @Test
    void test_ltree() {
        UploadedFile uploadedFileToAdd = builder.given_a_uploaded_file();
        uploadedFileToAdd.setOriginalFilename("parent");
        builder.persistAndReturn(uploadedFileToAdd);


        UploadedFile uploadedfileChildToAdd = builder.given_a_uploaded_file();
        uploadedfileChildToAdd.setOriginalFilename("child");
        uploadedfileChildToAdd.setParent(uploadedFileToAdd);
        builder.persistAndReturn(uploadedfileChildToAdd);


        UploadedFile uploadedfileSubChildToAdd = builder.given_a_uploaded_file();
        uploadedfileSubChildToAdd.setParent(uploadedfileChildToAdd);
        builder.persistAndReturn(uploadedfileSubChildToAdd);

        UploadedFile uploadedfileSubSubChildToAdd = builder.given_a_uploaded_file();
        uploadedfileSubSubChildToAdd.setParent(uploadedfileSubChildToAdd);
        builder.persistAndReturn(uploadedfileSubSubChildToAdd);


        assertThat(uploadedfileSubSubChildToAdd.getLTree()).contains(uploadedfileSubChildToAdd.getLTree());
        assertThat(uploadedfileSubChildToAdd.getLTree()).contains(uploadedfileChildToAdd.getLTree());
        assertThat(uploadedfileChildToAdd.getLTree()).contains(uploadedFileToAdd.getLTree());

        assertThat(uploadedfileSubSubChildToAdd.getParent().getId()).isEqualTo(uploadedfileSubChildToAdd.getId());
        uploadedFileService.delete(uploadedfileSubChildToAdd, null, null, false);
        entityManager.refresh(uploadedfileSubSubChildToAdd);
        assertThat(uploadedfileSubSubChildToAdd.getParent().getId()).isEqualTo(uploadedfileChildToAdd.getId());

    }

    @Test
    void search_uploadedFile_by_storage() {
        throw new RuntimeException("to implement in corporate version (?) ; some issues in the current version");
    }

    @Test
    void get_uploaded_file_by_user() {
        UploadedFile uploadedFile = builder.given_a_uploaded_file();
        assertThat(uploadedFile).isEqualTo(uploadedFileService.get(uploadedFile.getId()));
    }

    @Test
    void get_unexisting_uploadedFile_return_null() {
        assertThat(uploadedFileService.get(0L)).isNull();
    }

    @Test
    void find_uploadedFile_with_success() {
        UploadedFile uploadedFile = builder.given_a_uploaded_file();
        assertThat(uploadedFileService.find(uploadedFile.getId()).isPresent());
        assertThat(uploadedFile).isEqualTo(uploadedFileService.find(uploadedFile.getId()).get());
    }

    @Test
    void find_unexisting_uploadedFile_return_empty() {
        assertThat(uploadedFileService.find(0L)).isEmpty();
    }

    @Test
    void add_valid_uploadedFile_with_success() {
        Ontology ontology = builder.given_an_ontology();
        UploadedFile uploadedFile = builder.given_a_not_persisted_uploaded_file();

        CommandResponse commandResponse = uploadedFileService.add(uploadedFile.toJsonObject());

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        assertThat(uploadedFileService.find(commandResponse.getObject().getId())).isPresent();
        UploadedFile created = uploadedFileService.find(commandResponse.getObject().getId()).get();
    }

    @Test
    void add_uploadedFile_with_null_storage_fail() {
        UploadedFile uploadedFile = builder.given_a_not_persisted_uploaded_file();
        uploadedFile.setStorage(null);
        Assertions.assertThrows(WrongArgumentException.class, () -> {
            uploadedFileService.add(uploadedFile.toJsonObject());
        });
    }


    @Test
    void edit_valid_uploaded_file_with_success() {
        UploadedFile uploadedFile = builder.given_a_uploaded_file();

        CommandResponse commandResponse = uploadedFileService.update(uploadedFile, uploadedFile.toJsonObject().withChange("originalFilename", "NEW NAME"));

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        assertThat(uploadedFileService.find(commandResponse.getObject().getId())).isPresent();
        UploadedFile edited = uploadedFileService.find(commandResponse.getObject().getId()).get();
        assertThat(edited.getOriginalFilename()).isEqualTo("NEW NAME");
    }

    @Test
    void edit_valid_uploaded_file_storage_with_success() {
        UploadedFile uploadedFile = builder.given_a_uploaded_file();
        Storage storage = builder.given_a_storage();

        CommandResponse commandResponse = uploadedFileService.update(uploadedFile, uploadedFile.toJsonObject().withChange("storage", storage.getId()));

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        assertThat(uploadedFileService.find(commandResponse.getObject().getId())).isPresent();
        UploadedFile edited = uploadedFileService.find(commandResponse.getObject().getId()).get();
        assertThat(edited.getStorage().getId()).isEqualTo(storage.getId());
    }


    @Test
    void delete_uploadedFile_with_success() {
        UploadedFile uploadedFile = builder.given_a_uploaded_file();

        CommandResponse commandResponse = uploadedFileService.delete(uploadedFile, null, null, true);

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        assertThat(uploadedFileService.find(uploadedFile.getId()).isEmpty());
    }

    @Test
    void delete_uploadedFile_with_dependencies_with_success() {
        AbstractImage abstractImage = builder.given_an_abstract_image();

        CommandResponse commandResponse = uploadedFileService.delete(abstractImage.getUploadedFile(), null, null, true);

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        assertThat(uploadedFileService.find(abstractImage.getUploadedFile().getId()).isEmpty());
        assertThat(abstractImageRepository.findById(abstractImage.getId()).isEmpty());
    }

    @Test
    void delete_uploaded_file_with_image_in_project() {
        ImageInstance imageInstance = builder.given_an_image_instance();
        Assertions.assertThrows(ForbiddenException.class, () ->
                uploadedFileService.delete(imageInstance.getBaseImage().getUploadedFile(), null, null, false)
        );
    }
}
