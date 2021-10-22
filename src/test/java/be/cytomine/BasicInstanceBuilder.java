package be.cytomine;

import be.cytomine.domain.ontology.Ontology;
import be.cytomine.domain.ontology.Relation;
import be.cytomine.domain.ontology.RelationTerm;
import be.cytomine.domain.ontology.Term;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.UUID;

@Component
@Transactional
public class BasicInstanceBuilder {

    EntityManager em;

    TransactionTemplate transactionTemplate;

    private static User defaultUser;

    public BasicInstanceBuilder(EntityManager em, TransactionTemplate transactionTemplate) {
        this.em = em;
        this.transactionTemplate = transactionTemplate;
        this.transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                given_default_user();
            }
        });
    }

    public User given_default_user() {
        if (defaultUser == null) {
            defaultUser = given_a_user();
        }
        return defaultUser;
    }

    public User given_a_user() {
        return persistAndReturn(given_a_user_not_persisted());
    }

    public static User given_a_user_not_persisted() {
        //User user2 = new User();
        User user = new User();
        user.setFirstname("firstname");
        user.setLastname("lastname");
        user.setUsername(UUID.randomUUID().toString());
        user.setEmail(UUID.randomUUID() + "@example.com");
        user.setPublicKey(UUID.randomUUID().toString());
        user.setPrivateKey(UUID.randomUUID().toString());
        user.setPassword(UUID.randomUUID().toString());
        user.setOrigin("unkown");
        return user;
    }


    public Term given_a_term() {
        return persistAndReturn(given_a_not_persisted_term(given_an_ontology()));
    }

    public Term given_a_term(Ontology ontology) {
        return persistAndReturn(given_a_not_persisted_term(ontology));
    }

    public static Term given_a_not_persisted_term(Ontology ontology) {
        Term term = new Term();
        term.setName(UUID.randomUUID().toString());
        term.setOntology(ontology);
        term.setColor("blue");
        return term;
    }

    public Relation given_a_relation() {
        return (Relation)em.createQuery("SELECT relation FROM Relation relation WHERE relation.name LIKE 'parent'").getResultList().get(0);
    }

    public RelationTerm given_a_relation_term() {
        Ontology ontology = given_an_ontology();
        return given_a_relation_term(given_a_term(ontology), given_a_term(ontology));
    }

    public RelationTerm given_a_relation_term(Term term1, Term term2) {
        return given_a_relation_term(given_a_relation(), term1, term2);
    }

    public RelationTerm given_a_relation_term(Relation relation, Term term1, Term term2) {
        return persistAndReturn(given_a_not_persisted_relation_term(relation, term1, term2));
    }

    public static RelationTerm given_a_not_persisted_relation_term(Relation relation, Term term1, Term term2) {
        RelationTerm relationTerm = new RelationTerm();
        relationTerm.setRelation(relation);
        relationTerm.setTerm1(term1);
        relationTerm.setTerm2(term2);

        return relationTerm;
    }

    public Ontology given_an_ontology() {
        return persistAndReturn(given_a_not_persisted_ontology());
    }

    public static Ontology given_a_not_persisted_ontology() {
        Ontology ontology = new Ontology();
        ontology.setName(UUID.randomUUID().toString());
        ontology.setUser(defaultUser);
        return ontology;
    }

    public Project given_a_project() {
        return persistAndReturn(given_a_not_persisted_project());
    }

    public Project given_a_project_with_ontology(Ontology ontology) {
        Project project =  given_a_not_persisted_project();
        project.setOntology(ontology);
        return persistAndReturn(project);
    }

    public static Project given_a_not_persisted_project() {
        Project project = new Project();
        project.setName(UUID.randomUUID().toString());
        project.setOntology(null);
        project.setCountAnnotations(0);
        return project;
    }

    public <T> T persistAndReturn(T instance) {
        em.persist(instance);
        em.flush();
        return instance;
    }
}
