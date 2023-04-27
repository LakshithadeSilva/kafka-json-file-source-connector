package kafka.connect.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Struct;

import java.util.HashMap;
import java.util.Map;

public class EventParser {

    // In this prototype, the schema is hard coded. However, it should be read from
    // a schema registry using the schema reference accompanying each event. The
    // event schema here follows the Confluent Schema Registry format
    private static final String EVENT_SCHEMA = """
            {
                "name":"UserEvent",
                "version":"1",
                "fields":[
                {
                    "name":"event",
                    "type":"string"
                },
                {
                    "name":"userId",
                    "type":"string"
                },
                {
                    "name":"secId",
                    "type":"int"
                },
                {
                    "name":"timestamp",
                    "type":"string"
                }]
            }
            """;

    private static volatile EventParser instance;

    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, Schema> schemaCache = new HashMap<>();

    private EventParser() {
        // As noted above, the schema should be read from the schema registry, if not
        // already found in the cache, and cache it for subsequent event validation.
        // This should be done for each event read from log files
        schemaCache.put("UserEvent:1", SchemaParser.instance().parse(EVENT_SCHEMA));
    }

    public static EventParser instance() {
        if (instance == null) {
            synchronized (EventParser.class) {
                if (instance == null) {
                    instance = new EventParser();
                }
            }
        }
        return instance;
    }

    public Struct parse(String jsonEvent) {
        try {
            var event = mapper.readValue(jsonEvent, Event.class);
            return packageEventWithSchema(event);

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error parsing JSON event object", e);
        }
    }

    private Struct packageEventWithSchema(Event event) {
        var eventSchema = retrieveSchema(event.schema);
        var eventStruct = new Struct(eventSchema);

        for (var entry : event.body.entrySet()) {
            eventStruct.put(entry.getKey(), entry.getValue());
        }
        return eventStruct;
    }

    private Schema retrieveSchema(String schema) {
        // The schema should be specified in the <name:version> format
        // If the schema is not available in the cache, it must be retrieved from
        // the schema registry, converted to a Schema object and placed in the cache
        return schemaCache.get(schema);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Event {

        private final String schema;
        private final Map<String, Object> body;

        @JsonCreator
        private Event(
                @JsonProperty(value = "schema", required = true) String schema,
                @JsonProperty(value = "body", required = true) Map<String, Object> body) {
            this.schema = schema;
            this.body = body;
        }
    }
}
