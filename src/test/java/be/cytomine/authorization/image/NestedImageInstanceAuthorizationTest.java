package be.cytomine.authorization.image;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.authorization.CRUDAuthorizationTest;
import be.cytomine.domain.image.AbstractImage;
import be.cytomine.domain.image.CompanionFile;
import be.cytomine.domain.image.NestedImageInstance;
import be.cytomine.service.PermissionService;
import be.cytomine.service.image.CompanionFileService;
import be.cytomine.service.image.NestedImageInstanceService;
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
import static org.springframework.security.acls.domain.BasePermission.READ;
import static org.springframework.security.acls.domain.BasePermission.WRITE;

@AutoConfigureMockMvc
@SpringBootTest(classes = CytomineCoreApplication.class)
@Transactional
public class NestedImageInstanceAuthorizationTest extends CRUDAuthorizationTest {

    private NestedImageInstance nestedImageInstance = null;

    @Autowired
    NestedImageInstanceService nestedImageInstanceService;

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    SecurityACLService securityACLService;

    @Autowired
    PermissionService permissionService;

    @BeforeEach
    public void before() throws Exception {
        if (nestedImageInstance == null) {
            nestedImageInstance = builder.given_a_nested_image_instance();
            initACL(nestedImageInstance.container());
        }
    }

    @Test
    @WithMockUser(username = SUPERADMIN)
    public void admin_can_list() {
        assertThat(nestedImageInstanceService.list(nestedImageInstance.getParent())).contains(nestedImageInstance);
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_can_list(){
        assertThat(nestedImageInstanceService.list(nestedImageInstance.getParent())).contains(nestedImageInstance);
    }

    @Test
    @WithMockUser(username = USER_NO_ACL)
    public void user_with_no_read_cannot_list(){
        expectForbidden(() -> nestedImageInstanceService.list(nestedImageInstance.getParent()));
    }


    @Override
    public void when_i_get_domain() {
        nestedImageInstanceService.get(nestedImageInstance.getId());
    }

    @Override
    protected void when_i_add_domain() {
        NestedImageInstance nestedImageInstance = builder.given_a_not_persisted_nested_image_instance();
        nestedImageInstance.setProject(this.nestedImageInstance.getProject());
        nestedImageInstance.setBaseImage(builder.given_an_abstract_image());
        nestedImageInstanceService.add(nestedImageInstance.toJsonObject());
    }

    @Override
    public void when_i_edit_domain() {
        nestedImageInstanceService.update(nestedImageInstance, nestedImageInstance.toJsonObject());
    }

    @Override
    protected void when_i_delete_domain() {
        NestedImageInstance nestedImageInstanceToDelete = nestedImageInstance;
        nestedImageInstanceService.delete(nestedImageInstanceToDelete, null, null, true);
    }

    @Override
    protected Optional<Permission> minimalPermissionForCreate() {
        return Optional.of(READ);
    }

    @Override
    protected Optional<Permission> minimalPermissionForDelete() {
        return Optional.of(READ);
    }

    @Override
    protected Optional<Permission> minimalPermissionForEdit() {
        return Optional.of(READ);
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
