#!/bin/bash

set -o xtrace

echo "************************************** Launch tests ******************************************"

file='./ci/version'
VERSION_NUMBER=$(<"$file")

echo "Launch tests for $VERSION_NUMBER"

docker build --rm -f scripts/docker/Dockerfile-test.build --build-arg VERSION_NUMBER=$VERSION_NUMBER -t  cytomine/cytomine-core-spring-test .
mkdir -p ./ci/reports/test

containerId=$(docker create --network scripts_default --link nginxTest:localhost-core --link postgresqltest:postgresqltest --link mongodbtest:mongodbtest --link rabbitmqtest:rabbitmqtest  cytomine/cytomine-core-spring-test )
#docker network connect scripts_default $containerId
docker start -ai  $containerId

if [ $? -eq 0 ]
then
  echo "Success: no tests fails"
  final=0
else
  echo "Failure: Some tests fails" >&2
  final=1
fi

docker cp $containerId:/app/build "$PWD"/ci
docker #cp $containerId:/app/build/test-results "$PWD"/ci/reports
docker #cp $containerId:/app/build/jacoco "$PWD"/ci/reports
docker rm $containerId

cp -r "$PWD"/ci/build/test-results $WORKSPACE/test-results
cp -r "$PWD"/ci/build/jacoco $WORKSPACE/jacoco
exit $final