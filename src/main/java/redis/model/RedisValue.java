package redis.model;

public class RedisValue {
    private String val;
    private long expiry;

    public RedisValue(String val, long expiry) {
        this.val = val;
        this.expiry = expiry;
    }

    public RedisValue(String val) {
        this.val = val;
        this.expiry = 0;
    }

    public String getVal() { return this.val; }
    public long getExpiry() { return this.expiry; }
}
