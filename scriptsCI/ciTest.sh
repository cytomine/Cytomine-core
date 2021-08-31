#!/bin/bash

echo "************************************** Launch tests ******************************************"

file='./ci/version'
VERSION_NUMBER=$(<"$file")

echo "Launch tests for $VERSION_NUMBER"

docker build --rm -f scriptsCI/docker/Dockerfile-test.build --build-arg VERSION_NUMBER=$VERSION_NUMBER -t  cytomine/cytomine-core-test .
mkdir ./ci/test-reports
containerId=$(docker create --network scriptsci_default --link postgresqltest:postgresqltest --link mongodbtest:mongodbtest --link rabbitmqtest:rabbitmqtest cytomine/cytomine-core-test )
#docker network connect scripts_default $containerId
docker start -ai  $containerId
docker cp $containerId:/app/target/test-reports/ ./ci
#docker cp $containerId:/tmp/testLog-debug.log ./ci

docker rm $containerId
docker rmi cytomine/cytomine-core-test