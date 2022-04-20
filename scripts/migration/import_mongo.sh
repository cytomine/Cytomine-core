#!/bin/bash

set -o xtrace
set -o errexit

echo "************************************** Mongo ******************************************"

docker rm --force mongomigrationscript
docker run --rm --name mongomigrationscript -v /home/lrollus/dataset/mongodb_06-04-2022_02_30/cytomine:/datasets --link scripts_mongodbmigration_1:mongodb --net scripts_default  -d mongo:4.4

docker cp ./scripts/migration/import.sh mongomigrationscript:/tmp/import.sh

docker exec -it mongomigrationscript mongorestore --version

docker exec -it mongomigrationscript /tmp/import.sh

docker rm --force mongomigrationscript
