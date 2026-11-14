package redis.protocol;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public final class RespWriter {
    private RespWriter() {}

    public static void writeError(OutputStream output, String message) throws IOException {
        output.write(("-" + message + "\r\n").getBytes(StandardCharsets.UTF_8));
        output.flush();
    }


    public static void writeInteger(OutputStream output, int val) throws IOException {
        output.write((":" + val + "\r\n").getBytes(StandardCharsets.UTF_8));
        output.flush();
    }


    public static void writeSimpleString(OutputStream output, String value) throws IOException {
        output.write(("+" + value + "\r\n").getBytes(StandardCharsets.UTF_8));
        output.flush();
    }

    public static void writeNullBulkString(OutputStream output) throws IOException {
        output.write("$-1\r\n".getBytes(StandardCharsets.UTF_8));
        output.flush();
    }

    public static void writeBulkString(OutputStream output, String value) throws IOException {
        if (value == null) {
            writeNullBulkString(output);
            return;
        }
        byte[] data = value.getBytes(StandardCharsets.UTF_8);
        String header = "$" + data.length + "\r\n";
        output.write(header.getBytes(StandardCharsets.UTF_8));
        output.write(data);
        output.write("\r\n".getBytes(StandardCharsets.UTF_8));
        output.flush();
    }
}
