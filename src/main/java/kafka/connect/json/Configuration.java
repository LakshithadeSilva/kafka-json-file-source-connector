package kafka.connect.json;

public class Configuration {

    public static final String VERSION = "1.0";

    public static class Keys {

        public static final String SOURCE_NODENAME = "source.nodename";
        public static final String SOURCE_FILENAME = "source.filename";
        public static final String SCHEMA_REGISTRY_URL = "schema.registry.url";
        public static final String TOPIC = "topic";

        public static final String ENV_SOURCE_NODENAME = "JSON_SOURCE_CONNECTOR_NODENAME";
    }
}
