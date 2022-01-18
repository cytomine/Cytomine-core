#!/bin/bash

#
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

CORE_URL=$(cat /usr/share/tomcat9/.grails/cytomineconfig.groovy | grep grails.serverURL)
CORE_URL=(${CORE_URL//=/ })
CORE_URL=${CORE_URL[1]}
CORE_URL=$(echo $CORE_URL | sed "s/'/\"/g")

cd /var/lib/tomcat9/  && sed -i "/basePath/c\   \"basePath\": $CORE_URL," restapidoc.json

bash /tmp/addHosts.sh

mv /tmp/setenv.sh $CATALINA_HOME/bin/

bash /usr/local/tomcat/bin/catalina.sh run
