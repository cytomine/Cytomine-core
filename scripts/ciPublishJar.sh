#!/bin/bash

set -o xtrace
set -o errexit

echo "************************************** Publish Jar ******************************************"

file='./ci/version'
VERSION_NUMBER=$(<"$file")

echo "Build for $VERSION_NUMBER"
ssh cytomine@192.168.122.16 "mkdir -p /home/cytomine/public_html/drupal7/dwnld/dev/releases/core/$VERSION_NUMBER"
scp ./ci/cytomine.jar cytomine@192.168.122.16:/home/cytomine/public_html/drupal7/dwnld/dev/releases/core/$VERSION_NUMBER/Core.jar

CORE_URL="https://cytomine.com/dwnld/dev/releases/core/$VERSION_NUMBER/Core.jar"
echo $CORE_URL
echo $CORE_URL > ./ci/url