#!/bin/bash

set -o xtrace
set -o errexit
set -a

rm -rf ./ci
mkdir ./ci

./scripts/ciBuildVersion.sh

./scripts/ciDownloadDependencies.sh

./scripts/ciTest.sh

./scripts/ciBuildJar.sh

./scripts/ciPublishJar.sh

./scripts/ciBuildDockerImage.sh