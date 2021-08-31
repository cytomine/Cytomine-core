#!/bin/bash

set -o xtrace
set -o errexit

echo "************************************** Build war ******************************************"

file='./ci/version'
VERSION_NUMBER=$(<"$file")

echo "Build for $VERSION_NUMBER"
ssh cytomine@192.168.122.16 "mkdir -p /home/cytomine/public_html/drupal7/dwnld/dev/releases/core/$VERSION_NUMBER"
scp ./ci/cytomine.war cytomine@192.168.122.16:/home/cytomine/public_html/drupal7/dwnld/dev/releases/core/$VERSION_NUMBER/Core.war

CORE_URL="https://cytomine.com/dwnld/dev/releases/core/$VERSION_NUMBER/Core.war"
echo $CORE_URL
echo $CORE_URL > ./ci/url