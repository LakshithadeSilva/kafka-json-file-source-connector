package kafka.connect.json;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SchemaParserTest {

    private static final String VALID_SCHEMA = """
            {
                "name":"UserEvent",
                "version":"1",
                "fields":[
                {
                    "name":"type",
                    "type":"string"
                },
                {
                    "name":"id",
                    "type":"long"
                }]
            }
            """;

    private static final String SCHEMA_WITH_NO_FIELDS = """
            {
                "name":"UserEvent",
                "version":"1",
                "fields":[]
            }
            """;

    private static final String SCHEMA_WITH_BAD_FIELD_TYPE = """
            {
                "name":"UserEvent",
                "version":"1",
                "fields":[
                {
                    "name":"type",
                    "type":"integer"
                }]
            }
            """;

    private static final String SCHEMA_WITH_MISSING_HEADER = """
            {
                "name":"UserEvent",
                "fields":[
                {
                    "name":"type",
                    "type":"integer"
                }]
            }
            """;

    private final SchemaParser parser = SchemaParser.instance();

    @Test
    public void testSchemaNameIsParsed() {
        var schema = parser.parse(VALID_SCHEMA);
        assertEquals("UserEvent", schema.name());
    }

    @Test
    public void testSchemaVersionIsParsed() {
        var schema = parser.parse(VALID_SCHEMA);
        assertEquals(1, schema.version());
    }

    @Test
    public void testFullListOfSchemaFieldsIsParsed() {
        var schema = parser.parse(VALID_SCHEMA);
        assertEquals(2, schema.fields().size());
    }

    @Test
    public void testSchemaWithNoFields() {
        assertThrows(RuntimeException.class, () -> parser.parse(SCHEMA_WITH_NO_FIELDS));
    }

    @Test
    public void testSchemaWithBadFieldType() {
        assertThrows(RuntimeException.class, () -> parser.parse(SCHEMA_WITH_BAD_FIELD_TYPE));
    }

    @Test
    public void testSchemaWithMissingHeader() {
        assertThrows(RuntimeException.class, () -> parser.parse(SCHEMA_WITH_MISSING_HEADER));
    }
}
