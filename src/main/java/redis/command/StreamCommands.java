package redis.command;

import redis.datastructure.RedisList;
import redis.model.RedisValue;
import static redis.protocol.RespWriter.*;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ConcurrentHashMap;

public final class StreamCommands {
    private StreamCommands() {}

    public static void handleType(OutputStream output, String[] commands, ConcurrentHashMap<String, RedisValue> mp, ConcurrentHashMap<String, RedisList> listMap) throws IOException {
        String keyName = commands[1];
        if(mp.containsKey(keyName))
            writeSimpleString(output, new String("string"));
        else if(listMap.containsKey(keyName))
            writeSimpleString(output, new String("list"));
        else
            writeSimpleString(output, new String("none"));
    }
}
