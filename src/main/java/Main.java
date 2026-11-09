import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

public class Main {

    public static void main(String[] args) {
        System.out.println("Logs from your program will appear here!");

        int port = 6379;

        // Shared database for all client connections
        ConcurrentHashMap<String, String> mp = new ConcurrentHashMap<>();

        try (ServerSocket serverSocket = new ServerSocket()) {

            // Set reuse address BEFORE binding
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(port));

            while (true) {
                Socket client = serverSocket.accept();

                Thread t = new Thread(() -> handleClient(client, mp));
                t.start();
            }

        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

    static void handleClient(
            Socket client,
            ConcurrentHashMap<String, String> mp) {

        try (client;
             InputStream input = new BufferedInputStream(client.getInputStream());
             OutputStream output = new BufferedOutputStream(client.getOutputStream())) {

            String[] command;

            while ((command = readRESPArray(input)) != null) {

                if (command.length == 0 || command[0] == null) {
                    writeError(output, "ERR empty command");
                    continue;
                }

                String curCommand = command[0];

                // PING
                if (curCommand.equalsIgnoreCase("PING")) {

                    if (command.length == 1) {
                        writeSimpleString(output, "PONG");

                    } else if (command.length == 2) {
                        writeBulkString(output, command[1]);

                    } else {
                        writeError(output, "ERR wrong number of arguments");
                    }

                    // ECHO
                } else if (curCommand.equalsIgnoreCase("ECHO")) {

                    if (command.length != 2) {
                        writeError(output, "ERR wrong number of arguments");
                        continue;
                    }

                    writeBulkString(output, command[1]);

                    // GET
                } else if (curCommand.equalsIgnoreCase("GET")) {

                    if (command.length != 2) {
                        writeError(output, "ERR wrong number of arguments");
                        continue;
                    }

                    String key = command[1];
                    String value = mp.get(key);

                    if (value != null) {
                        writeBulkString(output, value);
                    } else {
                        writeNullBulkString(output);
                    }

                    // SET
                } else if (curCommand.equalsIgnoreCase("SET")) {

                    if (command.length != 3) {
                        writeError(output, "ERR wrong number of arguments");
                        continue;
                    }

                    String key = command[1];
                    String value = command[2];

                    mp.put(key, value);
                    writeSimpleString(output, "OK");

                    // Unknown command
                } else {
                    writeError(output, "ERR unknown command");
                }
            }

        } catch (IOException e) {
            System.out.println(
                    "Client connection error: " + e.getMessage()
            );
        }
    }

    // ---------------- RESP WRITERS ----------------

    static void writeError(OutputStream output, String message) throws IOException {

        output.write(("-" + message + "\r\n").getBytes(StandardCharsets.UTF_8));
        output.flush();
    }

    static void writeSimpleString(OutputStream output, String value) throws IOException {

        output.write(("+" + value + "\r\n").getBytes(StandardCharsets.UTF_8));
        output.flush();
    }

    static void writeNullBulkString(OutputStream output) throws IOException {

        output.write("$-1\r\n".getBytes(StandardCharsets.UTF_8));
        output.flush();
    }

    static void writeBulkString(OutputStream output, String value) throws IOException {

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

    // ---------------- RESP READERS ----------------

    static String readLine(InputStream input) throws IOException {

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        while (true) {
            int b = input.read();

            if (b == -1) {
                throw new EOFException("Unexpected end of stream");
            }

            if (b == '\r') {
                int nextBit = input.read();

                if (nextBit != '\n') {
                    throw new IOException("Invalid CRLF");
                }

                return buffer.toString(StandardCharsets.UTF_8);
            }

            if (b == '\n') {
                throw new IOException("Invalid CRLF");
            }

            buffer.write(b);
        }
    }

    static byte[] readExactly(InputStream input, int length) throws IOException {

        if (length < 0) {
            throw new IOException("Negative length");
        }

        byte[] data = new byte[length];

        int offset = 0;

        while (offset < length) {

            int read = input.read(data, offset, length - offset);

            if (read == -1) {
                throw new EOFException("Incomplete bulk string");
            }

            if (read == 0) {
                continue;
            }

            offset += read;
        }

        return data;
    }

    static String readBulkString(InputStream input) throws IOException {

        int prefix = input.read();

        if (prefix == -1) {
            throw new EOFException("Unexpected end of stream");
        }

        if (prefix != '$') {
            throw new IOException("Expected bulk string");
        }

        int sizeOfData;

        try {
            sizeOfData = Integer.parseInt(readLine(input));
        } catch (NumberFormatException e) {
            throw new IOException("Invalid bulk string length", e);
        }

        if (sizeOfData == -1) {
            return null;
        }

        if (sizeOfData < 0) {
            throw new IOException("Invalid bulk string length");
        }

        byte[] data = readExactly(input, sizeOfData);

        int cr = input.read();
        int lf = input.read();

        if (cr != '\r' || lf != '\n') {
            throw new IOException("Invalid bulk string terminator");
        }

        return new String(data, StandardCharsets.UTF_8);
    }

    static String[] readRESPArray(InputStream input) throws IOException {

        // RESP Array:
        // *<number-of-elements>\r\n
        // followed by RESP elements.

        int prefix = input.read();

        if (prefix == -1) {
            return null; // Client disconnected cleanly
        }

        if (prefix != '*') {
            throw new IOException("Expected RESP array");
        }

        int commandsLength;

        try {
            commandsLength = Integer.parseInt(readLine(input));
        } catch (NumberFormatException e) {
            throw new IOException("Invalid RESP array length", e);
        }

        if (commandsLength < 0) {
            throw new IOException("Invalid RESP array length");
        }

        String[] commands = new String[commandsLength];

        for (int i = 0; i < commandsLength; i++) {
            commands[i] = readBulkString(input);
        }

        return commands;
    }
}
