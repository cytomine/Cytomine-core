#!/bin/bash

set -o xtrace
set -o errexit

echo "************************************** Publish docker ******************************************"

file='./ci/version'
VERSION_NUMBER=$(<"$file")

docker build --rm -f scripts/docker/core/Dockerfile -t  cytomine/core:$VERSION_NUMBER .

docker push cytomine/core:$VERSION_NUMBER

docker rmi cytomine/core:$VERSION_NUMBER