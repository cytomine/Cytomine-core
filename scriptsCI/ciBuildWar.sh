#!/bin/bash

set -o xtrace
set -o errexit

echo "************************************** Build war ******************************************"

file='./ci/version'
VERSION_NUMBER=$(<"$file")

echo "Build for $VERSION_NUMBER"

docker build --rm -f scriptsCI/docker/Dockerfile-war.build --build-arg VERSION_NUMBER=$VERSION_NUMBER -t  cytomine/cytomine-core-war .

containerId=$(docker create cytomine/cytomine-core-war )
#docker network connect scripts_default $containerId
docker start -ai  $containerId
docker cp $containerId:/app/target/cytomine.war ./ci

docker rm $containerId
docker rmi cytomine/cytomine-core-war