package be.cytomine.service.ontology;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.TestUtils;
import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.image.SliceInstance;
import be.cytomine.domain.ontology.Term;
import be.cytomine.domain.ontology.UserAnnotation;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.dto.AnnotationLight;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.repository.AnnotationListing;
import be.cytomine.repository.UserAnnotationListing;
import be.cytomine.repository.ontology.UserAnnotationRepository;
import be.cytomine.service.AnnotationListingService;
import be.cytomine.service.CommandService;
import be.cytomine.service.command.TransactionService;
import be.cytomine.service.dto.AnnotationResult;
import be.cytomine.service.ontology.UserAnnotationService;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.JsonObject;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import org.apache.commons.lang3.time.DateUtils;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_SUPER_ADMIN", username = "superadmin")
@Transactional
public class AnnotationListingServiceTests {

    @Autowired
    UserAnnotationService userAnnotationService;

    @Autowired
    UserAnnotationRepository userAnnotationRepository;

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    CommandService commandService;

    @Autowired
    TransactionService transactionService;

    @Autowired
    EntityManager entityManager;

    @Autowired
    AnnotationListingService annotationListingService;

    @Test
    void search_user_annotation_by_project() {
        UserAnnotation userAnnotation = builder.given_a_user_annotation();
        UserAnnotation userAnnotationFromAnotherProject = builder.given_a_user_annotation();

        UserAnnotationListing userAnnotationListing = new UserAnnotationListing(entityManager);
        userAnnotationListing.setProject(userAnnotation.getProject().getId());
        assertThat(annotationListingService.listGeneric(userAnnotationListing)
                .stream().map(x->((AnnotationResult)x).get("id")))
                .contains(userAnnotation.getId())
                .doesNotContain(userAnnotationFromAnotherProject.getId());
    }

    @Test
    void search_user_annotation_by_image() {
        UserAnnotation userAnnotation = builder.given_a_user_annotation();
        UserAnnotation userAnnotationFromAnotherImage = builder.given_a_user_annotation();

        UserAnnotationListing userAnnotationListing = new UserAnnotationListing(entityManager);
        userAnnotationListing.setImage(userAnnotation.getImage().getId());
        assertThat(annotationListingService.listGeneric(userAnnotationListing)
                .stream().map(x->((AnnotationResult)x).get("id")))
                .contains(userAnnotation.getId())
                .doesNotContain(userAnnotationFromAnotherImage.getId());
    }

    @Test
    void search_user_annotation_by_images() {
        UserAnnotation userAnnotation = builder.given_a_user_annotation();
        UserAnnotation userAnnotationFromAnotherImage = builder.given_a_user_annotation();
        userAnnotationFromAnotherImage.setImage(builder.given_an_image_instance(userAnnotation.getProject()));

        UserAnnotationListing userAnnotationListing = new UserAnnotationListing(entityManager);
        userAnnotationListing.setImages(Arrays.asList(userAnnotation.getImage().getId(), userAnnotationFromAnotherImage.getImage().getId()));
        assertThat(annotationListingService.listGeneric(userAnnotationListing)
                .stream().map(x->((AnnotationResult)x).get("id")))
                .contains(userAnnotation.getId())
                .doesNotContain(userAnnotationFromAnotherImage.getId());
    }

    @Test
    void search_user_annotation_by_images_from_different_project_fails() {
        UserAnnotation userAnnotation = builder.given_a_user_annotation();
        UserAnnotation userAnnotationFromAnotherImage = builder.given_a_user_annotation();

        UserAnnotationListing userAnnotationListing = new UserAnnotationListing(entityManager);
        userAnnotationListing.setImages(Arrays.asList(userAnnotation.getImage().getId(), userAnnotationFromAnotherImage.getImage().getId()));
        Assertions.assertThrows(WrongArgumentException.class, () -> annotationListingService.listGeneric(userAnnotationListing));
    }

    @Test
    void search_user_annotation_by_terms() {
        UserAnnotation userAnnotation = builder.given_an_annotation_term().getUserAnnotation();
        UserAnnotation userAnnotationWithDifferentTerm
                = builder.given_a_user_annotation();
        userAnnotationWithDifferentTerm.setImage(userAnnotation.getImage());
        userAnnotationWithDifferentTerm.setProject(userAnnotation.getProject());
        builder.given_an_annotation_term(userAnnotationWithDifferentTerm, builder.given_a_term(userAnnotationWithDifferentTerm.getProject().getOntology()));

        entityManager.refresh(userAnnotation);
        entityManager.refresh(userAnnotationWithDifferentTerm);

        UserAnnotationListing userAnnotationListing = new UserAnnotationListing(entityManager);
        userAnnotationListing.setTerms(Arrays.asList(userAnnotation.getTerms().get(0).getId()));
        assertThat(annotationListingService.listGeneric(userAnnotationListing)
                .stream().map(x->((AnnotationResult)x).get("id")))
                .contains(userAnnotation.getId())
                .doesNotContain(userAnnotationWithDifferentTerm.getId());

        userAnnotationListing.setTerms(Arrays.asList(userAnnotation.getTerms().get(0).getId(), userAnnotationWithDifferentTerm.getTerms().get(0).getId()));
        assertThat(annotationListingService.listGeneric(userAnnotationListing)
                .stream().map(x->((AnnotationResult)x).get("id")))
                .contains(userAnnotation.getId(), userAnnotationWithDifferentTerm.getId());
    }


    @Test
    void search_user_annotation_with_multiple_terms() {
        UserAnnotation userAnnotation = builder.given_an_annotation_term().getUserAnnotation();
        builder.given_an_annotation_term(userAnnotation, builder.given_a_term(userAnnotation.getProject().getOntology()));

        entityManager.refresh(userAnnotation);

        UserAnnotationListing userAnnotationListing = new UserAnnotationListing(entityManager);
        userAnnotationListing.setTerms(Arrays.asList(userAnnotation.getTerms().get(0).getId(), userAnnotation.getTerms().get(1).getId()));
        assertThat(annotationListingService.listGeneric(userAnnotationListing)
                .stream().map(x->((AnnotationResult)x).get("id")))
                .contains(userAnnotation.getId());
        AnnotationResult annotationResult = (AnnotationResult) annotationListingService.listGeneric(userAnnotationListing).get(0);
        assertThat(annotationResult.get("id")).isEqualTo(userAnnotation.getId());
        assertThat((List<Long>)annotationResult.get("terms")).containsExactlyElementsOf(userAnnotation.getTerms().stream().map(CytomineDomain::getId).collect(Collectors.toList()));
    }






}
