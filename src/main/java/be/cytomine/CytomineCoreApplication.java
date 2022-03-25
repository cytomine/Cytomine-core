package be.cytomine;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.io.File;
import java.util.Date;

@Slf4j
@SpringBootApplication
@EnableMongoRepositories("be.cytomine.repositorynosql")
@EnableJpaRepositories("be.cytomine.repository")
public class CytomineCoreApplication {

	public static void main(String[] args) {
		log.info("CytomineCoreApplication.main");
		log.info("Current directory: " + new File(".").getAbsolutePath());
		for (File file : new File("./").listFiles()) {
			log.info("sub directories/files: " + file.getAbsolutePath());
		}

		log.info("application.yml found? " + new File("./application.yml").exists());

		SpringApplication.run(CytomineCoreApplication.class, args);
	}

}
