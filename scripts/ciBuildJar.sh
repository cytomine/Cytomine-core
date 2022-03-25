#!/bin/bash

set -o xtrace
set -o errexit

echo "************************************** Build jar ******************************************"

file='./ci/version'
VERSION_NUMBER=$(<"$file")

echo "Build for $VERSION_NUMBER"

docker build --rm -f scripts/docker/Dockerfile-jar.build --build-arg VERSION_NUMBER=$VERSION_NUMBER -t  cytomine/cytomine-core-jar .

containerId=$(docker create cytomine/cytomine-core-jar )
#docker network connect scripts_default $containerId
docker start -ai  $containerId
docker cp $containerId:/app/build/libs/cytomine.jar ./ci

docker rm $containerId
docker rmi cytomine/cytomine-core-jar