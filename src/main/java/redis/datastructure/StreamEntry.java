package redis.datastructure;

import java.util.List;

public record StreamEntry(StreamId id, List<String> fieldsAndValues) {
    public StreamEntry {
        fieldsAndValues = List.copyOf(fieldsAndValues);
    }
}