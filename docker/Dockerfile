ARG CORE_VERSION
ARG SCRIPTS_REPO_TAG


#######################################################################################
# Stage: core dependencies download via gradle
FROM gradle:7.2-jdk17-alpine AS deps-downloader

# We first copy the build.gradle file and the binaries stored in the source repository.
# This way, we retrieve all gradle dependencies at the beginning. All these steps will be
# cached by Docker unless build.gradle. This means that we only retrieve all dependencies 
# if we modify the dependencies definition.
RUN mkdir -p /opt/gradle/.gradle
ENV GRADLE_USER_HOME=/opt/gradle/.gradle

WORKDIR /app
COPY ../build.gradle /app/build.gradle

RUN gradle clean build --no-daemon --console=verbose

#######################################################################################
## Stage: building the core jar file
FROM gradle:7.2-jdk17-alpine AS jar-builder

ENV GRADLE_USER_HOME=/opt/gradle/.gradle
COPY --from=deps-downloader /opt/gradle/.gradle /opt/gradle/.gradle

WORKDIR /app
COPY ../src /app/src
COPY ../build.gradle /app/build.gradle

ARG CORE_VERSION
ENV CORE_VERSION=$CORE_VERSION

RUN sed -i -- 's/version: 0.0.0/version: '$CORE_VERSION'/g' /app/src/main/resources/application.yml

RUN gradle bootJar --console=verbose

#######################################################################################
## Stage: downloading provisioning scripts
FROM alpine/git:2.36.3 as scripts-downloader

ARG SCRIPTS_REPO_TAG="latest"
ARG SCRIPT_REPO_URL="https://github.com/cytomine/cytomine-docker-entrypoint-scripts.git"

WORKDIR /root

RUN mkdir scripts
RUN git clone $SCRIPT_REPO_URL /root/scripts \
    && cd /root/scripts \
    && git checkout tags/$SCRIPTS_REPO_TAG

#######################################################################################
## Stage: Cytomine core development image
FROM gradle:7.2-jdk17-alpine AS development

RUN apk update && apk add gettext

ENV GRADLE_USER_HOME=/opt/gradle/.gradle
COPY --from=deps-downloader /opt/gradle/.gradle /opt/gradle/.gradle

# startup scripts
RUN mkdir /docker-entrypoint-cytomine.d/
COPY --from=scripts-downloader --chmod=774 /root/scripts/cytomine-entrypoint.sh /usr/local/bin/
COPY --from=scripts-downloader --chmod=774 /root/scripts/envsubst-on-templates-and-move.sh /docker-entrypoint-cytomine.d/500-envsubst-on-templates-and-move.sh
COPY --from=scripts-downloader --chmod=774 /root/scripts/configure-etc-hosts-reverse-proxy.sh /docker-entrypoint-cytomine.d/750-configure-etc-hosts-reverse-proxy.sh

# core base folder must be mounted in run command or compose file
EXPOSE 5005

WORKDIR /app
ENTRYPOINT ["cytomine-entrypoint.sh"]
CMD ["gradle", ":bootRun", "--debug-jvm"]

#######################################################################################
## Stage: Cytomine core image
FROM openjdk:17-slim-bullseye as production

# base librairies and configuration
RUN apt-get update -y && apt-get -y install logrotate

RUN sed -i "/su root syslog/c\su root root" /etc/logrotate.conf
ENV LANG C.UTF-8
ENV DEBIAN_FRONTEND noninteractive

# tomcat configurations
RUN apt-get update -y && apt-get install --no-install-recommends --no-install-suggests -y logrotate gettext

COPY --from=jar-builder /app/build/libs/cytomine.jar /app/cytomine.jar

# entrypoint scripts
RUN mkdir /docker-entrypoint-cytomine.d/
COPY --from=scripts-downloader --chmod=774 /root/scripts/cytomine-entrypoint.sh /usr/local/bin/
COPY --from=scripts-downloader --chmod=774 /root/scripts/envsubst-on-templates-and-move.sh /docker-entrypoint-cytomine.d/500-envsubst-on-templates-and-move.sh
COPY --from=scripts-downloader --chmod=774 /root/scripts/configure-etc-hosts-reverse-proxy.sh /docker-entrypoint-cytomine.d/750-configure-etc-hosts-reverse-proxy.sh
COPY --chmod=774 ./scripts/generate-meta-prefix-file.sh /docker-entrypoint-cytomine.d/875-generate-meta-prefix-file.sh

WORKDIR /app
ENTRYPOINT ["cytomine-entrypoint.sh"]

CMD ["java", "-jar", "/app/cytomine.jar"]
