package be.cytomine.service.ontology;

import java.util.Optional;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.domain.ontology.*;
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.repository.ontology.AlgoAnnotationTermRepository;
import be.cytomine.service.CommandService;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.JsonObject;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_SUPER_ADMIN", username = "superadminjob")
@Transactional
public class AlgoAnnotationTermServiceTests {

    @Autowired
    AlgoAnnotationTermService algoAnnotationTermService;

    @Autowired
    AlgoAnnotationTermRepository algoAnnotationTermRepository;

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    CommandService commandService;

    @Autowired
    EntityManager entityManager;

    @Test
    void list_algo_annotation_term_for_annotation_with_success() {
        AlgoAnnotationTerm algoAnnotationTerm = builder.given_an_algo_annotation_term();
        AlgoAnnotationTerm algoAnnotationTermFromAnotherAnnotation = builder.given_an_algo_annotation_term();

        assertThat(algoAnnotationTermService.list(
                AnnotationDomain.getAnnotationDomain(entityManager, algoAnnotationTerm.getAnnotationIdent())))
                .contains(algoAnnotationTerm)
                .doesNotContain(algoAnnotationTermFromAnotherAnnotation);
    }

    @Test
    void count_algo_annotation_term_for_project() {
        AlgoAnnotationTerm algoAnnotationTerm = builder.given_an_algo_annotation_term();
        AlgoAnnotationTerm algoAnnotationTermFromAnotherProject = builder.given_an_algo_annotation_term();
        assertThat(algoAnnotationTermService.count(algoAnnotationTerm.getProject())).isEqualTo(1);
    }

    @Test
    void find_algo_annotation_term_with_success() {
        AlgoAnnotationTerm algoAnnotationTerm = builder.given_an_algo_annotation_term();
        Optional<AlgoAnnotationTerm> result = algoAnnotationTermService.find
                (AnnotationDomain.getAnnotationDomain(entityManager, algoAnnotationTerm.getAnnotationIdent()), algoAnnotationTerm.getTerm(), algoAnnotationTerm.getUserJob());
        assertThat(result).isPresent();
        assertThat(algoAnnotationTerm).isEqualTo(result.get());
    }

    @Test
    void find_algo_annotation_term_with_success_without_user() {
        AlgoAnnotationTerm algoAnnotationTerm = builder.given_an_algo_annotation_term();
        Optional<AlgoAnnotationTerm> result = algoAnnotationTermService.find
                (AnnotationDomain.getAnnotationDomain(entityManager, algoAnnotationTerm.getAnnotationIdent()), algoAnnotationTerm.getTerm(), null);
        assertThat(result).isPresent();
        assertThat(algoAnnotationTerm).isEqualTo(result.get());
    }

    @Test
    void add_valid_algo_annotation_term_with_success() {
        AlgoAnnotation algoAnnotation = builder.given_a_algo_annotation();
        AlgoAnnotationTerm algoAnnotationTerm = builder.given_a_not_persisted_algo_annotation_term(algoAnnotation);
        CommandResponse commandResponse = algoAnnotationTermService.add(algoAnnotationTerm.toJsonObject());
        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        assertThat(algoAnnotationTermRepository
                .findByAnnotationIdentAndTermAndUserJob(
                        algoAnnotation.getId(),
                        algoAnnotationTerm.getTerm(),
                        builder.given_superadmin_job())
        ).isPresent();
        commandService.undo();
        assertThat(algoAnnotationTermRepository
                .findByAnnotationIdentAndTermAndUserJob(
                        algoAnnotation.getId(),
                        algoAnnotationTerm.getTerm(),
                        builder.given_superadmin_job())
        ).isEmpty();
        commandService.redo();
        assertThat(algoAnnotationTermRepository
                .findByAnnotationIdentAndTermAndUserJob(
                        algoAnnotation.getId(),
                        algoAnnotationTerm.getTerm(),
                        builder.given_superadmin_job())
        ).isPresent();
    }

    @Test
    void add_valid_annotation_term_with_direct_method_success() {
        AlgoAnnotation algoAnnotation = builder.given_a_algo_annotation();
        AlgoAnnotationTerm algoAnnotationTerm = builder.given_a_not_persisted_algo_annotation_term(algoAnnotation);
        CommandResponse commandResponse = algoAnnotationTermService.addAlgoAnnotationTerm(
                algoAnnotation,
                algoAnnotationTerm.getTerm().getId(),
                algoAnnotationTerm.getUserJob().getId(),
                algoAnnotationTerm.getUserJob(),
                null
        );
        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
    }

    @Test
    void add_annotation_term_fails_if_term_is_from_other_ontology() {
        AlgoAnnotation algoAnnotation = builder.given_a_algo_annotation();
        AlgoAnnotationTerm algoAnnotationTerm = builder.given_a_not_persisted_algo_annotation_term(algoAnnotation);
        algoAnnotationTerm.setTerm(builder.given_a_term(builder.given_an_ontology()));
        Assertions.assertThrows(WrongArgumentException.class, () -> {
            algoAnnotationTermService.add(algoAnnotationTerm.toJsonObject());
        });
    }

    @Test
    void delete_annotation_term_with_success() {
        AlgoAnnotationTerm algoAnnotationTerm = builder.given_an_algo_annotation_term();
        AnnotationDomain annotationDomain = AnnotationDomain.getAnnotationDomain(entityManager, algoAnnotationTerm.getAnnotationIdent());

        CommandResponse commandResponse =
                algoAnnotationTermService.delete(algoAnnotationTerm, null, null, true);

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        assertThat(algoAnnotationTermService.find(annotationDomain, algoAnnotationTerm.getTerm(), algoAnnotationTerm.getUserJob())).isEmpty();

        commandService.undo();

        assertThat(algoAnnotationTermService.find(annotationDomain, algoAnnotationTerm.getTerm(), algoAnnotationTerm.getUserJob())).isPresent();

        commandService.redo();

        assertThat(algoAnnotationTermService.find(annotationDomain, algoAnnotationTerm.getTerm(), algoAnnotationTerm.getUserJob())).isEmpty();
    }
}
