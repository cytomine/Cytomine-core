package be.cytomine;

/*
* Copyright (c) 2009-2022. Authors: see NOTICE file.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.image.*;
import be.cytomine.domain.image.server.Storage;
import be.cytomine.domain.meta.*;
import be.cytomine.domain.middleware.ImageServer;
import be.cytomine.domain.ontology.*;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.project.ProjectDefaultLayer;
import be.cytomine.domain.project.ProjectRepresentativeUser;
import be.cytomine.domain.security.*;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.repository.image.MimeRepository;
import be.cytomine.repository.security.SecRoleRepository;
import be.cytomine.repository.security.SecUserRepository;
import be.cytomine.service.PermissionService;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import org.springframework.security.acls.model.Permission;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.security.acls.domain.BasePermission.ADMINISTRATION;

@Component
@Transactional
public class BasicInstanceBuilder {

    private User SUPERADMIN;

    public static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";

    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    public static final String ROLE_USER = "ROLE_USER";

    public static final String ROLE_GUEST = "ROLE_GUEST";

    EntityManager em;

    TransactionTemplate transactionTemplate;

    PermissionService permissionService;

    SecRoleRepository secRoleRepository;

    MimeRepository mimeRepository;

    SecUserRepository secUserRepository;

    ApplicationBootstrap applicationBootstrap;

    private static User aUser;
    private static User anAdmin;
    private static User aGuest;

    public BasicInstanceBuilder(
            EntityManager em,
            TransactionTemplate transactionTemplate,
            SecUserRepository secUserRepository,
            PermissionService permissionService,
            SecRoleRepository secRoleRepository,
            MimeRepository mimeRepository,
            ApplicationBootstrap applicationBootstrap) {
        if (secRoleRepository.count()==0) {
            applicationBootstrap.init();
        }
        this.em = em;
        this.secUserRepository = secUserRepository;
        this.permissionService = permissionService;
        this.secRoleRepository = secRoleRepository;
        this.mimeRepository = mimeRepository;
        this.transactionTemplate = transactionTemplate;

        this.transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                aUser = (User) secUserRepository.findByUsernameLikeIgnoreCase("user")
                        .orElseGet(() -> given_default_user());
                anAdmin = (User) secUserRepository.findByUsernameLikeIgnoreCase("admin")
                        .orElseGet(() -> given_default_admin());
            }
        });
    }

    public User given_default_user() {
        if (aUser == null) {
            aUser = given_a_user("user");
        }
        return aUser;
    }

    public User given_default_admin() {
        if (anAdmin == null) {
            anAdmin = given_a_admin("admin");
        }
        return anAdmin;
    }


    public User given_default_guest() {
        if (aGuest == null) {
            aGuest = given_a_guest("guest");
        }
        return aGuest;
    }

    public User given_a_user() {
        return given_a_user(randomString());
    }

    public User given_a_guest() {
        return given_a_guest(randomString());
    }

    public User given_a_admin() {
        return given_a_admin(randomString());
    }

    public User given_a_user(String username) {
        User user = persistAndReturn(given_a_not_persisted_user());
        user.setUsername(username);
        user = persistAndReturn(user);
        addRole(user, ROLE_USER);
        return user;
    }

    public User given_a_guest(String username) {
        User user = persistAndReturn(given_a_not_persisted_user());
        user.setUsername(username);
        user = persistAndReturn(user);
        addRole(user, ROLE_GUEST);
        return user;
    }

    public User given_a_admin(String username) {
        User user = persistAndReturn(given_a_not_persisted_user());
        user.setUsername(username);
        user = persistAndReturn(user);
        addRole(user, ROLE_ADMIN);
        return user;
    }


    public UserJob given_a_user_job() {
        return given_a_user_job(randomString());
    }

    public UserJob given_a_user_job(String username) {
        UserJob user = persistAndReturn(given_a_user_job_not_persisted(given_a_user()));
        user.setUsername(username);
        user = persistAndReturn(user);
        addRole(user, ROLE_USER);
        return user;
    }

    public void addRole(SecUser user, String authority) {
        SecUserSecRole secUserSecRole = new SecUserSecRole();
        secUserSecRole.setSecUser(user);
        secUserSecRole.setSecRole(secRoleRepository.findByAuthority(authority).orElseThrow(() -> new ObjectNotFoundException("authority " + authority + " does not exists")));
        em.persist(secUserSecRole);
        em.flush();
        em.refresh(user);
    }

    public User given_superadmin() {
        return (User)secUserRepository.findByUsernameLikeIgnoreCase("superadmin").orElseThrow(() -> new ObjectNotFoundException("superadmin not in db"));
    }

    public UserJob given_superadmin_job() {
        return (UserJob)secUserRepository.findByUsernameLikeIgnoreCase("superadminjob").orElseThrow(() -> new ObjectNotFoundException("superadminjob not in db"));
    }

    public static User given_a_not_persisted_user() {
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


    public static UserJob given_a_user_job_not_persisted(User creator) {
        //User user2 = new User();
        UserJob user = new UserJob();
        user.setUsername(randomString());
        user.setUser(creator);
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
        ontology.setUser(aUser);
        return ontology;
    }

    public Project given_a_project_with_user(User user) {
        Project project = given_a_project();
        addUserToProject(project, user.getUsername(), ADMINISTRATION);
        return project;
    }

    public Project given_a_project() {
        return persistAndReturn(given_a_project_with_ontology(given_an_ontology()));
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
        permissionService.addPermission(project, username, permission, this.given_superadmin());
    }

    public void addUserToProject(Project project, String username) {
        permissionService.addPermission(project, username, ADMINISTRATION, this.given_superadmin());
    }

    public void addUserToOntology(Ontology ontology, String username, Permission permission) {
        permissionService.addPermission(ontology, username, permission);
    }

    public void addUserToOntology(Ontology ontology, String username) {
        permissionService.addPermission(ontology, username, ADMINISTRATION, this.given_superadmin());
    }

    public void addUserToStorage(Storage storage, String username, Permission permission) {
        permissionService.addPermission(storage, username, permission);
    }

    public void addUserToStorage(Storage storage, String username) {
        permissionService.addPermission(storage, username, ADMINISTRATION, this.given_superadmin());
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
        uploadedFile.setExt("tif");
        uploadedFile.setImageServer(given_an_image_server());
        uploadedFile.setContentType("image/pyrtiff");
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

    public static Storage given_a_not_persisted_storage(User user) {
        Storage storage = new Storage();
        storage.setName(randomString());
        storage.setUser(user);
        return storage;
    }

    public Storage given_a_not_persisted_storage() {
        return given_a_not_persisted_storage(given_superadmin());
    }

    public static String randomString() {
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
        image.setWidth(16000);
        image.setHeight(16000);
        return image;
    }


    public ImageInstance given_an_image_instance(AbstractImage abstractImage, Project project) {
        ImageInstance imageInstance = given_a_not_persisted_image_instance(abstractImage, project);
        return persistAndReturn(imageInstance);
    }

    public ImageInstance given_a_not_persisted_image_instance(Project project) {
        return given_a_not_persisted_image_instance(given_an_abstract_image(), project);
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

    public ImageInstance given_an_image_instance(Project project) {
        ImageInstance imageInstance = given_an_image_instance(given_an_abstract_image(), project);
        return persistAndReturn(imageInstance);
    }

    public ImageInstance given_an_image_instance() {
        ImageInstance imageInstance = given_an_image_instance(given_an_abstract_image(), given_a_project());
        return persistAndReturn(imageInstance);
    }

    public AbstractSlice given_an_abstract_slice() {
        AbstractImage abstractImage = given_an_abstract_image();
        UploadedFile uploadedFile = given_a_uploaded_file();
        uploadedFile.setStorage(abstractImage.getUploadedFile().getStorage());
        persistAndReturn(uploadedFile);
        return given_an_abstract_slice(abstractImage, uploadedFile);
    }

    public AbstractSlice given_an_abstract_slice(AbstractImage abstractImage, int c, int z, int t) {
        AbstractSlice slice = given_a_not_persisted_abstract_slice(abstractImage, abstractImage.getUploadedFile());
        slice.setMime(given_a_mime("openslide/mrxs"));
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

    public SliceInstance given_a_slice_instance(Project project) {
        AbstractImage image = given_an_abstract_image();
        SliceInstance sliceInstance = given_a_slice_instance(given_an_image_instance(image, project), given_an_abstract_slice(image, 0,0,0));
        return persistAndReturn(sliceInstance);
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

    public Mime given_a_mime(String mimeType) {
        return mimeRepository.findByMimeType(mimeType).orElseThrow(() -> new ObjectNotFoundException("MimeType", mimeType));
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

    public Property given_a_property(CytomineDomain cytomineDomain) {
        return persistAndReturn(given_a_not_persisted_property(cytomineDomain, "key", "value"));
    }

    public Property given_a_property(CytomineDomain cytomineDomain, String key, String value) {
        return persistAndReturn(given_a_not_persisted_property(cytomineDomain, key, value));
    }

    public Property given_a_not_persisted_property(CytomineDomain cytomineDomain, String key, String value) {
        Property property = new Property();
        property.setDomain(cytomineDomain);
        property.setKey(key);
        property.setValue(value);
        return property;
    }

    public CompanionFile given_a_companion_file(AbstractImage abstractImage) {
        return persistAndReturn(given_a_not_persisted_companion_file(abstractImage));
    }

    public CompanionFile given_a_not_persisted_companion_file(AbstractImage abstractImage) {
        CompanionFile companionFile = new CompanionFile();
        companionFile.setImage(abstractImage);
        companionFile.setUploadedFile(abstractImage.getUploadedFile());
        companionFile.setFilename(randomString());
        companionFile.setOriginalFilename(randomString());
        companionFile.setType(companionFile.getImage().getContentType());
        companionFile.setProgress(50);
        return companionFile;
    }

    public UserAnnotation given_a_not_persisted_user_annotation() {
        UserAnnotation annotation = new UserAnnotation();
        annotation.setUser(given_superadmin());
        try {
            annotation.setLocation(new WKTReader().read("POLYGON ((1983 2168, 2107 2160, 2047 2074, 1983 2168))"));
        } catch (ParseException ignored) {

        }
        annotation.setSlice(given_a_slice_instance());
        annotation.setImage(annotation.getSlice().getImage());
        annotation.setProject(annotation.getImage().getProject());
        return annotation;
    }

    public UserAnnotation given_a_user_annotation(Project project) {
        UserAnnotation annotation = given_a_not_persisted_user_annotation();
        annotation.getSlice().setProject(project);
        annotation.getImage().setProject(project);
        annotation.setProject(project);
        return persistAndReturn(annotation);
    }

    public UserAnnotation given_a_not_persisted_user_annotation(Project project) {
        UserAnnotation annotation = given_a_not_persisted_user_annotation();
        annotation.getSlice().setProject(project);
        annotation.getImage().setProject(project);
        annotation.setProject(project);
        return annotation;
    }


    public UserAnnotation given_a_not_persisted_user_annotation(SliceInstance sliceInstance) {
        UserAnnotation annotation = new UserAnnotation();
        annotation.setUser(given_superadmin());
        try {
            annotation.setLocation(new WKTReader().read("POLYGON ((1983 2168, 2107 2160, 2047 2074, 1983 2168))"));
        } catch (ParseException ignored) {

        }
        annotation.setSlice(sliceInstance);
        annotation.setImage(annotation.getSlice().getImage());
        annotation.setProject(annotation.getImage().getProject());
        return annotation;
    }

    public UserAnnotation given_a_user_annotation(SliceInstance sliceInstance) {
        return persistAndReturn(given_a_user_annotation(sliceInstance, given_superadmin()));
    }

    public UserAnnotation given_a_user_annotation(SliceInstance sliceInstance, User user) {
        UserAnnotation annotation = given_a_not_persisted_user_annotation();
        annotation.setSlice(sliceInstance);
        annotation.setImage(sliceInstance.getImage());
        annotation.setProject(sliceInstance.getProject());
        annotation.setUser(user);
        return persistAndReturn(annotation);
    }

    public UserAnnotation given_a_user_annotation() {
        return persistAndReturn(given_a_not_persisted_user_annotation());
    }

    public UserAnnotation given_a_user_annotation(SliceInstance sliceInstance, String location, User user, Term term) throws ParseException {
        UserAnnotation annotation = given_a_not_persisted_user_annotation(sliceInstance);
        annotation.setLocation(new WKTReader().read(location));
        annotation.setUser(user);
        persistAndReturn(annotation);

        if (term!=null) {
            AnnotationTerm annotationTerm = new AnnotationTerm();
            annotationTerm.setUserAnnotation(annotation);
            annotationTerm.setUser(user);
            annotationTerm.setTerm(term);
            persistAndReturn(annotationTerm);
            em.refresh(annotation);
        }
        return annotation;
    }

    public AnnotationTerm given_a_not_persisted_annotation_term(UserAnnotation annotation) {
        return given_a_not_persisted_annotation_term(annotation, this.given_a_term(annotation.getProject().getOntology()));
    }

    public AnnotationTerm given_a_not_persisted_annotation_term(UserAnnotation annotation, Term term) {
        AnnotationTerm annotationTerm = new AnnotationTerm();
        annotationTerm.setTerm(term);
        annotationTerm.setUser(this.given_superadmin());
        annotationTerm.setUserAnnotation(annotation);
        return annotationTerm;
    }

    public AnnotationTerm given_an_annotation_term(UserAnnotation annotation, Term term) {
        return persistAndReturn(given_a_not_persisted_annotation_term(annotation, term));
    }

    public AnnotationTerm given_an_annotation_term(UserAnnotation annotation) {
        return persistAndReturn(given_a_not_persisted_annotation_term(annotation, this.given_a_term(annotation.getProject().getOntology())));
    }

    public AnnotationTerm given_an_annotation_term() {
        UserAnnotation annotation = given_a_user_annotation();
        return persistAndReturn(given_a_not_persisted_annotation_term(annotation, this.given_a_term(annotation.getProject().getOntology())));
    }

    public ReviewedAnnotation given_a_not_persisted_reviewed_annotation() {
        ReviewedAnnotation annotation = new ReviewedAnnotation();
        annotation.setUser(given_superadmin());
        try {
            annotation.setLocation(new WKTReader().read("POLYGON ((1983 2168, 2107 2160, 2047 2074, 1983 2168))"));
        } catch (ParseException ignored) {

        }
        UserAnnotation userAnnotation = given_a_user_annotation();
        annotation.setSlice(userAnnotation.getSlice());
        annotation.setImage(userAnnotation.getImage());
        annotation.setProject(userAnnotation.getImage().getProject());
        annotation.putParentAnnotation(userAnnotation);
        annotation.setReviewUser(given_superadmin());
        annotation.setStatus(0);
        return annotation;
    }

    public ReviewedAnnotation given_a_not_persisted_reviewed_annotation(Project project) {
        ReviewedAnnotation annotation = given_a_not_persisted_reviewed_annotation();
        UserAnnotation userAnnotation = given_a_not_persisted_user_annotation(project);
        userAnnotation = persistAndReturn(userAnnotation);
        annotation.putParentAnnotation(userAnnotation);
        annotation.getSlice().setProject(project);
        annotation.getImage().setProject(project);
        annotation.setProject(project);
        annotation.setReviewUser(given_superadmin());
        annotation.setStatus(0);
        return annotation;
    }


    public ReviewedAnnotation given_a_reviewed_annotation() {
        return persistAndReturn(given_a_not_persisted_reviewed_annotation());
    }

    public ReviewedAnnotation given_a_reviewed_annotation(Project project) throws ParseException {
        SliceInstance sliceInstance = given_a_slice_instance(given_an_image_instance(project), given_an_abstract_slice());
        return given_a_reviewed_annotation(
                sliceInstance,
                "POLYGON ((1983 2168, 2107 2160, 2047 2074, 1983 2168))",
                given_superadmin(),
                given_a_term(project.getOntology()));
    }

    public ReviewedAnnotation given_a_reviewed_annotation(SliceInstance sliceInstance, String location, User user, Term term) throws ParseException {
        UserAnnotation userAnnotation =
                given_a_user_annotation(sliceInstance, location, user, term);

        ReviewedAnnotation annotation = new ReviewedAnnotation();
        annotation.setImage(sliceInstance.getImage());
        annotation.setSlice(sliceInstance);
        annotation.setLocation(new WKTReader().read(location));
        annotation.setUser(user);
        annotation.setProject(sliceInstance.getProject());
        annotation.putParentAnnotation(userAnnotation);
        annotation.setReviewUser(given_superadmin());
        annotation.setStatus(0);
        persistAndReturn(annotation);

        if (term!=null) {
            AnnotationTerm annotationTerm = new AnnotationTerm();
            annotationTerm.setUserAnnotation(userAnnotation);
            annotationTerm.setUser(user);
            annotationTerm.setTerm(term);
            persistAndReturn(annotationTerm);
            annotation.getTerms().add(term);
            persistAndReturn(annotation);
        }
        persistAndReturn(annotation);
        em.refresh(userAnnotation);
        em.refresh(annotation);
        return annotation;
    }


    public AlgoAnnotation given_a_not_persisted_algo_annotation() {
        AlgoAnnotation annotation = new AlgoAnnotation();
        annotation.setUser(given_superadmin_job());
        try {
            annotation.setLocation(new WKTReader().read("POLYGON ((1983 2168, 2107 2160, 2047 2074, 1983 2168))"));
        } catch (ParseException ignored) {

        }
        annotation.setSlice(given_a_slice_instance());
        annotation.setImage(annotation.getSlice().getImage());
        annotation.setProject(annotation.getImage().getProject());
        return annotation;
    }

    public AlgoAnnotation given_a_algo_annotation(Project project) {
        return persistAndReturn(given_a_not_persisted_algo_annotation(project));
    }

    public AlgoAnnotation given_a_not_persisted_algo_annotation(Project project) {
        AlgoAnnotation annotation = given_a_not_persisted_algo_annotation();
        annotation.getSlice().setProject(project);
        annotation.getImage().setProject(project);
        annotation.setProject(project);
        return annotation;
    }


    public AlgoAnnotation given_a_algo_annotation() {
        return persistAndReturn(given_a_not_persisted_algo_annotation());
    }

    public AlgoAnnotation given_a_algo_annotation(SliceInstance sliceInstance, String location, UserJob user, Term term) throws ParseException {
        AlgoAnnotation annotation = given_a_algo_annotation();
        annotation.setImage(sliceInstance.getImage());
        annotation.setSlice(sliceInstance);
        annotation.setLocation(new WKTReader().read(location));
        annotation.setUser(user);
        annotation.setProject(sliceInstance.getProject());
        persistAndReturn(annotation);

        if (term!=null) {
            AlgoAnnotationTerm annotationTerm = new AlgoAnnotationTerm();
            annotationTerm.setAnnotation(annotation);
            annotationTerm.setUserJob(user);
            annotationTerm.setTerm(term);
            annotationTerm.setRate(0d);
            persistAndReturn(annotationTerm);
            em.refresh(annotation);
        }

        return annotation;
    }

    public AlgoAnnotationTerm given_an_algo_annotation_term() {
        AlgoAnnotation algoAnnotation = given_a_algo_annotation();
        AlgoAnnotationTerm annotationTerm = new AlgoAnnotationTerm();
        annotationTerm.setAnnotation(algoAnnotation);
        annotationTerm.setUserJob(algoAnnotation.getUser());
        annotationTerm.setTerm(given_a_term(algoAnnotation.getProject().getOntology()));
        annotationTerm.setRate(0d);
        persistAndReturn(annotationTerm);
        em.refresh(algoAnnotation);
        return annotationTerm;
    }

    public AlgoAnnotationTerm given_a_not_persisted_algo_annotation_term(AlgoAnnotation algoAnnotation) {
        AlgoAnnotationTerm annotationTerm = new AlgoAnnotationTerm();
        annotationTerm.setAnnotation(algoAnnotation);
        annotationTerm.setUserJob(algoAnnotation.getUser());
        annotationTerm.setTerm(given_a_term(algoAnnotation.getProject().getOntology()));
        annotationTerm.setRate(0d);
        return annotationTerm;
    }

    public SharedAnnotation given_a_not_persisted_shared_annotation() {
        SharedAnnotation sharedAnnotation = new SharedAnnotation();
        sharedAnnotation.setAnnotation(given_a_user_annotation());
        sharedAnnotation.setComment("Rech. proj. pr proj. priv. Self Dem. Brt. Poss. S'adr. à l'hô. Mart");
        sharedAnnotation.setSender(given_superadmin());
        sharedAnnotation.setReceivers(List.of(given_superadmin()));
        return sharedAnnotation;
    }

    public SharedAnnotation given_a_shared_annotation() {
        return persistAndReturn(given_a_not_persisted_shared_annotation());
    }
    public SharedAnnotation given_a_shared_annotation(AnnotationDomain annotationDomain) {
        SharedAnnotation sharedAnnotation = given_a_not_persisted_shared_annotation();
        sharedAnnotation.setAnnotation(annotationDomain);
        return persistAndReturn(sharedAnnotation);
    }

    public Track given_a_track() {
        return persistAndReturn(given_a_not_persisted_track());
    }

    public Track given_a_not_persisted_track() {
        Track track = new Track();
        track.setName(randomString());
        track.setColor(randomString());
        track.setImage(given_an_image_instance());
        track.setProject(track.getImage().getProject());
        return track;
    }

    public AnnotationTrack given_a_annotation_track() {
        return persistAndReturn(given_a_not_persisted_annotation_track());
    }

    public AnnotationTrack given_a_not_persisted_annotation_track() {
        AnnotationTrack annotationTrack = new AnnotationTrack();
        UserAnnotation annotation = given_a_user_annotation();
        annotationTrack.setAnnotation(annotation);
        annotationTrack.setSlice(annotation.getSlice());
        annotationTrack.setTrack(given_a_track());
        return annotationTrack;
    }

    public AttachedFile given_a_attached_file(CytomineDomain domain) {
        return persistAndReturn(given_a_not_persisted_attached_file(domain));
    }

    public AttachedFile given_a_not_persisted_attached_file(CytomineDomain domain) {
        AttachedFile attachedFile = new AttachedFile();
        attachedFile.setData(UUID.randomUUID().toString().getBytes());
        attachedFile.setFilename("test.txt");
        attachedFile.setDomain(domain);
        return attachedFile;
    }

    public AnnotationIndex given_a_not_persisted_annotation_index() {
        AnnotationIndex annotationIndex = new AnnotationIndex();
        annotationIndex.setSlice(given_a_slice_instance());
        annotationIndex.setCountAnnotation(1L);
        annotationIndex.setCountReviewedAnnotation(1L);
        annotationIndex.setUser(given_superadmin());
        return annotationIndex;
    }

    public AnnotationIndex given_a_annotation_index() {
        return persistAndReturn(given_a_not_persisted_annotation_index());
    }

    public Configuration given_a_configuration(String key) {
        return persistAndReturn(given_a_not_persisted_configuration(key));
    }

    public Configuration given_a_not_persisted_configuration(String key) {
        Configuration configuration = new Configuration();
        configuration.setKey(key);
        configuration.setValue("value");
        configuration.setReadingRole(ConfigurationReadingRole.ALL);
        return configuration;
    }

    public Description given_a_description(CytomineDomain domain) {
        return persistAndReturn(given_a_not_persisted_description(domain));
    }

    public Description given_a_not_persisted_description(CytomineDomain domain) {
        Description description = new Description();
        description.setDomain(domain);
        description.setData("hello!");
        return description;
    }

    public Tag given_a_tag(String name) {
        return persistAndReturn(given_a_not_persisted_tag(name));
    }


    public Tag given_a_tag() {
        return persistAndReturn(given_a_not_persisted_tag(randomString()));
    }

    public Tag given_a_not_persisted_tag(String name) {
        Tag tag = new Tag();
        tag.setName(name);
        tag.setUser(given_superadmin());
        return tag;
    }

    public TagDomainAssociation given_a_tag_association(Tag tag, CytomineDomain domain) {
        return persistAndReturn(given_a_not_persisted_tag_association(tag, domain));
    }

    public TagDomainAssociation given_a_not_persisted_tag_association(Tag tag, CytomineDomain domain) {
        TagDomainAssociation tagDomainAssociation = new TagDomainAssociation();
        tagDomainAssociation.setTag(tag);
        tagDomainAssociation.setDomain(domain);
        return tagDomainAssociation;
    }

    public ProjectRepresentativeUser given_a_project_representative_user() {
        return persistAndReturn(given_a_not_persisted_project_representative_user(given_a_project(), given_superadmin()));
    }

    public ProjectRepresentativeUser given_a_project_representative_user(Project project, User user) {
        return persistAndReturn(given_a_not_persisted_project_representative_user(project, user));
    }

    public ProjectRepresentativeUser given_a_not_persisted_project_representative_user() {
        return given_a_not_persisted_project_representative_user(given_a_project(), given_superadmin());
    }

    public ProjectRepresentativeUser given_a_not_persisted_project_representative_user(Project project, User user) {
        addUserToProject(project, user.getUsername());
        ProjectRepresentativeUser projectRepresentativeUser = new ProjectRepresentativeUser();
        projectRepresentativeUser.setUser(user);
        projectRepresentativeUser.setProject(project);
        return projectRepresentativeUser;
    }


    public ProjectDefaultLayer given_a_project_default_layer() {
        return persistAndReturn(given_a_not_persisted_project_default_layer(given_a_project(), given_superadmin()));
    }

    public ProjectDefaultLayer given_a_project_default_layer(Project project, User user) {
        return persistAndReturn(given_a_not_persisted_project_default_layer(project, user));
    }

    public ProjectDefaultLayer given_a_not_persisted_project_default_layer() {
        return given_a_not_persisted_project_default_layer(given_a_project(), given_superadmin());
    }

    public ProjectDefaultLayer given_a_not_persisted_project_default_layer(Project project, User user) {
        addUserToProject(project, user.getUsername());
        ProjectDefaultLayer projectDefaultLayer = new ProjectDefaultLayer();
        projectDefaultLayer.setUser(user);
        projectDefaultLayer.setProject(project);
        projectDefaultLayer.setHideByDefault(false);
        return projectDefaultLayer;
    }

    public SecUserSecRole given_a_user_role() {
        return persistAndReturn(given_a_not_persisted_user_role( given_a_guest(), secRoleRepository.findByAuthority(ROLE_USER).get()));
    }

    public SecUserSecRole given_a_user_role(User user) {
        return persistAndReturn(given_a_not_persisted_user_role(user, secRoleRepository.findByAuthority(ROLE_USER).get()));
    }

    public SecUserSecRole given_a_user_role(User user, SecRole secRole) {
        return persistAndReturn(given_a_not_persisted_user_role(user, secRole));
    }

    public SecUserSecRole given_a_not_persisted_user_role(User user, String authority) {
        return given_a_not_persisted_user_role(user, secRoleRepository.findByAuthority(authority).get());
    }

    public SecUserSecRole given_a_not_persisted_user_role(SecUser secUser, SecRole secRole) {
        SecUserSecRole secUserSecRole = new SecUserSecRole();
        secUserSecRole.setSecRole(secRole);
        secUserSecRole.setSecUser(secUser);
        return secUserSecRole;
    }


}
