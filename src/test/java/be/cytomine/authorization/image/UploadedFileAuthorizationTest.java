package be.cytomine.authorization.image;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.authorization.CRUDAuthorizationTest;
import be.cytomine.domain.image.AbstractImage;
import be.cytomine.domain.image.UploadedFile;
import be.cytomine.service.PermissionService;
import be.cytomine.service.image.AbstractImageService;
import be.cytomine.service.image.UploadedFileService;
import be.cytomine.service.security.SecurityACLService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.springframework.security.acls.domain.BasePermission.WRITE;

@AutoConfigureMockMvc
@SpringBootTest(classes = CytomineCoreApplication.class)
@Transactional
public class UploadedFileAuthorizationTest extends CRUDAuthorizationTest {

    // We need more flexibility:

    private UploadedFile uploadedFile = null;

    @Autowired
    UploadedFileService uploadedFileService;

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    SecurityACLService securityACLService;

    @Autowired
    PermissionService permissionService;

    @BeforeEach
    public void before() throws Exception {
        if (uploadedFile == null) {
            uploadedFile = builder.given_a_uploaded_file();
            initUser();
            initACL(uploadedFile.container());
        }
    }

    @Override
    public void when_i_get_domain() {
        uploadedFileService.get(uploadedFile.getId());
    }

    @Override
    protected void when_i_add_domain() {
        UploadedFile uploadedFileToCreate = builder.given_a_not_persisted_uploaded_file();
        uploadedFileToCreate.setStorage(uploadedFile.getStorage());
        uploadedFileService.add(uploadedFileToCreate.toJsonObject());
    }

    @Override
    public void when_i_edit_domain() {
        uploadedFileService.update(uploadedFile, uploadedFile.toJsonObject());
    }

    @Override
    protected void when_i_delete_domain() {
        UploadedFile uploadedFileToDelete = uploadedFile;
        uploadedFileService.delete(uploadedFileToDelete, null, null, true);
    }

    @Override
    protected Optional<Permission> minimalPermissionForCreate() {
        return Optional.of(WRITE);
    }

    @Override
    protected Optional<Permission> minimalPermissionForDelete() {
        return Optional.of(WRITE);
    }

    @Override
    protected Optional<Permission> minimalPermissionForEdit() {
        return Optional.of(WRITE);
    }


    @Override
    protected Optional<String> minimalRoleForCreate() {
        return Optional.of("ROLE_USER");
    }

    @Override
    protected Optional<String> minimalRoleForDelete() {
        return Optional.of("ROLE_USER");
    }

    @Override
    protected Optional<String> minimalRoleForEdit() {
        return Optional.of("ROLE_USER");
    }
}
