#!/bin/bash

set -o xtrace
set -o errexit

echo "************************************** Build war ******************************************"

file='./ci/version'
VERSION_NUMBER=$(<"$file")

echo "Build for $VERSION_NUMBER"
ssh -p 50004 cytomine@185.35.173.82 "mkdir -p /data/core/$CUSTOMER/$VERSION_NUMBER"
scp -P 50004 ./ci/Core.jar cytomine@185.35.173.82:/data/core/$CUSTOMER/$VERSION_NUMBER/Core.jar

CORE_PATH="/data/core/$CUSTOMER/$VERSION_NUMBER/Core.jar"
echo $CORE_PATH
echo $CORE_PATH > ./ci/path