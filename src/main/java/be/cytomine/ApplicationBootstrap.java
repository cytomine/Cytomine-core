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
import be.cytomine.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.springframework.security.acls.domain.BasePermission.ADMINISTRATION;

@Component
@Order(0)
@Slf4j
@RequiredArgsConstructor
@EnableConfigurationProperties({LiquibaseProperties.class,ApplicationConfiguration.class})
@Transactional
class ApplicationBootstrap implements ApplicationListener<ApplicationReadyEvent> {

    @Value("${spring.mail.password}")
    public String password;

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

        printConfiguration();

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
    }

    private void printConfiguration() {
        log.info("Cytomine-core: " + applicationConfiguration.getVersion());
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        log.info("*************** SYSTEM CONFIGURATION ******************");
        log.info("Max memory:" + memoryBean.getHeapMemoryUsage().getMax() / (1024 * 1024) + "MB");
        log.info("Used memory:" + memoryBean.getHeapMemoryUsage().getUsed() / (1024 * 1024) + "MB");
        log.info("Init memory:" + memoryBean.getHeapMemoryUsage().getInit() / (1024 * 1024) + "MB");
        log.info("Default charset: " + Charset.defaultCharset());
//        log.info("*************** JAVA CONFIGURATION ******************");
//        for (Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
//            log.info(entry.getKey() + "=" + entry.getValue());
//        }
        log.info("*************** APPLICATION CONFIGURATION ******************");
        log.info(applicationConfiguration.toString());
         final Environment env = applicationContext.getEnvironment();

        log.info("====== Environment and configuration ======");
        log.info("Active profiles: {}", Arrays.toString(env.getActiveProfiles()));
        MutablePropertySources sources = ((AbstractEnvironment) env).getPropertySources();
        StreamSupport.stream(sources.spliterator(), false)
                .filter(ps -> ps instanceof EnumerablePropertySource)
                .map(ps -> ((EnumerablePropertySource) ps).getPropertyNames())
                .flatMap(Arrays::stream)
                .distinct()
                .sorted()
                .forEach(prop -> log.info("ACTIVE {}: {}", prop, mustBeObscurify(prop) ? StringUtils.obscurify(env.getProperty(prop), 2) : env.getProperty(prop)));

    }

    private boolean mustBeObscurify(String prop) {
        return (prop.contains("credentials") || prop.contains("password") || prop.contains("PrivateKey") || prop.contains("secret"));

    }

//
    @Autowired
    ApplicationContext applicationContext;

//
//    @EventListener
//    public void handleContextRefreshed(ContextRefreshedEvent event) {
//        printActiveProperties((ConfigurableEnvironment) event.getApplicationContext().getEnvironment());
//    }
//
//    private void printActiveProperties(ConfigurableEnvironment env) {
//
//        System.out.println("************************* ACTIVE APP PROPERTIES ******************************");
//
//        List<MapPropertySource> propertySources = new ArrayList<>();
//
//        env.getPropertySources().forEach(it -> {
//            propertySources.add((MapPropertySource) it);
//        });
//
//
//        propertySources.stream()
//                .map(propertySource -> propertySource.getSource().keySet())
//                .flatMap(Collection::stream)
//                .distinct()
//                .sorted()
//                .forEach(key -> {
//                    try {
//                        System.out.println(key + "=" + env.getProperty(key));
//                    } catch (Exception e) {
//                        log.warn("{} -> {}", key, e.getMessage());
//                    }
//                });
//        System.out.println("******************************************************************************");
//    }

}