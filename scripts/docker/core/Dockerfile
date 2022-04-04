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

# JAVA_OPTS="-Djava.awt.headless=true -Xms512m -Xmx4096m -XX:MaxPermSize=512M -XX:+UseConcMarkSweepGC -Djavax.servlet.request.encoding=UTF-8 -Dfile.encoding=UTF-8"

FROM openjdk:17.0-jdk-slim

# base librairies and configuration
RUN apt-get update -y && apt-get -y install \
      build-essential \
      locate \
      logrotate \
      net-tools \
      unzip \
      wget

RUN sed -i "/su root syslog/c\su root root" /etc/logrotate.conf
ENV LANG C.UTF-8
ENV DEBIAN_FRONTEND noninteractive


#tomcat configuration

RUN apt-get -y update && apt-get install -y autoconf automake libpopt-dev libtool make xz-utils
RUN cd /tmp/ && wget https://github.com/logrotate/logrotate/releases/download/3.18.0/logrotate-3.18.0.tar.xz && tar -xJf logrotate-3.18.0.tar.xz
RUN cd /tmp/logrotate-3.18.0 && autoreconf -fiv && ./configure && make

RUN cp /tmp/logrotate-3.18.0/logrotate /usr/sbin/logrotate

ADD scripts/docker/core/deploy.sh /app/deploy.sh
RUN chmod +x /app/deploy.sh
ADD ci/cytomine.jar /app/cytomine.jar
RUN touch /tmp/addHosts.sh && chmod +x /tmp/addHosts.sh
RUN ls -ln /app/cytomine.jar
WORKDIR /app
ENTRYPOINT "/app/deploy.sh"
#ENTRYPOINT ["java -Djava.awt.headless=true -Xms512m -Xmx4096m -XX:MaxPermSize=512M -XX:+UseConcMarkSweepGC -Djavax.servlet.request.encoding=UTF-8 -Dfile.encoding=UTF-8 -cp /app/cytomine.jar -Dloader.main=be.cytomine.CytomineCoreApplication -Djava.awt.headless=true org.springframework.boot.loader.PropertiesLauncher"]
