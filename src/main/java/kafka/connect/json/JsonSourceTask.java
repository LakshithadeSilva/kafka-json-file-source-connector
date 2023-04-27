package kafka.connect.json;

import com.fasterxml.jackson.core.JacksonException;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.source.SourceTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonSourceTask extends SourceTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonSourceTask.class);

    private static final Duration TASK_POLL_SLEEP_DURATION = Duration.ofSeconds(2);

    private static final String SOURCE_PARTITION_KEY = "partition";
    private static final String SOURCE_OFFSET_KEY = "offset";

    private Map<String, String> sourcePartition;
    private Map<String, Long> sourceOffset;
    private String topic;
    private File source;

    @Override
    public void start(Map<String, String> properties)  {
        LOGGER.info("Initialising JSON source task...");

        topic = properties.get(Configuration.Keys.TOPIC);
        source = new File(properties.get(Configuration.Keys.SOURCE_FILENAME));

        var nodename = properties.get(Configuration.Keys.SOURCE_FILENAME);
        var partitionName = nodename + '-' + source.getAbsolutePath();
        sourcePartition = Map.of(SOURCE_PARTITION_KEY, partitionName);

        sourceOffset = new HashMap<>();
        var previousOffset = context.offsetStorageReader().offset(sourcePartition);
        var offset = previousOffset == null ? 0L : (Long) previousOffset.get(SOURCE_OFFSET_KEY);
        sourceOffset.put(SOURCE_OFFSET_KEY, offset);
    }

    @Override
    public List<SourceRecord> poll() throws InterruptedException {
        Thread.sleep(TASK_POLL_SLEEP_DURATION.toMillis());

        if (!source.exists()) {
            LOGGER.warn("Source does not yet exist; no data returned to Connect");
            return null;
        }

        var offset = sourceOffset.get(SOURCE_OFFSET_KEY);
        var length = source.length();
        if (length < offset) {
            LOGGER.warn("Source appears to have been rotated; setting offset to 0");
            offset = 0L;
        }

        var records = new ArrayList<SourceRecord>();
        try (var sourceHandle = new RandomAccessFile(source, "r")) {
            sourceHandle.seek(offset);
            String line;
            while ((line = sourceHandle.readLine()) != null) {
                var event = EventParser.instance().parse(line);
                var sourceRecord = new SourceRecord(sourcePartition, sourceOffset, topic, event.schema(), event);
                records.add(sourceRecord);

                offset = sourceHandle.getFilePointer();
                sourceOffset.put(SOURCE_OFFSET_KEY, offset);
            }
        } catch (JacksonException e) {
            LOGGER.error("Error parsing JSON data from source; no data returned to Connect");
            return null;

        } catch (IOException e) {
            LOGGER.error("Error reading source; no data returned to Connect");
            return null;

        } catch (Exception e) {
            LOGGER.error("Error serialising data to Connect: " + e);
            return null;
        }
        return records;
    }

    @Override
    public synchronized void stop() {
        LOGGER.info("Shutting down JSON source task...");
    }

    @Override
    public String version() {
        return Configuration.VERSION;
    }
}
