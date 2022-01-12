package be.cytomine.service.ontology;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.domain.ontology.AnnotationDomain;
import be.cytomine.domain.ontology.SharedAnnotation;
import be.cytomine.domain.ontology.UserAnnotation;
import be.cytomine.repository.ontology.SharedAnnotationRepository;
import be.cytomine.service.CommandService;
import be.cytomine.service.command.TransactionService;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.JsonObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import javax.transaction.Transactional;
import java.util.List;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_SUPER_ADMIN", username = "superadmin")
@Transactional
public class SharedAnnotationServiceTests {

    @Autowired
    SharedAnnotationService sharedAnnotationService;

    @Autowired
    SharedAnnotationRepository sharedAnnotationRepository;

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    CommandService commandService;

    @Autowired
    TransactionService transactionService;

    @Test
    void list_all_sharedAnnotation_with_success() {
        SharedAnnotation sharedAnnotation = builder.given_a_shared_annotation();
        assertThat(sharedAnnotation).isIn(sharedAnnotationService.list());
    }

    @Test
    void get_sharedAnnotation_with_success() {
        SharedAnnotation sharedAnnotation = builder.given_a_shared_annotation();
        assertThat(sharedAnnotation).isEqualTo(sharedAnnotationService.get(sharedAnnotation.getId()));
    }

    @Test
    void get_unexisting_sharedAnnotation_return_null() {
        assertThat(sharedAnnotationService.get(0L)).isNull();
    }

    @Test
    void find_sharedAnnotation_with_success() {
        SharedAnnotation sharedAnnotation = builder.given_a_shared_annotation();
        assertThat(sharedAnnotationService.find(sharedAnnotation.getId()).isPresent());
        assertThat(sharedAnnotation).isEqualTo(sharedAnnotationService.find(sharedAnnotation.getId()).get());
    }

    @Test
    void find_unexisting_sharedAnnotation_return_empty() {
        assertThat(sharedAnnotationService.find(0L)).isEmpty();
    }

    @Test
    void list_all_sharedAnnotation_by_annotation() {
        UserAnnotation annotation = builder.given_a_user_annotation();
        SharedAnnotation sharedAnnotation = builder.given_a_shared_annotation(annotation);
        assertThat(sharedAnnotation).isIn(sharedAnnotationService.listComments(annotation));
        assertThat(builder.given_a_shared_annotation()).isNotIn(sharedAnnotationService.listComments(annotation));
    }

    @Test
    void add_valid_sharedAnnotation_with_success() {
        AnnotationDomain annotationDomain = builder.given_a_user_annotation();
        SharedAnnotation sharedAnnotation = builder.given_a_not_persisted_shared_annotation();
        sharedAnnotation.setAnnotation(annotationDomain);
        JsonObject json = sharedAnnotation.toJsonObject();
        json.put("subject", "subject for test mail");
        json.put("message", "message for test mail");
        json.put("users", List.of(builder.given_superadmin().getId()));
        json.put("annotationIdent", sharedAnnotation.getAnnotationIdent());
        json.put("annotationClassName", sharedAnnotation.getAnnotationClassName());

        CommandResponse commandResponse = sharedAnnotationService.add(sharedAnnotation.toJsonObject());

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);


        assertThat(sharedAnnotationService.listComments(annotationDomain).size()).isEqualTo(1);

    }

    @Test
    void add_valid_sharedAnnotation_with_receivers_not_on_the_platform() {
        AnnotationDomain annotationDomain = builder.given_a_user_annotation();
        SharedAnnotation sharedAnnotation = builder.given_a_not_persisted_shared_annotation();
        sharedAnnotation.setAnnotation(annotationDomain);
        JsonObject json = sharedAnnotation.toJsonObject();
        json.put("subject", "subject for test mail");
        json.put("message", "message for test mail");
        json.put("users", List.of(builder.given_superadmin().getId()));
        json.put("annotationIdent", sharedAnnotation.getAnnotationIdent());
        json.put("annotationClassName", sharedAnnotation.getAnnotationClassName());
        json.remove("receivers");
        json.put("emails", List.of("IamNotOnCytomine@tooBad.com"));
        CommandResponse commandResponse = sharedAnnotationService.add(sharedAnnotation.toJsonObject());

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);

        assertThat(sharedAnnotationService.listComments(annotationDomain).size()).isEqualTo(1);
    }



    @Test
    void delete_sharedAnnotation_with_success() {
        SharedAnnotation sharedAnnotation = builder.given_a_shared_annotation();

        CommandResponse commandResponse = sharedAnnotationService.delete(sharedAnnotation, null, null, true);

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        assertThat(sharedAnnotationService.find(sharedAnnotation.getId()).isEmpty());
    }
}
