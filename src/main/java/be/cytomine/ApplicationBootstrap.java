package be.cytomine;

import be.cytomine.config.ApplicationConfiguration;
import be.cytomine.config.nosqlmigration.InitialMongodbSetupMigration;
import be.cytomine.domain.ontology.Ontology;
import be.cytomine.domain.ontology.Term;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.SecUser;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.repository.image.server.StorageRepository;
import be.cytomine.repository.ontology.OntologyRepository;
import be.cytomine.repository.project.ProjectRepository;
import be.cytomine.repository.security.AclRepository;
import be.cytomine.repository.security.SecUserRepository;
import be.cytomine.service.PermissionService;
import be.cytomine.service.UrlApi;
import be.cytomine.service.database.BootstrapDataService;
import be.cytomine.service.database.BootstrapUtilsService;
import be.cytomine.service.database.SequenceService;
import be.cytomine.service.project.ProjectService;
import be.cytomine.service.utils.Dataset;
import be.cytomine.utils.EnvironmentUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.List;

import static org.springframework.security.acls.domain.BasePermission.ADMINISTRATION;

@Component
@Order(0)
@Slf4j
@RequiredArgsConstructor
@EnableConfigurationProperties({LiquibaseProperties.class,ApplicationConfiguration.class})
@Transactional
class ApplicationBootstrap implements ApplicationListener<ApplicationReadyEvent> {

    private final SecUserRepository secUserRepository;

    private final AclRepository aclRepository;

    private final StorageRepository storageRepository;

    private final OntologyRepository ontologyRepository;

    private final ProjectService projectService;

    private final EntityManager entityManager;

    private final SequenceService sequenceService;


    private final ApplicationConfiguration applicationConfiguration;

    private final Environment environment;


    @Autowired
    BootstrapDataService bootstrapDataService;

    @Autowired
    BootstrapUtilsService bootstrapUtilDataService;

    @Autowired
    ProjectRepository projectRepository;

    @Autowired
    PermissionService permissionService;

    @Autowired
    InitialMongodbSetupMigration initialSetupMigration;

    @Autowired
    Dataset dataset;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("ApplicationListener#onApplicationEvent()");
        init();
    }

    public void init() {


        initialSetupMigration.changeSet();


        SecUser secUser = secUserRepository.findByUsernameLikeIgnoreCase("admin").orElse(null);
        if (secUser!=null) {
            secUser.setPassword(passwordEncoder.encode("password"));
            secUserRepository.save(secUser);

            if (ontologyRepository.count()==0) {
                Ontology ontology = new Ontology();
                ontology.setName("ONTOLOGY_DEMO1");
                ontology.setUser((User)secUser);
                ontology = ontologyRepository.save(ontology);

                Term term1 = new Term();
                term1.setName("term1");
                term1.setColor("#FF0000");
                term1.setOntology(ontology);
                entityManager.persist(term1);
                Term term2 = new Term();
                term2.setName("term2");
                term2.setColor("#00FF00");
                term2.setOntology(ontology);
                entityManager.persist(term2);

                Project project = new Project();
                project.setName("PROJECT_DEMO1");
                project.setOntology(ontology);

                project = projectRepository.save(project);

                permissionService.addPermission(project, secUser.getUsername(), ADMINISTRATION, secUser);
                permissionService.addPermission(ontology, secUser.getUsername(), ADMINISTRATION, secUser);
            }



        }




        // TODO: print config
        log.info ("#############################################################################");
        log.info ("#############################################################################");
        log.info ("#############################################################################");
        String cytomineWelcomMessage = "                   _____      _                  _\n" +
                "                  / ____|    | |                (_)\n" +
                "                 | |    _   _| |_ ___  _ __ ___  _ _ __   ___\n" +
                "                 | |   | | | | __/ _ \\| '_ ` _ \\| | '_ \\ / _ \\\n" +
                "                 | |___| |_| | || (_) | | | | | | | | | |  __/\n" +
                "                  \\_____\\__, |\\__\\___/|_| |_| |_|_|_| |_|\\___|\n" +
                "                 |  _ \\  __/ |     | |     | |\n" +
                "                 | |_) ||___/  ___ | |_ ___| |_ _ __ __ _ _ __\n" +
                "                 |  _ < / _ \\ / _ \\| __/ __| __| '__/ _` | '_ \\\n" +
                "                 | |_) | (_) | (_) | |_\\__ \\ |_| | | (_| | |_) |\n" +
                "                 |____/ \\___/ \\___/ \\__|___/\\__|_|  \\__,_| .__/\n" +
                "                                                         | |\n" +
                "                                                         |_|";
        log.info (cytomineWelcomMessage);
        log.info ("#############################################################################");
        log.info ("#############################################################################");
        log.info ("#############################################################################");
        log.info ("Environment:" + Arrays.toString(environment.getActiveProfiles()));
        log.info ("Current directory:" + new File( "./" ).getAbsolutePath());
        log.info ("HeadLess:" + java.awt.GraphicsEnvironment.isHeadless());
        log.info ("JVM Args" + ManagementFactory.getRuntimeMXBean().getInputArguments());
        log.info (applicationConfiguration.toString());
        log.info ("#############################################################################");
        log.info ("#############################################################################");
        log.info ("#############################################################################");

        UrlApi.setServerURL(
                applicationConfiguration.getServerURL(),
                applicationConfiguration.getUseHTTPInternally()
        );

        if (EnvironmentUtils.isTest(environment) && secUserRepository.count() == 0) {
            bootstrapDataService.initData();
            //noSQLCollectionService.cleanActivityDB() TODO:
            bootstrapUtilDataService.createUser(dataset.ANOTHERLOGIN, "Just another", "User", dataset.ADMINEMAIL, dataset.ADMINPASSWORD, List.of("ROLE_USER", "ROLE_ADMIN","ROLE_SUPER_ADMIN"));

            // same as superadmin, but a userjob
            bootstrapUtilDataService.createUserJob("superadminjob", dataset.ADMINPASSWORD,
                    (User)secUserRepository.findByUsernameLikeIgnoreCase("superadmin").orElseThrow(() -> new ObjectNotFoundException("User", "superadmin")),
                    List.of("ROLE_USER", "ROLE_ADMIN","ROLE_SUPER_ADMIN"));
            //mockServicesForTests() TODO in test?
        } else if (secUserRepository.count() == 0) {
            //if database is empty, put minimal data
            bootstrapDataService.initData();
        }

        if (applicationConfiguration.getImageServerPrivateKey()!=null && applicationConfiguration.getImageServerPublicKey()!=null) {
            SecUser imageServerUser = secUserRepository.findByUsernameLikeIgnoreCase("ImageServer1")
                    .orElseThrow(() -> new ObjectNotFoundException("No user imageserver1, cannot assign keys"));
            imageServerUser.setPrivateKey(applicationConfiguration.getImageServerPrivateKey());
            imageServerUser.setPublicKey(applicationConfiguration.getImageServerPublicKey());
            secUserRepository.save(imageServerUser);
        }
        if (applicationConfiguration.getRabbitMQPrivateKey()!=null && applicationConfiguration.getRabbitMQPrivateKey()!=null) {
            secUserRepository.findByUsernameLikeIgnoreCase("rabbitmq")
                    .ifPresent(user -> {
                        user.setPrivateKey(applicationConfiguration.getRabbitMQPrivateKey());
                        user.setPublicKey(applicationConfiguration.getRabbitMQPublicKey());
                            secUserRepository.save(user);
                    });
        }

        File softwareSourceDirectory = new File(applicationConfiguration.getSoftwareSources());
        if (!softwareSourceDirectory.exists() && !softwareSourceDirectory.mkdirs()) {
            log.error("Software Sources folder doesn't exist");
        }

        bootstrapUtilDataService.initRabbitMq();

        log.info("create multiple IS and Retrieval...");
        bootstrapUtilDataService.createMultipleImageServer();
        bootstrapUtilDataService.updateProcessingServerRabbitQueues();

        log.info ("#############################################################################");
        // TODO!
//        bootstrapUtilDataService.fillProjectConnections();
//        bootstrapUtilDataService.fillImageConsultations();


    }




}