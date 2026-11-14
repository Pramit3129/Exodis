package redis.command;

import redis.model.RedisValue;
import redis.model.SetOptions;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ConcurrentHashMap;

import static redis.protocol.RespWriter.*;
import static redis.storage.StorageUtils.getLiveEntry;

public final class SetCommand {
    private SetCommand() {}

    public static SetOptions parseSetOptions(String[] command) throws IOException {
        SetOptions options = new SetOptions();
        boolean conditionSpecified = false;
        boolean expirationSpecified = false;
        int i = 3;
        while (i < command.length) {
            String option = command[i].toUpperCase();
            switch (option) {
                case "NX":
                    if (conditionSpecified) throw new IOException("ERR syntax error");
                    options.nx = true;
                    conditionSpecified = true;
                    i++;
                    break;
                case "XX":
                    if (conditionSpecified) throw new IOException("ERR syntax error");
                    options.xx = true;
                    conditionSpecified = true;
                    i++;
                    break;
                case "GET":
                    if (options.get) throw new IOException("ERR syntax error");
                    options.get = true;
                    i++;
                    break;
                case "KEEPTTL":
                    if (expirationSpecified) throw new IOException("ERR syntax error");
                    options.keepTtl = true;
                    expirationSpecified = true;
                    i++;
                    break;
                case "EX":
                case "PX":
                case "EXAT":
                case "PXAT":
                    if (expirationSpecified) throw new IOException("ERR syntax error");
                    if (i + 1 >= command.length) throw new IOException("ERR syntax error");
                    long number;
                    try {
                        number = Long.parseLong(command[i + 1]);
                    } catch (NumberFormatException e) {
                        throw new IOException("ERR invalid expire time");
                    }
                    if (number <= 0) throw new IOException("ERR invalid expire time");
                    try {
                        switch (option) {
                            case "EX":
                                options.expiresAt = Math.addExact(System.currentTimeMillis(), Math.multiplyExact(number, 1000L));
                                break;
                            case "PX":
                                options.expiresAt = Math.addExact(System.currentTimeMillis(), number);
                                break;
                            case "EXAT":
                                options.expiresAt = Math.multiplyExact(number, 1000L);
                                break;
                            case "PXAT":
                                options.expiresAt = number;
                                break;
                        }
                    } catch (ArithmeticException e) {
                        throw new IOException("ERR invalid expire time");
                    }
                    expirationSpecified = true;
                    i += 2;
                    break;
                default:
                    throw new IOException("ERR syntax error");
            }
        }
        return options;
    }

    public static void executeSet(OutputStream output, ConcurrentHashMap<String, RedisValue> mp, String[] command) throws IOException {
        if (command.length < 3) {
            writeError(output, "ERR wrong number of arguments");
            return;
        }
        String key = command[1];
        String value = command[2];
        SetOptions options;
        try {
            options = parseSetOptions(command);
        } catch (IOException e) {
            writeError(output, e.getMessage());
            return;
        }

        synchronized (mp) {
            RedisValue oldEntry = getLiveEntry(mp, key);
            String oldValue = oldEntry == null ? null : oldEntry.getVal();

            if (options.nx && oldEntry != null) {
                if (options.get) writeBulkString(output, oldValue);
                else writeNullBulkString(output);
                return;
            }

            if (options.xx && oldEntry == null) {
                if (options.get) writeBulkString(output, null);
                else writeNullBulkString(output);
                return;
            }

            long newExpiresAt = options.expiresAt;
            if (options.keepTtl && oldEntry != null) {
                newExpiresAt = oldEntry.getExpiry();
            }

            mp.put(key, new RedisValue(value, newExpiresAt));

            if (options.get) {
                writeBulkString(output, oldValue);
            } else {
                writeSimpleString(output, "OK");
            }
        }
    }
}
