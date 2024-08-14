# Cytomine Core

## Install

See our [Cytomine-bootstrap](https://github.com/cytomine/Cytomine-bootstrap) project to install it with Docker.
A branch `spring-core` has the modification to install the core in its spring version (v4).

>  **WARNING**: The current beta version does not support already existing dataset created from previous version. You can only create Cytomine instance with an empty dataset. 
> This will be supported in the final v4.0.0+.

## Run tests

Automated tests require:
* A postgresql with postgis
* A mongodb
* A rabbitMQ

You can simply run the docker-compose configuration from `scripts/docker-compose.yml` in order to have all these dependencies up and running with the right configuration.

`docker-compose -f scripts/docker-compose-test.yml up -d`

Tests can be executed with Gradle:

`./gradlew :test`

Or via your favorite IDE (right click on `/src/test/java` and choose *Run Tests in cytomine-core*)

After tests, you can shutdown the configuration :

`docker-compose -f scripts/docker-compose-test.yml down -v`

## Run as dev

Execute the main class `src/main/java/be/cytomine/CytomineCoreApplication.java` 

## Build an executable

`./gradlew bootJar` produce a auto-executable jar under `build/libs/cytomine.jar`

Run it using:

`java -jar /app/cytomine.jar`

## Override configuration

Default application configuration values are located in `src/main/resources/application.yml`.
You can override them by creating a `application.yml` or `.properties` in the application root directory.

## Upgrade from old version Cytomine dataset

The upgrade only works with a dataset from at least core 3.2.0.

> We will provide soon a automated way to upgrade databases

Postgresql database:
Regarding Postgresql, nothing to be done. 
Liquibase should apply changes during the first run.

MongoDB database:
As we are moving from MongoDB v2 to v4, the current migration process needs:
* extract the current mongodb database .json / .bson files
* import them using `./scripts/migration/import_mongo.sh` into the new MongoDB 4.x database

