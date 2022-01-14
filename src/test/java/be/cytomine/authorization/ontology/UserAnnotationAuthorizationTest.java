package be.cytomine.authorization.ontology;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.authorization.CRUDAuthorizationTest;
import be.cytomine.domain.image.SliceInstance;
import be.cytomine.domain.ontology.AnnotationTerm;
import be.cytomine.domain.ontology.Term;
import be.cytomine.domain.ontology.UserAnnotation;
import be.cytomine.domain.project.EditingMode;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.CytomineMethodNotYetImplementedException;
import be.cytomine.service.PermissionService;
import be.cytomine.service.ontology.UserAnnotationService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.utils.JsonObject;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@AutoConfigureMockMvc
@SpringBootTest(classes = CytomineCoreApplication.class)
@Transactional
public class UserAnnotationAuthorizationTest extends CRUDAuthorizationTest {


    private UserAnnotation userAnnotation = null;

    @Autowired
    UserAnnotationService userAnnotationService;

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    SecurityACLService securityACLService;

    @Autowired
    PermissionService permissionService;

    @BeforeEach
    public void before() throws Exception {
        if (userAnnotation == null) {
            userAnnotation = builder.given_a_user_annotation();
            initUser();
            initACL(userAnnotation.container());
        }
        userAnnotation.getProject().setMode(EditingMode.CLASSIC);
        builder.persistAndReturn(userAnnotation.getProject());
    }

    @Test
    @WithMockUser(username = SUPERADMIN)
    public void admin_can_list_all_user_Annotations() {
        expectOK (() -> { userAnnotationService.listLight(); });
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_cannot_list_all_user_Annotations(){
        expectForbidden(() -> {
            userAnnotationService.listLight();
        });
    }


    @Test
    @WithMockUser(username = SUPERADMIN)
    public void admin_can_update_annotation_in_restricted_project() {
        UserAnnotation userAnnotation
                = builder.given_a_user_annotation();
        userAnnotation.setProject(this.userAnnotation.getProject());
        Project project = (Project) userAnnotation.container();
        project.setMode(EditingMode.RESTRICTED);
        builder.persistAndReturn(project);
        expectOK (() -> { when_i_add_domain(); });
        expectOK (() -> { when_i_edit_domain(); });
        expectOK (() -> { when_i_delete_domain(); });
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_cannot_update_annotation_in_restricted_project() {
        UserAnnotation userAnnotation
                = builder.given_a_user_annotation();
        userAnnotation.setProject(this.userAnnotation.getProject());
        Project project = (Project) userAnnotation.container();
        project.setMode(EditingMode.RESTRICTED);
        builder.persistAndReturn(project);
        expectForbidden (() -> { when_i_add_domain(); });
        expectForbidden (() -> { when_i_edit_domain(); });
        expectForbidden (() -> { when_i_delete_domain(); });

        project.setMode(EditingMode.CLASSIC);
        builder.persistAndReturn(project);
        expectOK (() -> { when_i_add_domain(); });
        expectOK (() -> { when_i_edit_domain(); });
        expectOK (() -> { when_i_delete_domain(); });
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_can_delete_its_annotation_even_if_other_users_has_set_terms(){
        AnnotationTerm annotationTerm = builder.given_an_annotation_term(userAnnotation);
        userAnnotationService.delete(userAnnotation, null, null, false);
    }

    @Override
    public void when_i_get_domain() {
        userAnnotationService.get(userAnnotation.getId());
    }

    @Override
    protected void when_i_add_domain() {
        JsonObject jsonObject = builder.given_a_not_persisted_user_annotation(this.userAnnotation.getProject()).toJsonObject();
        userAnnotationService.add(jsonObject);
    }

    @Override
    public void when_i_edit_domain() {
        userAnnotationService.update(userAnnotation, userAnnotation.toJsonObject());
    }

    @Override
    protected void when_i_delete_domain() {
        UserAnnotation annotation = builder.persistAndReturn(builder.given_a_not_persisted_user_annotation(this.userAnnotation.getProject()));
        userAnnotationService.delete(annotation, null, null, true);
    }


    @Override
    protected Optional<Permission> minimalPermissionForCreate() {
        return Optional.of(BasePermission.READ);
    }

    @Override
    protected Optional<Permission> minimalPermissionForDelete() {
        return Optional.of(BasePermission.READ);
    }

    @Override
    protected Optional<Permission> minimalPermissionForEdit() {
        return Optional.of(BasePermission.READ);
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
