package be.cytomine;

import be.cytomine.authorization.AbstractAuthorizationTest;
import be.cytomine.domain.image.*;
import be.cytomine.domain.image.server.Storage;
import be.cytomine.domain.middleware.ImageServer;
import be.cytomine.domain.ontology.Ontology;
import be.cytomine.domain.ontology.Relation;
import be.cytomine.domain.ontology.RelationTerm;
import be.cytomine.domain.ontology.Term;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.SecUserSecRole;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.repository.image.MimeRepository;
import be.cytomine.repository.security.SecRoleRepository;
import be.cytomine.repository.security.SecUserSecRoleRepository;
import be.cytomine.repository.security.UserRepository;
import be.cytomine.service.PermissionService;
import be.cytomine.service.database.BootstrapDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.acls.model.Permission;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.security.acls.domain.BasePermission.ADMINISTRATION;

@Component
@Transactional
public class BasicInstanceBuilder {

    public static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";

    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    public static final String ROLE_USER = "ROLE_USER";

    public static final String ROLE_GUEST = "ROLE_GUEST";

    EntityManager em;

    TransactionTemplate transactionTemplate;

    UserRepository userRepository;

    PermissionService permissionService;

    SecRoleRepository secRoleRepository;

    MimeRepository mimeRepository;

    ApplicationBootstrap applicationBootstrap;

    private static User defaultUser;

    public BasicInstanceBuilder(
            EntityManager em,
            TransactionTemplate transactionTemplate,
            UserRepository userRepository,
            PermissionService permissionService,
            SecRoleRepository secRoleRepository,
            MimeRepository mimeRepository,
            ApplicationBootstrap applicationBootstrap) {
        applicationBootstrap.init();
        this.em = em;
        this.userRepository = userRepository;
        this.permissionService = permissionService;
        this.secRoleRepository = secRoleRepository;
        this.mimeRepository = mimeRepository;
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
        return given_a_user(randomString());
    }

    public User given_a_user(String username) {
        User user = persistAndReturn(given_a_user_not_persisted());
        user.setUsername(username);
        addRole(user, ROLE_USER);
        return user;
    }

    public void addRole(User user, String authority) {
        SecUserSecRole secUserSecRole = new SecUserSecRole();
        secUserSecRole.setSecUser(user);
        secUserSecRole.setSecRole(secRoleRepository.findByAuthority(authority).orElseThrow(() -> new ObjectNotFoundException("authority " + authority + " does not exists")));
        em.persist(secUserSecRole);
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

    public void addUserToProject(Project project, String username, Permission permission) {
        permissionService.addPermission(project, username, permission);
    }

    public void addUserToProject(Project project, String username) {
        permissionService.addPermission(project, username, ADMINISTRATION);
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
        return given_a_storage(given_superadmin());
    }

    public Storage given_a_storage(User user) {
        Storage storage = given_a_not_persisted_storage();
        storage.setUser(user);
        storage = persistAndReturn(storage);
        permissionService.addPermission(storage, storage.getUser().getUsername(), ADMINISTRATION, storage.getUser());
        return storage;
    }

    public Storage given_a_not_persisted_storage(User user) {
        Storage storage = new Storage();
        storage.setName(randomString());
        storage.setUser(user);
        return storage;
    }

    public Storage given_a_not_persisted_storage() {
        return given_a_not_persisted_storage(given_superadmin());
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

    public AbstractImage given_an_abstract_image() {
        AbstractImage imageServer = given_a_not_persisted_abstract_image();
        return persistAndReturn(imageServer);
    }

    public AbstractImage given_a_not_persisted_abstract_image() {
        AbstractImage image = new AbstractImage();
        image.setUploadedFile(given_a_uploaded_file());
        image.setOriginalFilename(randomString());
        return image;
    }


    public ImageInstance given_an_image_instance(AbstractImage abstractImage, Project project) {
        ImageInstance imageInstance = given_a_not_persisted_image_instance(abstractImage, project);
        return persistAndReturn(imageInstance);
    }

    public ImageInstance given_a_not_persisted_image_instance() {
        return given_a_not_persisted_image_instance(given_an_abstract_image(), given_a_project());
    }

    public ImageInstance given_a_not_persisted_image_instance(AbstractImage abstractImage, Project project) {
        ImageInstance image = new ImageInstance();
        image.setBaseImage(abstractImage);
        image.setProject(project);
        image.setUser(given_superadmin());
        return image;
    }

    public ImageInstance given_an_image_instance() {
        ImageInstance imageInstance = given_an_image_instance(given_an_abstract_image(), given_a_project());
        return persistAndReturn(imageInstance);
    }

    public AbstractSlice given_an_abstract_slice() {
        return given_an_abstract_slice(given_an_abstract_image(), given_a_uploaded_file());
    }

    public AbstractSlice given_an_abstract_slice(AbstractImage abstractImage, int c, int z, int t) {
        AbstractSlice slice = given_a_not_persisted_abstract_slice(abstractImage, given_a_uploaded_file());
        slice.setChannel(c);
        slice.setZStack(z);
        slice.setTime(t);
        return persistAndReturn(slice);
    }

    public AbstractSlice given_an_abstract_slice(AbstractImage abstractImage, UploadedFile uploadedFile) {
        return persistAndReturn(given_a_not_persisted_abstract_slice(abstractImage, uploadedFile));
    }

    public AbstractSlice given_a_not_persisted_abstract_slice() {
        return given_a_not_persisted_abstract_slice(given_an_abstract_image(), given_a_uploaded_file());
    }

    public AbstractSlice given_a_not_persisted_abstract_slice(AbstractImage abstractImage, UploadedFile uploadedFile) {
        AbstractSlice slice = new AbstractSlice();
        slice.setImage(abstractImage);
        slice.setUploadedFile(uploadedFile);
        slice.setMime(given_a_mime());
        slice.setChannel(0);
        slice.setZStack(0);
        slice.setTime(0);
        return slice;
    }

    public SliceInstance given_a_slice_instance() {
        return given_a_slice_instance(given_an_image_instance(), 0, 0, 0);
    }

    public SliceInstance given_a_slice_instance(ImageInstance imageInstance, int c, int z, int t) {
        AbstractSlice abstractSlice = given_an_abstract_slice(imageInstance.getBaseImage(), c, z, t);
        return persistAndReturn(given_a_not_persisted_slice_instance(imageInstance, abstractSlice));
    }

    public SliceInstance given_a_slice_instance(ImageInstance imageInstance, AbstractSlice abstractSlice) {
        return persistAndReturn(given_a_not_persisted_slice_instance(imageInstance, abstractSlice));
    }

    public SliceInstance given_a_not_persisted_slice_instance() {
        return given_a_not_persisted_slice_instance(given_an_image_instance(), given_an_abstract_slice());
    }

    public SliceInstance given_a_not_persisted_slice_instance(ImageInstance imageInstance, AbstractSlice abstractSlice) {
        SliceInstance slice = new SliceInstance();
        slice.setImage(imageInstance);
        slice.setProject(imageInstance.getProject());
        slice.setBaseSlice(abstractSlice);
        return slice;
    }


    public Mime given_a_mime() {
        Optional<Mime> optionalMime = mimeRepository.findByMimeType("image/pyrtiff");
        if (optionalMime.isPresent()) {
            return optionalMime.get();
        } else {
            Mime mime = new Mime();
            mime.setExtension("tif");
            mime.setMimeType("image/pyrtiff");
            return persistAndReturn(mime);
        }
    }

    public NestedImageInstance given_a_nested_image_instance() {
        return persistAndReturn(given_a_not_persisted_nested_image_instance());
    }

    public NestedImageInstance given_a_not_persisted_nested_image_instance() {
        ImageInstance parent = given_an_image_instance();
        NestedImageInstance nestedImageInstance = new NestedImageInstance();
        nestedImageInstance.setBaseImage(given_an_abstract_image());
        nestedImageInstance.setProject(parent.getProject());
        nestedImageInstance.setUser(given_superadmin());
        nestedImageInstance.setParent(parent);
        nestedImageInstance.setX(1);
        nestedImageInstance.setY(2);
        return nestedImageInstance;
    }
}
