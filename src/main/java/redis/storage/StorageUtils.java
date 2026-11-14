package redis.storage;

import redis.model.RedisValue;

import java.util.concurrent.ConcurrentHashMap;

public final class StorageUtils {
    private StorageUtils() {}

    public static RedisValue getLiveEntry(ConcurrentHashMap<String, RedisValue> mp, String key) {
        RedisValue entry = mp.get(key);
        if (entry == null) return null;
        if (entry.getExpiry() != 0 && System.currentTimeMillis() >= entry.getExpiry()) {
            mp.remove(key, entry);
            return null;
        }
        return entry;
    }
}
