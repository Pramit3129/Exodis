package redis.command;

import redis.datastructure.RedisList;
import redis.datastructure.RedisStream;
import redis.datastructure.StreamEntry;
import redis.datastructure.StreamId;
import redis.model.RedisValue;
import static redis.protocol.RespWriter.*;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public final class StreamCommands {
    private StreamCommands() {}

    public static void handleType(OutputStream output, String[] commands, ConcurrentHashMap<String, RedisValue> mp, ConcurrentHashMap<String, RedisList> listMap, ConcurrentHashMap<String, RedisStream> streams) throws IOException {
        String keyName = commands[1];
        if(mp.containsKey(keyName))
            writeSimpleString(output, "string");
        else if(listMap.containsKey(keyName))
            writeSimpleString(output,"list");
        else if(streams.containsKey(keyName))
            writeSimpleString(output, "stream");
        else
            writeSimpleString(output, "none");
    }

    public static void executeXADD(
            OutputStream output,
            String[] command,
            ConcurrentHashMap<String, RedisStream> streams
    ) throws IOException {

        if (command.length < 5 || command.length % 2 == 0) {
            writeError(output, "ERR wrong number of arguments for XADD");
            return;
        }

        String key = command[1];
        String requestedId = command[2];

        List<String> fields = Arrays.asList(
                Arrays.copyOfRange(command, 3, command.length)
        );

        RedisStream stream = streams.computeIfAbsent(
                key, ignored -> new RedisStream()
        );

        try {
            // System.err.println("XADD requested ID: " + requestedId);

            StreamId id = stream.add(requestedId, fields);

            // System.err.println("XADD accepted ID: " + id);

            writeBulkString(output, id.toString());

        } catch (IllegalArgumentException e) {

            // System.err.println("XADD validation error: " + e.getMessage());

            writeError(output, "ERR "+ e.getMessage());
        }
    }

    private static void executeXrange(
            OutputStream output,
            String[] command,
            ConcurrentHashMap<String, RedisStream> streams
    ) throws IOException {

        if (command.length != 4) {
            writeError(output, "ERR wrong number of arguments for XRANGE");
            return;
        }

        RedisStream stream = streams.get(command[1]);

        if (stream == null) {
            writeArrayHeader(output, 0);
            return;
        }

        try {
            StreamId start = command[2].equals("-")
                    ? new StreamId(0, 0)
                    : StreamId.parse(command[2]);

            StreamId end = command[3].equals("+")
                    ? new StreamId(-1L, -1L)
                    : StreamId.parse(command[3]);

            writeStreamEntries(output, stream.range(start, end));

        } catch (IllegalArgumentException e) {
            writeError(output, "ERR "+e.getMessage());
        }
    }

    private static void executeXread(
            OutputStream output,
            String[] command,
            ConcurrentHashMap<String, RedisStream> streams
    ) throws IOException {

        // Initial version: XREAD STREAMS key id
        if (command.length != 4 ||
                !command[1].equalsIgnoreCase("STREAMS")) {
            writeError(output, "ERR invalid XREAD arguments");
            return;
        }

        String key = command[2];
        RedisStream stream = streams.get(key);

        if (stream == null) {
            writeNullArray(output);
            return;
        }

        try {
            StreamId start = StreamId.parse(command[3]);

            List<StreamEntry> result = stream.readAfter(start);

            if (result.isEmpty()) {
                writeNullArray(output);
                return;
            }

            // XREAD response:
            // [ [streamName, [entries...]] ]

            writeArrayHeader(output, 1);
            writeArrayHeader(output, 2);

            writeBulkString(output, key);
            writeStreamEntries(output, result);

        } catch (IllegalArgumentException e) {
            writeError(output, "ERR "+e.getMessage());
        }
    }
}
