# Copyright (c) 2009-2022. Authors: see NOTICE file.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
#
# Input environment variables:
# - CORE_VERSION: [REQUIRED] an cytomine specific image version to append to rabbitmq version
# - SCRIPTS_REPO_URL: the complete url of the docker scripts repository including credentials (if necessary)
# - SCRIPTS_REPO_TAG: version for extracting initialization scripts
# - DOCKER_NAMESPACE: the namespace of the cytomine rabbitmq image built by this repository

set -e

if [ -f .env ]
then
  export $(cat .env | xargs)
fi

CORE_VERSION=${CORE_VERSION}
SCRIPTS_REPO_TAG=${SCRIPTS_REPO_TAG:-"latest"}  # TODO get latest tag automatically
DOCKER_NAMESPACE=${DOCKER_NAMESPACE:-"cytomine"}

ME=$(basename $0)

if [ -z "$CORE_VERSION" ]; then
  echo >&2 "$ME: ERROR: CORE_VERSION not provided"
  exit 1
fi

docker build \
  --build-arg CORE_VERSION=$CORE_VERSION \
  --build-arg SCRIPTS_REPO_TAG=$SCRIPTS_REPO_TAG \
  --secret id=scripts_repo_url,env=SCRIPTS_REPO_URL \
  -t "$DOCKER_NAMESPACE/core:$CORE_VERSION" -f docker/Dockerfile . $@

