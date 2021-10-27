package be.cytomine;

import be.cytomine.domain.image.UploadedFile;
import be.cytomine.domain.image.server.Storage;
import be.cytomine.domain.middleware.ImageServer;
import be.cytomine.domain.ontology.Ontology;
import be.cytomine.domain.ontology.Relation;
import be.cytomine.domain.ontology.RelationTerm;
import be.cytomine.domain.ontology.Term;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.repository.security.UserRepository;
import be.cytomine.service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.UUID;

import static org.springframework.security.acls.domain.BasePermission.ADMINISTRATION;

@Component
@Transactional
public class BasicInstanceBuilder {

    EntityManager em;

    TransactionTemplate transactionTemplate;

    UserRepository userRepository;

    PermissionService permissionService;

    private static User defaultUser;

    public BasicInstanceBuilder(EntityManager em, TransactionTemplate transactionTemplate, UserRepository userRepository, PermissionService permissionService) {
        this.em = em;
        this.transactionTemplate = transactionTemplate;
        this.transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                given_default_user();
            }
        });
        this.userRepository = userRepository;
        this.permissionService = permissionService;
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

    public User given_superadmin() {
        return userRepository.findByUsernameLikeIgnoreCase("superadmin").orElseThrow(() -> new ObjectNotFoundException("superadmin not in db"));
    }

    public static User given_a_user_not_persisted() {
        //User user2 = new User();
        User user = new User();
        user.setFirstname("firstname");
        user.setLastname("lastname");
        user.setUsername(randomString());
        user.setEmail(UUID.randomUUID() + "@example.com");
        user.setPublicKey(randomString());
        user.setPrivateKey(randomString());
        user.setPassword(randomString());
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
        term.setName(randomString());
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
        ontology.setName(randomString());
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
        project.setName(randomString());
        project.setOntology(null);
        project.setCountAnnotations(0);
        return project;
    }

    public <T> T persistAndReturn(T instance) {
        em.persist(instance);
        em.flush();
        return instance;
    }

    public UploadedFile given_a_uploaded_file() {
        UploadedFile uploadedFile = given_a_not_persisted_uploaded_file();
        return persistAndReturn(uploadedFile);
    }

    public UploadedFile given_a_not_persisted_uploaded_file() {
        UploadedFile uploadedFile = new UploadedFile();
        uploadedFile.setStorage(given_a_storage());
        uploadedFile.setUser(given_superadmin());
        uploadedFile.setFilename(randomString());
        uploadedFile.setOriginalFilename(randomString());
        uploadedFile.setExt("tiff");
        uploadedFile.setImageServer(given_an_image_server());
        uploadedFile.setContentType("tiff/type");
        uploadedFile.setSize(100L);
        uploadedFile.setParent(null);
        return uploadedFile;
    }

    public Storage given_a_storage() {
        Storage storage = given_a_not_persisted_storage();
        return persistAndReturn(storage);
    }

    public Storage given_a_not_persisted_storage() {
        Storage storage = new Storage();
        storage.setName(randomString());
        storage.setUser(given_superadmin());
        storage = persistAndReturn(storage);
        permissionService.addPermission(storage, storage.getUser().getUsername(), ADMINISTRATION, storage.getUser());
        return storage;
    }

    private static String randomString() {
        return UUID.randomUUID().toString();
    }

    public ImageServer given_an_image_server() {
        ImageServer imageServer = given_a_not_persisted_image_server();
        return persistAndReturn(imageServer);
    }

    public ImageServer given_a_not_persisted_image_server() {
        ImageServer imageServer = new ImageServer();
        imageServer.setName(randomString());
        imageServer.setUrl("http://" + randomString());
        imageServer.setBasePath("/data");
        imageServer.setAvailable(true);
        return imageServer;
    }

}
