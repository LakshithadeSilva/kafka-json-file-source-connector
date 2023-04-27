FROM confluentinc/cp-kafka-connect:7.4.0

MAINTAINER  Lakshitha de Silva
LABEL name="Kafka Connect" \
      description="Kafka Connect with JSON File Source Connector" \
      copyright="None"

ARG BUILD_ARTIFACT
ARG BUILD_LIB_DIRECTORY
ARG CONNECTOR_HOME=/opt/kafka-connect

WORKDIR ${CONNECTOR_HOME}
COPY ${BUILD_ARTIFACT} ${CONNECTOR_HOME}/
COPY ${BUILD_LIB_DIRECTORY} ${CONNECTOR_HOME}/${BUILD_LIB_DIRECTORY}/

ENV CONNECT_PLUGIN_PATH=${CONNECTOR_HOME}/${BUILD_ARTIFACT}
