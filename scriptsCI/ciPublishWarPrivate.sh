#!/bin/bash

set -o xtrace
set -o errexit

echo "************************************** Build war ******************************************"

file='./ci/version'
VERSION_NUMBER=$(<"$file")

echo "Build for $VERSION_NUMBER"
ssh -p 50004 cytomine@185.35.173.82 "mkdir -p /data/core/$CUSTOMER/$VERSION_NUMBER"
scp -P 50004 ./ci/cytomine.war cytomine@185.35.173.82:/data/core/$CUSTOMER/$VERSION_NUMBER/Core.war

CORE_PATH="/data/core/$CUSTOMER/$VERSION_NUMBER/Core.war"
echo $CORE_PATH
echo $CORE_PATH > ./ci/path