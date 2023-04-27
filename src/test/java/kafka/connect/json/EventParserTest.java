package kafka.connect.json;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EventParserTest {

    private static final String JSON_EVENT = """
            {
                "schema":"UserEvent:1",
                "body": {
                    "event":"OpenSession",
                    "userId":"GB22448800",
                    "secId":40962048,
                    "timestamp":"2023-04-06T15:52:10.325+01:00"
                }
            }
            """;

    EventParser parser = EventParser.instance();

    @Test
    public void testNameOfEventSchemaReferenceMatchesAssociatedSchemaName() {
        var event = parser.parse(JSON_EVENT);
        assertEquals("UserEvent", event.schema().name());
        assertEquals(1, event.schema().version());
    }

    @Test
    public void tesVersionOfEventSchemaReferenceMatchesAssociatedSchemaVersion() {
        var event = parser.parse(JSON_EVENT);
        assertEquals(1, event.schema().version());
    }

    @Test
    public void testAllEventPropertiesAreParsed() {
        var event = parser.parse(JSON_EVENT);
        assertEquals("OpenSession", event.getString("event"));
        assertEquals("GB22448800", event.getString("userId"));
        assertEquals(40962048, event.getInt32("secId"));
        assertEquals("2023-04-06T15:52:10.325+01:00", event.getString("timestamp"));
    }
}
