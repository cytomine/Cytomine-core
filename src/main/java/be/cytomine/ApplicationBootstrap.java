package be.cytomine;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.List;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import be.cytomine.config.properties.ApplicationProperties;
import be.cytomine.config.nosqlmigration.InitialMongodbSetupMigration;
import be.cytomine.domain.security.SecUser;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.repository.security.SecUserRepository;
import be.cytomine.service.UrlApi;
import be.cytomine.service.database.BootstrapDataService;
import be.cytomine.service.database.BootstrapTestsDataService;
import be.cytomine.service.database.BootstrapUtilsService;
import be.cytomine.service.utils.Dataset;
import be.cytomine.utils.EnvironmentUtils;

import static be.cytomine.service.database.BootstrapTestsDataService.*;

@Component
@Order(0)
@Slf4j
@RequiredArgsConstructor
@EnableConfigurationProperties({LiquibaseProperties.class, ApplicationProperties.class})
@Transactional
class ApplicationBootstrap {

    private final Environment environment;

    private final ApplicationProperties applicationProperties;

    private final InitialMongodbSetupMigration initialSetupMigration;

    private final Dataset dataset;

    private final SecUserRepository secUserRepository;

    private final BootstrapDataService bootstrapDataService;

    private final BootstrapUtilsService bootstrapUtilDataService;

    private final BootstrapTestsDataService bootstrapTestsDataService;

    @PostConstruct
    public void init() {

        initialSetupMigration.changeSet();

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
        log.info (applicationProperties.toString());
        log.info ("#############################################################################");
        log.info ("#############################################################################");
        log.info ("#############################################################################");

        UrlApi.setServerURL(applicationProperties.getServerURL());

        if (EnvironmentUtils.isTest(environment) && secUserRepository.count() == 0) {
            bootstrapDataService.initData();
            bootstrapUtilDataService.createUser(dataset.ANOTHERLOGIN, "Just another", "User", dataset.ADMINEMAIL, dataset.ADMINPASSWORD, List.of("ROLE_USER", "ROLE_ADMIN","ROLE_SUPER_ADMIN"));

            // same as superadmin, but a userjob
            bootstrapUtilDataService.createUserJob("superadminjob", dataset.ADMINPASSWORD,
                    (User)secUserRepository.findByUsernameLikeIgnoreCase("superadmin").orElseThrow(() -> new ObjectNotFoundException("User", "superadmin")),
                    List.of("ROLE_USER", "ROLE_ADMIN","ROLE_SUPER_ADMIN"));

            // We need these users for all authorization tests
            // So we create them at the beginning in order to avoid creating them before each authorization tests
            bootstrapTestsDataService.createUserForTests(SUPERADMIN);
            bootstrapTestsDataService.createUserForTests(USER_ACL_READ);
            bootstrapTestsDataService.createUserForTests(USER_ACL_WRITE);
            bootstrapTestsDataService.createUserForTests(USER_ACL_CREATE);
            bootstrapTestsDataService.createUserForTests(USER_ACL_DELETE);
            bootstrapTestsDataService.createUserForTests(USER_ACL_ADMIN);
            bootstrapTestsDataService.createUserForTests(USER_NO_ACL);
            bootstrapTestsDataService.createUserForTests(GUEST);
            bootstrapTestsDataService.createUserForTests(CREATOR);
        } else if (secUserRepository.count() == 0) {
            //if database is empty, put minimal data
            bootstrapDataService.initData();
        }

        if (applicationProperties.getImageServerPrivateKey()!=null && applicationProperties.getImageServerPublicKey()!=null) {
            SecUser imageServerUser = secUserRepository.findByUsernameLikeIgnoreCase("ImageServer1")
                    .orElseThrow(() -> new ObjectNotFoundException("No user imageserver1, cannot assign keys"));
            imageServerUser.setPrivateKey(applicationProperties.getImageServerPrivateKey());
            imageServerUser.setPublicKey(applicationProperties.getImageServerPublicKey());
            secUserRepository.save(imageServerUser);
        }

        log.info("Check image filters...");
        bootstrapDataService.updateImageFilters();
        log.info ("#############################################################################");
        log.info ("###################              READY              #########################");
        log.info ("#############################################################################");
    }
}
