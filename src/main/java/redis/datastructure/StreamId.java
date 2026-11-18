package redis.datastructure;

public record StreamId(long milliseconds, long sequence)
        implements Comparable<StreamId> {

    @Override
    public int compareTo(StreamId other) {
        int timeComparison = Long.compareUnsigned(this.milliseconds, other.milliseconds);

        if (timeComparison != 0) {
            return timeComparison;
        }

        return Long.compareUnsigned(
                this.sequence,
                other.sequence
        );
    }

    public static StreamId parse(String id) {
        String[] parts = id.split("-", -1);

        if (parts.length < 1 || parts.length > 2) {
            throw new IllegalArgumentException("Invalid stream ID");
        }

        long milliseconds = Long.parseUnsignedLong(parts[0]);
        long sequence = parts.length == 2
                ? Long.parseUnsignedLong(parts[1])
                : 0;

        return new StreamId(milliseconds, sequence);
    }

    @Override
    public String toString() {
        return Long.toUnsignedString(milliseconds)
                + "-"
                + Long.toUnsignedString(sequence);
    }
}