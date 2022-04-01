#!/bin/bash

set -o xtrace
set -o errexit
set -a

rm -rf ./ci
mkdir ./ci

./scripts/ciBuildVersion.sh

./scripts/ciDownloadDependencies.sh

#docker-compose -f scripts/docker-compose.yml up -d

#./scripts/ciTest.sh

#docker-compose -f scripts/docker-compose.yml down

./scripts/ciBuildJar.sh

#./scripts/ciPublishJar.sh

#./scripts/ciBuildDockerImage.sh