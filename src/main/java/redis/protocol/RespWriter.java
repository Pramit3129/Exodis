package redis.protocol;

import redis.datastructure.StreamEntry;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class RespWriter {
    private RespWriter() {}

    public static void writeError(OutputStream output, String message) throws IOException {
        output.write(("-" + message + "\r\n").getBytes(StandardCharsets.UTF_8));
        output.flush();
//        String response = "-ERR " + message + "\r\n";
//
//        output.write(
//                response.getBytes(StandardCharsets.UTF_8)
//        );
    }
//    public static void writeError(
//            OutputStream output,
//            String message
//    ) throws IOException {
//
//        String response = "-ERR " + message + "\r\n";
//
//        output.write(
//                response.getBytes(StandardCharsets.UTF_8)
//        );
//
//        output.flush();
//    }


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


    public static void writeNullArray(OutputStream output)
            throws IOException {

        output.write(
                "*-1\r\n".getBytes(StandardCharsets.UTF_8)
        );

        output.flush();
    }

    public static void writeRESPArray(OutputStream output, String[] elements) throws IOException {
        output.write(("*" + elements.length + "\r\n").getBytes(StandardCharsets.UTF_8));

        for (String element : elements) {
            writeBulkString(output, element);
        }

        output.flush();
    }

    private static void writeAscii(
            OutputStream output,
            String value
    ) throws IOException {
        output.write(value.getBytes(StandardCharsets.US_ASCII));
    }

    public static void writeArrayHeader(
            OutputStream output,
            int count
    ) throws IOException {
        writeAscii(output, "*" + count + "\r\n");
    }

    public static void writeStreamEntries(
            OutputStream output,
            List<StreamEntry> entries
    ) throws IOException {

        writeArrayHeader(output, entries.size());

        for (StreamEntry entry : entries) {
            writeArrayHeader(output, 2);

            writeBulkString(output, entry.id().toString());

            List<String> fields = entry.fieldsAndValues();

            writeArrayHeader(output, fields.size());

            for (String field : fields) {
                writeBulkString(output, field);
            }
        }
    }

}
