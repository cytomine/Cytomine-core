package be.cytomine.domain;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.domain.ontology.Ontology;
import be.cytomine.domain.ontology.RelationTerm;
import be.cytomine.domain.ontology.Term;
import be.cytomine.domain.project.Project;
import be.cytomine.exceptions.AlreadyExistException;
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.repository.ontology.RelationTermRepository;
import be.cytomine.repository.ontology.TermRepository;
import be.cytomine.service.CommandService;
import be.cytomine.service.command.TransactionService;
import be.cytomine.service.ontology.TermService;
import be.cytomine.utils.CommandResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import javax.transaction.Transactional;

import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_SUPER_ADMIN", username = "superadmin")
@Transactional
public class CytomineDomainTests {

    @Autowired
    BasicInstanceBuilder builder;


    @Test
    void assign_id_automatically() {
        Ontology ontology = BasicInstanceBuilder.given_a_not_persisted_ontology();
        assertThat(ontology.getId()).isNull();
        ontology = builder.persistAndReturn(ontology);
        assertThat(ontology.getId()).isPositive();
    }

    @Test
    void assign_created_date() {
        Date beforeCreate = new Date();
        Ontology ontology = BasicInstanceBuilder.given_a_not_persisted_ontology();
        assertThat(ontology.getCreated()).isNull();
        ontology = builder.persistAndReturn(ontology);
        Date afterCreate = new Date();
        assertThat(ontology.getCreated()).isBetween(beforeCreate, afterCreate, true, true);
    }

    @Test
    void assign_updated_date() {
        Date beforeCreate = new Date();
        Ontology ontology = BasicInstanceBuilder.given_a_not_persisted_ontology();
        assertThat(ontology.getUpdated()).isNull();
        ontology = builder.persistAndReturn(ontology);
        Date afterCreate = new Date();
        assertThat(ontology.getUpdated()).isBetween(beforeCreate, afterCreate, true, true);

        Date beforeUpdate = new Date();
        ontology.setName(UUID.randomUUID().toString());
        ontology = builder.persistAndReturn(ontology);
        Date afterUpdate = new Date();

        assertThat(ontology.getUpdated()).isBetween(beforeUpdate, afterUpdate, true, true);
    }
}
