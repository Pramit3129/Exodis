package redis.datastructure;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

public class RedisStream {

    private final NavigableMap<StreamId, StreamEntry> entries =
            new TreeMap<>();

    private StreamId lastGeneratedId = new StreamId(0, 0);

    public synchronized StreamId add(
            String requestedId,
            List<String> fieldsAndValues
    ) {
        StreamId id = generateId(requestedId);

        // Rule 1: Redis Stream IDs must be greater than 0-0
        if (id.milliseconds() == 0 && id.sequence() == 0) {
            throw new IllegalArgumentException(
                    "The ID specified in XADD must be greater than 0-0"
            );
        }

        // Rule 2: New ID must be greater than the last inserted ID
        if (id.compareTo(lastGeneratedId) <= 0) {
            throw new IllegalArgumentException(
                    "The ID specified in XADD is equal or smaller "
                            + "than the target stream top item"
            );
        }

        StreamEntry entry = new StreamEntry(id, fieldsAndValues);

        entries.put(id, entry);
        lastGeneratedId = id;

        notifyAll();

        return id;
    }

    private StreamId generateId(String requestedId) {
        if (requestedId.equals("*")) {
            long now = System.currentTimeMillis();

            long milliseconds =
                    Long.compareUnsigned(now, lastGeneratedId.milliseconds()) >= 0
                            ? now
                            : lastGeneratedId.milliseconds();

            long sequence;

            if (milliseconds == lastGeneratedId.milliseconds()) {
                if (lastGeneratedId.sequence() == -1L) {
                    throw new IllegalArgumentException("Stream ID overflow");
                }
                sequence = lastGeneratedId.sequence() + 1;
            } else {
                sequence = 0;
            }

            return new StreamId(milliseconds, sequence);
        }

        if (requestedId.endsWith("-*")) {
            String timestampPart =
                    requestedId.substring(0, requestedId.length() - 2);

            long milliseconds = Long.parseUnsignedLong(timestampPart);

            int comparison = Long.compareUnsigned(
                    milliseconds,
                    lastGeneratedId.milliseconds()
            );

            if (comparison < 0) {
                throw new IllegalArgumentException(
                        "Stream ID is smaller than last ID"
                );
            }

            long sequence;

            if (comparison == 0) {
                if (lastGeneratedId.sequence() == -1L) {
                    throw new IllegalArgumentException("Stream ID overflow");
                }
                sequence = lastGeneratedId.sequence() + 1;
            } else {
                sequence = milliseconds == 0 ? 1 : 0;
            }

            return new StreamId(milliseconds, sequence);
        }

        return StreamId.parse(requestedId);
    }

    public synchronized List<StreamEntry> range(
            StreamId start,
            StreamId end
    ) {
        if (start.compareTo(end) > 0) {
            return List.of();
        }

        return new ArrayList<>(
                entries.subMap(start, true, end, true).values()
        );
    }

    public synchronized List<StreamEntry> readAfter(
            StreamId lastSeenId
    ) {
        return new ArrayList<>(
                entries.tailMap(lastSeenId, false).values()
        );
    }

    public synchronized StreamId getLastId() {
        return lastGeneratedId;
    }
}