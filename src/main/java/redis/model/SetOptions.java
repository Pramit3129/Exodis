package redis.model;

public class SetOptions {
    public boolean nx = false;
    public boolean xx = false;
    public boolean get = false;
    public boolean keepTtl = false;
    public long expiresAt = 0;
}
