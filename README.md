# JSON File Source Connector

__*The following text assumes a general awareness of Kafka, Kafka Connect and the connector
architecture of Kafka Connect that enables external systems to stream to/from Kafka. For a thorough
introduction to these technologies you may refer to the official documentation
[here](https://kafka.apache.org/documentation/), 
[here](https://developer.confluent.io/what-is-apache-kafka/) and
[here](https://docs.confluent.io/platform/current/connect/concepts.html).*__

## Introduction
This is a prototype implementation of a Kafka Connect source connector that reads JSON formatted
events from log files and streams them to Kafka. 

JSON event logs files are where applications log events as JSON objects. These JSON objects describe
interesting events that occur in applications. An application event can be considered interesting if
it serves useful information to another application, or helps build business insights together with
events from other systems.

Kafka is a technology for efficiently moving events between applications with an organisation, and
also to the outside world. Events are published to a Kafka *topic*, and anyone interested consuming
these events will read/consume those events from the same topic. Kafka Connect is part of Kafka
ecosystem, and it helps developers produce or consume events easier than interacting directly with
the Kafka core. Kafka Connect provides a number of common services, for instance converting events
to efficient binary formats and validating events against a schema, that developers would otherwise
have to build themselves. Kafka Connect also comes bundled with numerous *source* and *sink*
connectors for reading from and publishing to well-known systems such as SQL databases and AWS S3.
Developers are able to configure Kafka Connect with any of these connectors to suit their use cases.
As an example, one may configure the JDBC Source Connector to source(i.e. read) updates to a MySQL
table and then sink (i.e. write) these to an AWS S3 bucket with the help of the Amazon S3 Sink
connector.

Kafka Connect, like Kafka itself, can be deployed in a distributed configuration, where multiple
Connect instances form a cluster to share load. Connectors of a Kafka Connect cluster are configured
through a REST API, invoked on any one of participating Kafka Connect nodes. The configuration is
then propagated to every node of the Connect cluster.

The remainder of this document presents implementation aspects of the JSON file source connector.

## JSON Event Log Format
The JSON File Source connector reads JSON formatted events from one or more files. Here's an example:
```json
{
    "schema":"UserEvent:1",
    "body": {
        "event":"OpenSession",
        "userId":"GB22448800",
        "secId":40962048,
        "timestamp":"2023-04-06T15:52:10.325+01:00"
    }
}
```
As shown, the JSON event object includes a reference to the schema used for describing the event
properties. This reference combines the schema name and the version to which the event conforms.
Note that while events are stored as key-value pairs in Kafka, this prototype assumes the key to
be `null`. Hence, the supplied schema applies to the value. A Kafka topic may receive events
of different versions of the same schema or of different schemas. Therefore, applications have
the freedom to emit multitude of different event types.

The properties of the event are contained within a nested object called `body`. The fields of the
event body are application specific. However, the name and the type of each field should conform
to the referenced schema, or else the event is rejected.

## Connector Implementation
This is a fully functional source connector that, in its current implementation, tails a given file,
parses new JSON events in this file, validates them against their specified schemas, and publishes
them to a specified topic. The connector works well in both distributed and standalone modes of
Kafka Connect.

The Maven build places the connector JAR onto the official Confluent Kafka Connect container image
to produce a custom Kafka Connect image. The `CONNECT_PLUGIN_PATH` environment variable of the
custom image points to the connector JAR for Kafka Connect to locate the connector at runtime.

### Features of Prototype
- Remembers the file offset of the last read event, even after the Kafka Connect process terminates.
This means the connector can continue reading the event log from where it left off after Kafka Connect
restarts.
- Able to detect when an event log has been rotated or truncated.
- Caches schemas to avoid calling Schema Registry repeatedly, thereby improving the performance of
the event read-validate-publish loop.

### Limitations of Prototype / TODOs
- Able to monitor only a single file. For a production-ready implementation, the connector should
be able to watch a given directory and read existing and new files within this directory.
- Uses a hard-coded schema. This should be changed to read schemas from the Schema Registry.
- Publishes events to a single Kafka topic.

### Building the Connector
This is a Maven project, therefore the connector JAR can be easily built with the standard Maven commands.
The Maven `pom.xml` file is also configured with a [Maven Docker plugin](https://dmp.fabric8.io/) for
building the custom Kafka Connect container image together with the connector JAR. The image is built using
the `src/main/docker/Dockerfile` Dockerfile.

To build just the connector JAR (i.e. without producing the container image), run:
```shell
mvn clean package
```

To build the custom Kafka Connect container add `docker:build` to the Maven command line.
```shell
mvn clean package docker:build
```

## Deploying the Connector with Kafka Connect
To deploy this connector correctly, both Kafka Connect and the connector itself have to be configured.
How we configure these vary between standalone and distributed deployments. However, we assume a Kafka
Connect cluster hosted in Kubernetes and therefore the following instructions apply for distributed
Kafka Connect.

### Notes
1) At this point we assume that a Kafka cluster with at least two nodes and a Schema Registry has
already been provisioned and available for use.
2) Examples shown here use the CLI tools (or scripts) included with the Confluent Kafka distribution.
While functionality of these tools should be identical to those distributed with Apache Kafka, their
names may slightly differ from those of Apache Kafka.

### Prerequisites
Before starting Kafka Connect, the required system topics and the topic for publishing application
events should be created. While we can let Kafka Connect create the system topics with default
properties, this is not recommended. Make sure to run the following commands to create the topics
*before* starting Kafka Connect or any Kafka consumer that listens to the `user-events` topic.
```shell
kafka-topics --bootstrap-server <kafka-broker:port> --create --topic user-events-offsets --replication-factor 1 --partitions 4 --config cleanup.policy=compact

kafka-topics --bootstrap-server <kafka-broker:port> --create --topic user-events-config --replication-factor 2 --partitions 1 --config cleanup.policy=compact

kafka-topics --bootstrap-server <kafka-broker:port> --create --topic user-events-status --replication-factor 1 --partitions 4 --config cleanup.policy=compact

kafka-topics --bootstrap-server <kafka-broker:port> --create --topic user-events --replication-factor 2 --partitions 8 --config cleanup.policy=delete
```

To list the newly created topics (and other existing topics) use the command:
```shell
kafka-topics --bootstrap-server <kafka-broker:port> --list
```

### Kafka Connect Configuration (distributed)
The Kafka Connect configuration given bellow is used when launching Kafka Connect as a container.
In this case the configuration injected through a number of environment variables.

```shell
export CONNECT_REST_PORT='8083'
export CONNECT_BOOTSTRAP_SERVERS=<kafka-broker:port>
export CONNECT_GROUP_ID=app-events
export CONNECT_CONFIG_STORAGE_TOPIC=user-events-config
export CONNECT_OFFSET_STORAGE_TOPIC=user-events-offsets
export CONNECT_STATUS_STORAGE_TOPIC=user-events-status
export CONNECT_KEY_CONVERTER=org.apache.kafka.connect.storage.StringConverter
export CONNECT_VALUE_CONVERTER=io.confluent.connect.avro.AvroConverter
export CONNECT_VALUE_CONVERTER_SCHEMA_REGISTRY_URL=http://<schema-registry:port>
export CONNECT_REST_ADVERTISED_HOST_NAME=<externally-resolvable-hostname>
export JSON_SOURCE_CONNECTOR_NODENAME=<unique-stable-nodename>
```
For an example how these environment variables have been configured to deploy Kafka Connect under
Kubernetes, refer to these Kubernetes specs:
<TODO>

Note that the `JSON_SOURCE_CONNECTOR_NODENAME` is specific to the JSON File Source connector and
is not part of the Kafka Connect configuration. This property is used together with the JSON event
file names to generate a unique Kafka Connect partition names for persisting offsets of those files.

### JSON File Source Connector Configuration (distributed)
As noted earlier, connectors hosted on Kafka Connect running in distributed mode are configured
by calling a REST endpoint on Kafka Connect. The JSON configuration for this prototype connector is:
```json
{
  "name":"user-events",
  "config":{
    "connector.class":"kafka.connect.json.JsonSourceConnector",
    "tasks.max":"2",
    "source.filename":"/var/log/events.log",
    "schema.registry.url":"http://<schema-registry:port>",
    "topic":"user-events"
  }
}
```
To start the connector with the above configuration, we can run a `curl` request to Kafka Connect's
REST API as follows (assume that `curl` is run from a host/container where Kafka Connect is running).
```shell
curl -X POST -H "Content-Type: application/json" --data\
 '{"name":"user-events","config":{"connector.class":"kafka.connect.json.JsonSourceConnector","tasks.max":"2","source.filename":"/var/log/events.log","schema.registry.url":"http://<schema-registry:port>","topic":"user-events"}}'\
  http://localhost:8083/connectors 
```
Once the connector has been loaded, its status can be queried anytime with this REST call.
```shell
curl localhost:8083/connectors/user-events/status
```
Finally, if we want to stop the connector, we can do so with this REST call.
```shell
curl -X DELETE localhost:8083/connectors/user-events
```

### Testing the JSON File Source Connector
We now have Kafka Connect and the JSON File Source connector running, and waiting to consume
events. To write an event to the `/var/events/events.log` file the connector is tailing, jump into
one of the hosts/container where Kafka Connect is running and do:
```shell
echo '{"schema":"UserEvent:1","body":{"event":"OpenSession","userId":"GB22448800","secId":40962048,"timestamp":"2023-04-06T15:52:10.325+01:00"}}' >> /var/events/events.log
```
It would be useful at this point to monitor the app logs of Kafka Connect to ensure there are no
errors in either Kafka Connect or the connector. If in Kubernetes, we can easily tail logs of a Kafka
Connect Pod by doing:
```shell
kubectl logs -f kafka-connect-0
```
Once we are certain that events are successfully being streamed to Kafka, we should be able to consume 
them with the help of console consumer tools that ship with Kafka. In our case we need to use the Avro
Console Consumer tool as our events are encoded in the Avro binary format.
```shell
kafka-avro-console-consumer --topic user-events --from-beginning --bootstrap-server <kafka-broker:port> --property 'schema.registry.url=http://<schema-registry:port>'
```

## Further reading
- [Kafka Connect User Guide](https://docs.confluent.io/platform/current/connect/userguide.html)
- [Connect REST Interface](https://docs.confluent.io/platform/current/connect/references/restapi.html)
- [Connector Developer Guide](https://docs.confluent.io/platform/current/connect/devguide.html)
