package be.cytomine.authorization.ontology;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.authorization.CRUDAuthorizationTest;
import be.cytomine.domain.ontology.ReviewedAnnotation;
import be.cytomine.domain.project.EditingMode;
import be.cytomine.domain.project.Project;
import be.cytomine.exceptions.CytomineMethodNotYetImplementedException;
import be.cytomine.service.PermissionService;
import be.cytomine.service.ontology.ReviewedAnnotationService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.utils.JsonObject;
import com.vividsolutions.jts.io.ParseException;
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
public class ReviewedAnnotationAuthorizationTest extends CRUDAuthorizationTest {


    private ReviewedAnnotation reviewedAnnotation = null;

    @Autowired
    ReviewedAnnotationService reviewedAnnotationService;

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    SecurityACLService securityACLService;

    @Autowired
    PermissionService permissionService;

    @BeforeEach
    public void before() throws Exception {
        if (reviewedAnnotation == null) {
            reviewedAnnotation = builder.given_a_reviewed_annotation();
            initUser();
            initACL(reviewedAnnotation.container());
        }
        reviewedAnnotation.getProject().setMode(EditingMode.CLASSIC);
        builder.persistAndReturn(reviewedAnnotation.getProject());
    }

    @Test
    @WithMockUser(username = SUPERADMIN)
    public void admin_can_list_all_reviewed_annotations() {
        expectOK (() -> { reviewedAnnotationService.listLight(); });
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_cannot_list_all_reviewed_annotations(){
        expectForbidden(() -> {
            reviewedAnnotationService.listLight();
        });
    }


    @Test
    @WithMockUser(username = SUPERADMIN)
    public void admin_can_update_annotation_in_restricted_project() {
        ReviewedAnnotation reviewedAnnotation
                = builder.given_a_reviewed_annotation();
        reviewedAnnotation.setProject(this.reviewedAnnotation.getProject());
        Project project = (Project) reviewedAnnotation.container();
        project.setMode(EditingMode.RESTRICTED);
        builder.persistAndReturn(project);
        expectOK (() -> { when_i_add_domain(); });
        expectOK (() -> { when_i_edit_domain(); });
        expectOK (() -> { when_i_delete_domain(); });
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_cannot_update_annotation_in_restricted_project() {
        ReviewedAnnotation reviewedAnnotation
                = builder.given_a_reviewed_annotation();
        reviewedAnnotation.setProject(this.reviewedAnnotation.getProject());
        Project project = (Project) reviewedAnnotation.container();
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
    @WithMockUser(username = USER_ACL_ADMIN)
    public void project_admin_can_correct_annotation_from_other_user() throws ParseException {
//        String basedLocation = "POLYGON ((0 0, 0 5000, 10000 5000, 10000 0, 0 0))";
//        String addedLocation = "POLYGON ((0 5000, 10000 5000, 10000 10000, 0 10000, 0 5000))";
//        String expectedLocation = "POLYGON ((0 0, 0 10000, 10000 10000, 10000 0, 0 0))";
//
//        // Annotation created by another user
//        ReviewedAnnotation annotation
//                = builder.given_a_not_persisted_reviewed_annotation(this.reviewedAnnotation.getProject());
//        annotation.setUser(builder.given_a_user(USER_ACL_READ));
//        annotation.setLocation(new WKTReader().read(basedLocation));
        throw new CytomineMethodNotYetImplementedException("");

        // + TODO: do same test for simple user
    }


    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_can_delete_its_annotation_even_if_other_users_has_set_terms(){
        throw new CytomineMethodNotYetImplementedException("");
        //TODO: see ReviewedAnnotationSecutiryTests . testDeleteReviewedAnnotationWithTerm
    }

    @Override
    public void when_i_get_domain() {
        reviewedAnnotationService.get(reviewedAnnotation.getId());
    }

    @Override
    protected void when_i_add_domain() {
        JsonObject jsonObject = builder.given_a_not_persisted_reviewed_annotation(this.reviewedAnnotation.getProject()).toJsonObject();
        reviewedAnnotationService.add(jsonObject);
    }

    @Override
    public void when_i_edit_domain() {
        reviewedAnnotationService.update(reviewedAnnotation, reviewedAnnotation.toJsonObject());
    }

    @Override
    protected void when_i_delete_domain() {
        ReviewedAnnotation annotation = builder.persistAndReturn(builder.given_a_not_persisted_reviewed_annotation(this.reviewedAnnotation.getProject()));
        reviewedAnnotationService.delete(annotation, null, null, true);
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
