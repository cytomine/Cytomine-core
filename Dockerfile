ARG CORE_VERSION
ARG SCRIPTS_REPO_TAG
ARG SCRIPTS_REPO_BRANCH


#######################################################################################
# Stage 1: core dependencies download via gradle
FROM gradle:7.4.2-jdk17-alpine AS deps-downloader

# We first copy the build.gradle file and the binaries stored in the source repository.
# This way, we retrieve all gradle dependencies at the beginning. All these steps will be
# cached by Docker unless build.gradle. This means that we only retrieve all dependencies 
# if we modify the dependencies definition.
RUN mkdir -p /opt/gradle/.gradle
ENV GRADLE_USER_HOME=/opt/gradle/.gradle

WORKDIR /app
COPY ./build.gradle /app/build.gradle

RUN gradle clean build --no-daemon --console=verbose

#######################################################################################
## Stage 2: building the core jar file
FROM gradle:7.4.2-jdk17-alpine AS jar-builder

ENV GRADLE_USER_HOME=/opt/gradle/.gradle
COPY --from=deps-downloader /opt/gradle/.gradle /opt/gradle/.gradle

WORKDIR /app
COPY ./src /app/src
COPY ./build.gradle /app/build.gradle

ARG CORE_VERSION
ENV CORE_VERSION=$CORE_VERSION

RUN sed -i -- 's/version: 0.0.0/version: '$CORE_VERSION'/g' /app/src/main/resources/application.yml

RUN gradle bootJar --console=verbose

#######################################################################################
## Stage 3: downloading provisioning scripts
FROM alpine/git:2.36.3 as scripts-downloader
ARG SCRIPTS_REPO_TAG
ARG SCRIPTS_REPO_BRANCH

WORKDIR /root
RUN mkdir scripts
RUN --mount=type=secret,id=scripts_repo_url \
    git clone $(cat /run/secrets/scripts_repo_url) /root/scripts \
    && cd /root/scripts \
    && git checkout tags/${SCRIPTS_REPO_TAG} -b ${SCRIPTS_REPO_BRANCH}

#######################################################################################
## Stage 4: Cytomine core image
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

# tomcat configuration
RUN apt-get -y update && apt-get install -y autoconf automake libpopt-dev libtool make xz-utils
RUN cd /tmp/ && wget https://github.com/logrotate/logrotate/releases/download/3.18.0/logrotate-3.18.0.tar.xz && tar -xJf logrotate-3.18.0.tar.xz
RUN cd /tmp/logrotate-3.18.0 && autoreconf -fiv && ./configure && make

RUN cp /tmp/logrotate-3.18.0/logrotate /usr/sbin/logrotate

COPY --from=jar-builder /app/build/libs/cytomine.jar /app/cytomine.jar

# entrypoint scripts
RUN mkdir /docker-entrypoint-cytomine.d/
COPY --from=scripts-downloader --chmod=774 /root/scripts/cytomine-entrypoint.sh /usr/local/bin/
COPY --from=scripts-downloader --chmod=774 /root/scripts/envsubst-on-templates-and-move.sh /docker-entrypoint-cytomine.d/500-envsubst-on-templates-and-move.sh

WORKDIR /app
ENTRYPOINT ["cytomine-entrypoint.sh"]

CMD ["java", "-jar", "/app/cytomine.jar"]
