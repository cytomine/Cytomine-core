package be.cytomine;

import be.cytomine.config.ApplicationConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@SpringBootApplication
//@EnableConfigurationProperties({LiquibaseProperties.class, ApplicationConfiguration.class})
public class TestApplication {

    @Autowired
    private ApplicationBootstrap applicationBootstrap;

//    public static void main(String[] args) throws Exception {
//        TestApplication testApplication = new TestApplication();
//        testApplication.start();
//    }
//
//    public void start() throws Exception {
//        log.info("Starting cytomine core TEST");
//        applicationBootstrap.init();
//    }

    @Bean
    public InitializingBean init() {
            // TODO: do not run twice in dev
        return () -> {
            log.info("Running cytomine core TEST");
            //applicationBootstrap.init();
        };
    }
}
