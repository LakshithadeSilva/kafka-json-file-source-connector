package kafka.connect.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SchemaParser {

    private static volatile SchemaParser instance;

    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, Schema> typeMapper = new HashMap<>();

    private SchemaParser() {
        typeMapper.put("int", Schema.INT32_SCHEMA);
        typeMapper.put("long", Schema.INT64_SCHEMA);
        typeMapper.put("float", Schema.FLOAT32_SCHEMA);
        typeMapper.put("double", Schema.FLOAT64_SCHEMA);
        typeMapper.put("string", Schema.STRING_SCHEMA);
        typeMapper.put("boolean", Schema.BOOLEAN_SCHEMA);
    }

    public static SchemaParser instance() {
        if (instance == null) {
            synchronized (SchemaParser.class) {
                if (instance == null) {
                    instance = new SchemaParser();
                }
            }
        }
        return instance;
    }

    public Schema parse(String jsonSchema) {
        try {
            var schemaSpec = mapper.readValue(jsonSchema, JsonSchema.class);
            var schemaBuilder = SchemaBuilder.struct()
                    .name(schemaSpec.name)
                    .version(schemaSpec.version);

            if (!schemaSpec.fields.isEmpty()) {
                for (JsonSchemaField field : schemaSpec.fields) {
                    var schemaType = typeMapper.get(field.type);
                    if (schemaType == null) {
                        throw new RuntimeException("Invalid field type in schema");
                    }
                    schemaBuilder.field(field.name, schemaType);
                }
                return schemaBuilder.build();
            }
            throw new RuntimeException("No fields found in schema");

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error de-serialising schema", e);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown=true)
    private static class JsonSchema {

        private final String name;
        private final int version;
        private final List<JsonSchemaField> fields;

        @JsonCreator
        private JsonSchema(
                @JsonProperty(value = "name", required = true) String name,
                @JsonProperty(value = "version", required = true) int version,
                @JsonProperty(value = "fields", required = true) List<JsonSchemaField> fields) {
            this.name = name;
            this.version = version;
            this.fields = fields;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown=true)
    private static class JsonSchemaField {

        private final String name;
        private final String type;

        private JsonSchemaField(
                @JsonProperty(value = "name", required = true) String name,
                @JsonProperty(value = "type", required = true) String type) {
            this.name = name;
            this.type = type;
        }
    }
}
