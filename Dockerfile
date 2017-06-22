# Verified with http://hadolint.lukasmartinelli.ch/
FROM hbpmip/scala-base-build:6d7528a as build-scala-env

USER root
COPY build.sbt /build/
RUN  mkdir -p /build/project/
COPY project/build.properties project/plugins.sbt /build/project/

RUN sbt about

COPY src/ /build/src/

RUN sbt assembly

FROM openjdk:8u131-jdk-alpine

MAINTAINER ludovic.claude54@gmail.com

COPY docker/runner/woken.sh /opt/woken/

RUN adduser -H -D -u 1000 woken \
    && chmod +x /opt/woken/woken.sh \
    && ln -s /opt/woken/woken.sh /run.sh \
    && chown -R woken:woken /opt/woken

COPY --from=build-scala-env /my-project/target/scala-2.11/woken-assembly-dev.jar /opt/woken/woken.jar

USER woken

# org.label-schema.build-date=$BUILD_DATE
# org.label-schema.vcs-ref=$VCS_REF
LABEL org.label-schema.schema-version="1.0" \
        org.label-schema.license="Apache 2.0" \
        org.label-schema.name="woken" \
        org.label-schema.description="An orchestration platform for Docker containers running data mining algorithms" \
        org.label-schema.url="https://github.com/LREN-CHUV/woken" \
        org.label-schema.vcs-type="git" \
        org.label-schema.vcs-url="https://github.com/LREN-CHUV/woken" \
        org.label-schema.vendor="LREN CHUV" \
        org.label-schema.version="githash" \
        org.label-schema.docker.dockerfile="Dockerfile" \
        org.label-schema.memory-hint="2048"

EXPOSE 8087
EXPOSE 8088

CMD ["/run.sh"]
