package kafka.connect.json;

import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.source.SourceConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class JsonSourceConnector extends SourceConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonSourceConnector.class);

    private static final ConfigDef CONFIG_DEFINITIONS = new ConfigDef()
            .define(Configuration.Keys.SOURCE_FILENAME,
                    ConfigDef.Type.STRING,
                    ConfigDef.Importance.HIGH,
                    "Name of file from where to read JSON formatted events")
            .define(Configuration.Keys.SCHEMA_REGISTRY_URL,
                    ConfigDef.Type.STRING,
                    ConfigDef.Importance.HIGH,
                    "Schema Registry URL for retrieving schemas")
            .define(Configuration.Keys.TOPIC,
                    ConfigDef.Type.STRING,
                    ConfigDef.Importance.HIGH,
                    "Name of Kafka topic where events are published");

    private String nodename;
    private String filename;
    private String schemaRegistry;
    private String topic;

    @Override
    public void start(Map<String, String> properties) {
        LOGGER.info("Starting JSON source connector...");

        var parsedConfig = new AbstractConfig(CONFIG_DEFINITIONS, properties);

        nodename = System.getenv(Configuration.Keys.ENV_SOURCE_NODENAME);
        filename = parsedConfig.getString(Configuration.Keys.SOURCE_FILENAME);
        schemaRegistry = parsedConfig.getString(Configuration.Keys.SCHEMA_REGISTRY_URL);
        topic = parsedConfig.getString(Configuration.Keys.TOPIC);
    }

    @Override
    public void stop() {
        LOGGER.info("Shutting down JSON source connector...");
    }

    @Override
    public Class<? extends Task> taskClass() {
        return JsonSourceTask.class;
    }

    @Override
    public List<Map<String, String>> taskConfigs(int maxTasks) {
        var configurations = new ArrayList<Map<String, String>>();
        for (var i = 0; i < maxTasks; ++i) {
            var taskConfiguration = new HashMap<String, String>();
            taskConfiguration.put(Configuration.Keys.SOURCE_NODENAME, nodename);
            taskConfiguration.put(Configuration.Keys.SOURCE_FILENAME, filename);
            taskConfiguration.put(Configuration.Keys.SCHEMA_REGISTRY_URL, schemaRegistry);
            taskConfiguration.put(Configuration.Keys.TOPIC, topic);
            configurations.add(taskConfiguration);
        }
        LOGGER.info("Configured {} tasks for the JSON File Source connector.", configurations.size());
        return configurations;
    }

    @Override
    public ConfigDef config() {
        return CONFIG_DEFINITIONS;
    }

    @Override
    public String version() {
        return Configuration.VERSION;
    }
}
