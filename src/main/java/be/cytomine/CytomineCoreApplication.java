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

import be.cytomine.utils.EndOfLifeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.io.File;

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

		EndOfLifeUtils.blockApplicationIfEndOfLifeHasBeenReached();

		SpringApplication.run(CytomineCoreApplication.class, args);
	}

}
