package be.cytomine.service.ontology;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.domain.ontology.AnnotationDomain;
import be.cytomine.domain.ontology.AnnotationTrack;
import be.cytomine.domain.ontology.UserAnnotation;
import be.cytomine.exceptions.AlreadyExistException;
import be.cytomine.repository.ontology.AnnotationTrackRepository;
import be.cytomine.service.CommandService;
import be.cytomine.service.command.TransactionService;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.JsonObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import javax.transaction.Transactional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_SUPER_ADMIN", username = "superadmin")
@Transactional
public class AnnotationTrackServiceTests {

    @Autowired
    AnnotationTrackService annotationTrackService;

    @Autowired
    AnnotationTrackRepository annotationTrackRepository;

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    CommandService commandService;

    @Autowired
    TransactionService transactionService;


    @Test
    void get_annotationTrack_with_success() {
        AnnotationTrack annotationTrack = builder.given_a_annotation_track();
        assertThat(annotationTrack).isEqualTo(annotationTrackService.get(annotationTrack.getId()));
    }

    @Test
    void get_unexisting_annotationTrack_return_null() {
        assertThat(annotationTrackService.get(0L)).isNull();
    }

    @Test
    void find_annotationTrack_with_success() {
        AnnotationTrack annotationTrack = builder.given_a_annotation_track();
        assertThat(annotationTrackService.find(annotationTrack.getId()).isPresent());
        assertThat(annotationTrack).isEqualTo(annotationTrackService.find(annotationTrack.getId()).get());
    }

    @Test
    void find_unexisting_annotationTrack_return_empty() {
        assertThat(annotationTrackService.find(0L)).isEmpty();
    }

    @Test
    void find_annotationTrack_with_annotation_and_track() {
        AnnotationTrack annotationTrack = builder.given_a_annotation_track();
        UserAnnotation annotation = builder.given_a_user_annotation();
        annotationTrack.setAnnotation(annotation);
        assertThat(annotationTrackService.find(annotation, annotationTrack.getTrack()).isPresent());
    }
    

    @Test
    void list_all_annotationTrack_by_track() {
        AnnotationTrack annotationTrack = builder.given_a_annotation_track();
        assertThat(annotationTrack).isIn(annotationTrackService.list(annotationTrack.getTrack()));
        assertThat(annotationTrackService.list(builder.given_a_track())).isEmpty();
    }

    @Test
    void list_all_annotationTrack_by_annotation() {
        AnnotationTrack annotationTrack = builder.given_a_annotation_track();
        UserAnnotation annotation = builder.given_a_user_annotation();
        annotationTrack.setAnnotation(annotation);
        assertThat(annotationTrack).isIn(annotationTrackService.list(annotation));
        assertThat(annotationTrackService.list(builder.given_a_algo_annotation())).isEmpty();
    }

    @Test
    void add_valid_annotationTrack_with_success() {
        AnnotationTrack annotationTrack = builder.given_a_not_persisted_annotation_track();

        CommandResponse commandResponse = annotationTrackService.add(annotationTrack.toJsonObject());

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
    }

    @Test
    void add_valid_annotationTrack_already_exists() {
        AnnotationTrack annotationTrack = builder.given_a_annotation_track();

        Assertions.assertThrows(AlreadyExistException.class, () -> {
            annotationTrackService.add(annotationTrack.toJsonObject()
                    .withChange("id", null));
        });
    }

    @Test
    void add_valid_annotationTrack_with_direct_method() {
        AnnotationTrack annotationTrack = builder.given_a_not_persisted_annotation_track();
        CommandResponse commandResponse = annotationTrackService.addAnnotationTrack(
                annotationTrack.getAnnotationClassName(),
                annotationTrack.getAnnotationIdent(),
                annotationTrack.getTrack().getId(),
                annotationTrack.getSlice().getId(),
                null
        );
        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
    }

    @Test
    void delete_annotationTrack_with_success() {
        AnnotationTrack annotationTrack = builder.given_a_annotation_track();

        CommandResponse commandResponse = annotationTrackService.delete(annotationTrack, null, null, true);

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        assertThat(annotationTrackService.find(annotationTrack.getId()).isEmpty());
    }
}
