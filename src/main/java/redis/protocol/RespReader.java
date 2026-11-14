package redis.protocol;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class RespReader {
    private RespReader() {}

    public static String readLine(InputStream input) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        while (true) {
            int b = input.read();
            if (b == -1) throw new EOFException("Unexpected end of stream");
            if (b == '\r') {
                int nextBit = input.read();
                if (nextBit != '\n') throw new IOException("Invalid CRLF");
                return buffer.toString(StandardCharsets.UTF_8);
            }
            if (b == '\n') throw new IOException("Invalid CRLF");
            buffer.write(b);
        }
    }

    public static byte[] readExactly(InputStream input, int length) throws IOException {
        if (length < 0) throw new IOException("Negative length");
        byte[] data = new byte[length];
        int offset = 0;
        while (offset < length) {
            int read = input.read(data, offset, length - offset);
            if (read == -1) throw new EOFException("Incomplete bulk string");
            if (read == 0) continue;
            offset += read;
        }
        return data;
    }

    public static String readBulkString(InputStream input) throws IOException {
        int prefix = input.read();
        if (prefix == -1) throw new EOFException("Unexpected end of stream");
        if (prefix != '$') throw new IOException("Expected bulk string");
        int sizeOfData;
        try {
            sizeOfData = Integer.parseInt(readLine(input));
        } catch (NumberFormatException e) {
            throw new IOException("Invalid bulk string length", e);
        }
        if (sizeOfData == -1) return null;
        if (sizeOfData < 0) throw new IOException("Invalid bulk string length");
        byte[] data = readExactly(input, sizeOfData);
        int cr = input.read();
        int lf = input.read();
        if (cr != '\r' || lf != '\n') throw new IOException("Invalid bulk string terminator");
        return new String(data, StandardCharsets.UTF_8);
    }

    public static String[] readRESPArray(InputStream input) throws IOException {
        int prefix = input.read();
        if (prefix == -1) return null;
        if (prefix != '*') throw new IOException("Expected RESP array");
        int commandsLength;
        try {
            commandsLength = Integer.parseInt(readLine(input));
        } catch (NumberFormatException e) {
            throw new IOException("Invalid RESP array length", e);
        }
        if (commandsLength < 0) throw new IOException("Invalid RESP array length");
        String[] commands = new String[commandsLength];
        for (int i = 0; i < commandsLength; i++) {
            commands[i] = readBulkString(input);
        }
        return commands;
    }
}
