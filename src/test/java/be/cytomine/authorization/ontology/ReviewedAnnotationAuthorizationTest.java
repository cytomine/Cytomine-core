package be.cytomine.authorization.ontology;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.authorization.CRUDAuthorizationTest;
import be.cytomine.domain.ontology.ReviewedAnnotation;
import be.cytomine.domain.ontology.UserAnnotation;
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

import java.util.Date;
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
    @WithMockUser(username = USER_ACL_ADMIN)
    public void user_can_review_annotation_if_reviewer() {
        UserAnnotation annotation
                = builder.given_a_user_annotation();
        annotation.getImage().setReviewStart(new Date());
        annotation.getImage().setReviewUser(builder.given_a_user(USER_ACL_ADMIN));
        expectOK (() -> {
            reviewedAnnotationService.reviewAnnotation(annotation.getId(), null);
        });
    }


    @Test
    @WithMockUser(username = USER_ACL_ADMIN)
    public void user_cannot_review_annotation_if_not_reviewer() {
        UserAnnotation annotation
                = builder.given_a_user_annotation();
        annotation.getImage().setReviewStart(new Date());
        annotation.getImage().setReviewUser(builder.given_a_user(SUPERADMIN)); // someone else
        expectForbidden (() -> {
            reviewedAnnotationService.reviewAnnotation(annotation.getId(), null);
        });
    }

    @Test
    @WithMockUser(username = CREATOR)
    public void creator_can_edit_its_annotation() {
        ReviewedAnnotation reviewedAnnotation
                = builder.given_a_reviewed_annotation();
        reviewedAnnotation.getImage().setReviewStart(new Date());
        reviewedAnnotation.getImage().setReviewUser(builder.given_a_user(USER_ACL_ADMIN));
        reviewedAnnotation.setReviewUser(userRepository.findByUsernameLikeIgnoreCase(CREATOR).get());
        expectOK (() -> {
            reviewedAnnotationService.update(reviewedAnnotation, reviewedAnnotation.toJsonObject(), null);
        });
    }

    @Test
    @WithMockUser(username = CREATOR)
    public void creator_can_delete_its_annotation() {
        ReviewedAnnotation reviewedAnnotation
                = builder.given_a_reviewed_annotation();
        reviewedAnnotation.getImage().setReviewStart(new Date());
        reviewedAnnotation.getImage().setReviewUser(builder.given_a_user(USER_ACL_ADMIN));
        reviewedAnnotation.setReviewUser(userRepository.findByUsernameLikeIgnoreCase(CREATOR).get());
        expectOK (() -> {
            reviewedAnnotationService.delete(reviewedAnnotation, null, null, false);
        });
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
        return Optional.of("CREATOR");
    }

    @Override
    protected Optional<String> minimalRoleForDelete() {
        return Optional.of("CREATOR");
    }

    @Override
    protected Optional<String> minimalRoleForEdit() {
        return Optional.of("CREATOR");
    }
}
