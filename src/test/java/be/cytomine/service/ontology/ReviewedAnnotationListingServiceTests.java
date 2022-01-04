package be.cytomine.service.ontology;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.image.SliceInstance;
import be.cytomine.domain.ontology.ReviewedAnnotation;
import be.cytomine.domain.ontology.Term;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.repository.ReviewedAnnotationListing;
import be.cytomine.repository.ontology.ReviewedAnnotationRepository;
import be.cytomine.service.AnnotationListingService;
import be.cytomine.service.CommandService;
import be.cytomine.service.command.TransactionService;
import be.cytomine.service.dto.AnnotationResult;
import com.vividsolutions.jts.io.ParseException;
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
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_SUPER_ADMIN", username = "superadmin")
@Transactional
public class ReviewedAnnotationListingServiceTests {

    @Autowired
    ReviewedAnnotationService reviewedAnnotationService;

    @Autowired
    ReviewedAnnotationRepository reviewedAnnotationRepository;

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
    void search_reviewed_annotation_by_project() {
        ReviewedAnnotation reviewedAnnotation = builder.given_a_reviewed_annotation();
        ReviewedAnnotation reviewedAnnotationFromAnotherProject = builder.given_a_reviewed_annotation();

        ReviewedAnnotationListing reviewedAnnotationListing = new ReviewedAnnotationListing(entityManager);
        reviewedAnnotationListing.setProject(reviewedAnnotation.getProject().getId());
        assertThat(annotationListingService.listGeneric(reviewedAnnotationListing)
                .stream().map(x -> ((AnnotationResult) x).get("id")))
                .contains(reviewedAnnotation.getId())
                .doesNotContain(reviewedAnnotationFromAnotherProject.getId());
    }

    @Test
    void search_reviewed_annotation_by_image() {
        ReviewedAnnotation reviewedAnnotation = builder.given_a_reviewed_annotation();
        ReviewedAnnotation reviewedAnnotationFromAnotherImage = builder.given_a_reviewed_annotation();

        ReviewedAnnotationListing reviewedAnnotationListing = new ReviewedAnnotationListing(entityManager);
        reviewedAnnotationListing.setImage(reviewedAnnotation.getImage().getId());
        assertThat(annotationListingService.listGeneric(reviewedAnnotationListing)
                .stream().map(x -> ((AnnotationResult) x).get("id")))
                .contains(reviewedAnnotation.getId())
                .doesNotContain(reviewedAnnotationFromAnotherImage.getId());
    }

    @Test
    void search_reviewed_annotation_by_images() {
        ReviewedAnnotation reviewedAnnotation = builder.given_a_reviewed_annotation();
        ReviewedAnnotation reviewedAnnotationFromAnotherImage = builder.given_a_reviewed_annotation();
        reviewedAnnotationFromAnotherImage.setImage(builder.given_an_image_instance(reviewedAnnotation.getProject()));

        ReviewedAnnotationListing reviewedAnnotationListing = new ReviewedAnnotationListing(entityManager);
        reviewedAnnotationListing.setImages(Arrays.asList(reviewedAnnotation.getImage().getId(), reviewedAnnotationFromAnotherImage.getImage().getId()));
        assertThat(annotationListingService.listGeneric(reviewedAnnotationListing)
                .stream().map(x -> ((AnnotationResult) x).get("id")))
                .contains(reviewedAnnotation.getId())
                .contains(reviewedAnnotationFromAnotherImage.getId());
    }

    @Test
    void search_reviewed_annotation_by_images_from_different_project_fails() {
        ReviewedAnnotation reviewedAnnotation = builder.given_a_reviewed_annotation();
        ReviewedAnnotation reviewedAnnotationFromAnotherImage = builder.given_a_reviewed_annotation();

        ReviewedAnnotationListing reviewedAnnotationListing = new ReviewedAnnotationListing(entityManager);
        reviewedAnnotationListing.setImages(Arrays.asList(reviewedAnnotation.getImage().getId(), reviewedAnnotationFromAnotherImage.getImage().getId()));
        Assertions.assertThrows(WrongArgumentException.class, () -> annotationListingService.listGeneric(reviewedAnnotationListing));
    }


    @Test
    void search_reviewed_annotation_by_users() {
        ReviewedAnnotation reviewedAnnotation = builder.given_a_reviewed_annotation();

        ReviewedAnnotationListing reviewedAnnotationListing = new ReviewedAnnotationListing(entityManager);
        reviewedAnnotationListing.setImages(Arrays.asList(reviewedAnnotation.getImage().getId()));
        reviewedAnnotationListing.setUser(reviewedAnnotation.getUser().getId());
        assertThat(annotationListingService.listGeneric(reviewedAnnotationListing)
                .stream().map(x -> ((AnnotationResult) x).get("id")))
                .contains(reviewedAnnotation.getId());

        reviewedAnnotationListing = new ReviewedAnnotationListing(entityManager);
        reviewedAnnotationListing.setImages(Arrays.asList(reviewedAnnotation.getImage().getId()));
        reviewedAnnotationListing.setUser(builder.given_a_user().getId());
        assertThat(annotationListingService.listGeneric(reviewedAnnotationListing)
                .stream().map(x -> ((AnnotationResult) x).get("id")))
                .doesNotContain(reviewedAnnotation.getId());
    }

    static Map<String, String> POLYGONES = Map.of(
            "a", "POLYGON ((1 1, 2 1, 2 2, 1 2, 1 1))",
            "b", "POLYGON ((1 3, 2 3, 2 5, 1 5, 1 3))",
            "c", "POLYGON ((3 1, 5 1,  5 3, 3 3, 3 1))",
            "d", "POLYGON ((4 4,8 4, 8 7,4 7,4 4))",
            "e", "POLYGON ((2 2, 3 2, 3 4, 2 4, 2 2))"
    ); //e intersect a,b and c

    @Test
    void search_reviewed_annotation_by_terms() throws ParseException {

        SliceInstance sliceInstance = builder.given_a_slice_instance();
        User user1 = builder.given_a_user();
        User user2 = builder.given_a_user();

        Term term1 = builder.given_a_term(sliceInstance.getProject().getOntology());
        Term term2 = builder.given_a_term(sliceInstance.getProject().getOntology());

        ReviewedAnnotation a1 = builder.given_a_reviewed_annotation(sliceInstance, POLYGONES.get("a"), user1, term1);
        ReviewedAnnotation a2 = builder.given_a_reviewed_annotation(sliceInstance, POLYGONES.get("b"), user1, term2);
        ReviewedAnnotation a3 = builder.given_a_reviewed_annotation(sliceInstance, POLYGONES.get("c"), user2, term1);
        ReviewedAnnotation a4 = builder.given_a_reviewed_annotation(sliceInstance, POLYGONES.get("d"), user2, term2);


        ReviewedAnnotationListing reviewedAnnotationListing = new ReviewedAnnotationListing(entityManager);
        reviewedAnnotationListing.setProject(sliceInstance.getProject().getId());
        assertThat(annotationListingService.listGeneric(reviewedAnnotationListing)
                .stream().map(x -> ((AnnotationResult) x).get("id")))
                .contains(a1.getId())
                .contains(a2.getId())
                .contains(a3.getId())
                .contains(a4.getId());

        reviewedAnnotationListing = new ReviewedAnnotationListing(entityManager);
        reviewedAnnotationListing.setProject(sliceInstance.getProject().getId());
        reviewedAnnotationListing.setTerm(term1.getId());
        assertThat(annotationListingService.listGeneric(reviewedAnnotationListing)
                .stream().map(x -> ((AnnotationResult) x).get("id")))
                .contains(a1.getId())
                .doesNotContain(a2.getId())
                .contains(a3.getId())
                .doesNotContain(a4.getId());

        reviewedAnnotationListing = new ReviewedAnnotationListing(entityManager);
        reviewedAnnotationListing.setProject(sliceInstance.getProject().getId());
        reviewedAnnotationListing.setTerms(Arrays.asList(term1.getId(), term2.getId()));
        assertThat(annotationListingService.listGeneric(reviewedAnnotationListing)
                .stream().map(x -> ((AnnotationResult) x).get("id")))
                .contains(a1.getId())
                .contains(a2.getId())
                .contains(a3.getId())
                .contains(a4.getId());
    }


    @Test
    void search_reviewed_annotation_by_bbox() throws ParseException {

        SliceInstance sliceInstance = builder.given_a_slice_instance();
        User user1 = builder.given_a_user();
        User user2 = builder.given_a_user();

        Term term1 = builder.given_a_term(sliceInstance.getProject().getOntology());
        Term term2 = builder.given_a_term(sliceInstance.getProject().getOntology());

        ReviewedAnnotation a1 = builder.given_a_reviewed_annotation(sliceInstance, POLYGONES.get("a"), user1, term1);
        ReviewedAnnotation a2 = builder.given_a_reviewed_annotation(sliceInstance, POLYGONES.get("b"), user1, term2);
        ReviewedAnnotation a3 = builder.given_a_reviewed_annotation(sliceInstance, POLYGONES.get("c"), user2, term1);
        ReviewedAnnotation a4 = builder.given_a_reviewed_annotation(sliceInstance, POLYGONES.get("d"), user2, term2);

        ReviewedAnnotationListing reviewedAnnotationListing = new ReviewedAnnotationListing(entityManager);
        reviewedAnnotationListing.setSlice(sliceInstance.getId());
        reviewedAnnotationListing.setBbox("POLYGON ((2 2, 3 2, 3 4, 2 4, 2 2))");
        assertThat(annotationListingService.listGeneric(reviewedAnnotationListing)
                .stream().map(x -> ((AnnotationResult) x).get("id")))
                .contains(a1.getId())
                .contains(a2.getId())
                .contains(a3.getId())
                .doesNotContain(a4.getId());

    }

    @Test
    void search_reviewed_annotation_by_image_and_review_user() throws ParseException {

        SliceInstance sliceInstance = builder.given_a_slice_instance();
        User user1 = builder.given_a_user();
        User user2 = builder.given_a_user();

        Term term1 = builder.given_a_term(sliceInstance.getProject().getOntology());
        Term term2 = builder.given_a_term(sliceInstance.getProject().getOntology());

        ReviewedAnnotation a1 = builder.given_a_reviewed_annotation(sliceInstance, POLYGONES.get("a"), user1, term1);
        ReviewedAnnotation a2 = builder.given_a_reviewed_annotation(sliceInstance, POLYGONES.get("b"), user1, term2);
        ReviewedAnnotation a3 = builder.given_a_reviewed_annotation(sliceInstance, POLYGONES.get("c"), user2, term1);
        ReviewedAnnotation a4 = builder.given_a_reviewed_annotation(sliceInstance, POLYGONES.get("d"), user2, term2);


        a1.setReviewUser(user1);
        a2.setReviewUser(user1);
        a3.setReviewUser(user2);
        a4.setReviewUser(user2);

        ReviewedAnnotationListing reviewedAnnotationListing = new ReviewedAnnotationListing(entityManager);
        reviewedAnnotationListing.setSlice(sliceInstance.getId());
        reviewedAnnotationListing.setReviewUsers(Arrays.asList(user1.getId()));
        assertThat(annotationListingService.listGeneric(reviewedAnnotationListing)
                .stream().map(x -> ((AnnotationResult) x).get("id")))
                .contains(a1.getId())
                .contains(a2.getId())
                .doesNotContain(a3.getId())
                .doesNotContain(a4.getId());


        reviewedAnnotationListing = new ReviewedAnnotationListing(entityManager);
        reviewedAnnotationListing.setSlice(sliceInstance.getId());
        reviewedAnnotationListing.setReviewUsers(Arrays.asList(user2.getId()));
        assertThat(annotationListingService.listGeneric(reviewedAnnotationListing)
                .stream().map(x -> ((AnnotationResult) x).get("id")))
                .doesNotContain(a1.getId())
                .doesNotContain(a2.getId())
                .contains(a3.getId())
                .contains(a4.getId());
    }
}