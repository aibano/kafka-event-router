package dev.logicdee.kafka.smt.mongo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.header.Headers;
import org.apache.kafka.connect.transforms.Transformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

public class EventRouter<R extends ConnectRecord<R>> implements Transformation<R> {
    public static final String EVENT_ID = "_EventId_";

    private static final Logger logger = LoggerFactory.getLogger(EventRouter.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    public EventRouter() {

    }

    @Override
    public void configure(Map<String, ?> configs) {
    }

    @Override
    public R apply(R record) {
        // Ignoring tombstones just in case
        if (record.value() == null) {
            return record;
        }

        Struct struct = (Struct) record.value();
        String op = struct.getString("op");

        System.out.println(struct);

        // ignoring deletions in the events table
        if (op.equals("d")) {
            return null;
        }
        else if (op.equals("c") || op.equals("r")) {
            try {
                Long timestamp = struct.getInt64("ts_ms");
                String after = struct.getString("after");
                Event event = mapper.readValue(after, Event.class);

                logger.debug("Received event {}", event);
                String key = event.getAggregateId();
                String topic = event.getAggregateType() + "Events";

                String eventId = event.getId().toString();
                String value = mapper.writeValueAsString(event);

                Headers headers = record.headers();
                headers.addString(EVENT_ID, eventId);
                logger.info("Route message key {} value {} to topic {}", key, value, topic);
                return record.newRecord(topic, null, Schema.STRING_SCHEMA, key, Schema.STRING_SCHEMA, value, record.timestamp(), headers);
            } catch (IOException e) {
                logger.error("Error processing event", e);
                throw new RuntimeException(e);
            }
        }
        else {
            throw new IllegalArgumentException("Record of unexpected op type: " + record);
        }
    }

    @Override
    public ConfigDef config() {
        return new ConfigDef();
    }

    @Override
    public void close() {
    }
}
