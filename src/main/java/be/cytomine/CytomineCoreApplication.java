package be.cytomine;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Slf4j
@SpringBootApplication
@EnableMongoRepositories("be.cytomine.repositorynosql")
@EnableJpaRepositories("be.cytomine.repository")
public class CytomineCoreApplication {

	public static void main(String[] args) {
		log.info("CytomineCoreApplication.main");
		SpringApplication.run(CytomineCoreApplication.class, args);
	}

}
