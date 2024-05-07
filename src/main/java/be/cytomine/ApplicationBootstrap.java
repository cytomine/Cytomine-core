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

import be.cytomine.config.properties.ApplicationProperties;
import be.cytomine.config.nosqlmigration.InitialMongodbSetupMigration;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.repository.project.ProjectRepository;
import be.cytomine.repository.security.UserRepository;
import be.cytomine.service.PermissionService;
import be.cytomine.service.UrlApi;
import be.cytomine.service.database.BootstrapDataService;
import be.cytomine.service.database.BootstrapTestsDataService;
import be.cytomine.service.database.BootstrapUtilsService;
import be.cytomine.service.utils.Dataset;
import be.cytomine.utils.EnvironmentUtils;
import be.cytomine.utils.StringUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.StreamSupport;

import static be.cytomine.service.database.BootstrapTestsDataService.*;

@Component
@Order(0)
@Slf4j
@RequiredArgsConstructor
@EnableConfigurationProperties({LiquibaseProperties.class, ApplicationProperties.class})
@Transactional
class ApplicationBootstrap {

    private final UserRepository userRepository;

    private final ApplicationProperties applicationProperties;

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

    @Autowired
    BootstrapTestsDataService bootstrapTestsDataService;

    @PostConstruct
    public void init() {

        printConfiguration();

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

        UrlApi.setServerURL(
                applicationProperties.getServerURL()
        );

        if (EnvironmentUtils.isTest(environment) && userRepository.count() == 0) {
            bootstrapDataService.initData();
            //noSQLCollectionService.cleanActivityDB() TODO:
            bootstrapUtilDataService.createUser(dataset.ANOTHERLOGIN, "Just another", "User", List.of("ROLE_USER", "ROLE_ADMIN","ROLE_SUPER_ADMIN"));

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


        } else if (userRepository.count() == 0) {
            //if database is empty, put minimal data
            bootstrapDataService.initData();
        }

        // TODO IAM: done by IAM
        if (applicationProperties.getImageServerPrivateKey()!=null && applicationProperties.getImageServerPublicKey()!=null) {
            User imageServerUser = userRepository.findByUsernameLikeIgnoreCase("ImageServer1")
                    .orElseThrow(() -> new ObjectNotFoundException("No user imageserver1, cannot assign keys"));
            imageServerUser.setPrivateKey(applicationProperties.getImageServerPrivateKey());
            imageServerUser.setPublicKey(applicationProperties.getImageServerPublicKey());
            userRepository.save(imageServerUser);
        }

        log.info("Check image filters...");
        bootstrapDataService.updateImageFilters();
        log.info ("#############################################################################");
        log.info ("###################              READY              #########################");
        log.info ("#############################################################################");

    }

    private void printConfiguration() {
        log.info("Cytomine-core: " + applicationProperties.getVersion());
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
        log.info(applicationProperties.toString());
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
        return (prop.toLowerCase().contains("credentials") || prop.toLowerCase().contains("password") || prop.toLowerCase().contains("privatekey") || prop.toLowerCase().contains("secret"));
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
