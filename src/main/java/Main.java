import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    public static class RedisValue {
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

    static class SetOptions {
        boolean nx = false;
        boolean xx = false;
        boolean get = false;
        boolean keepTtl = false;
        long expiresAt = 0;
    }

    public static void main(String[] args) {
        System.out.println("Logs from your program will appear here!");
        int port = 6379;
        ConcurrentHashMap<String, RedisValue> mp = new ConcurrentHashMap<>();
        ExecutorService threadPool = Executors.newCachedThreadPool();

        try (ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(port));

            while (true) {
                Socket client = serverSocket.accept();
                threadPool.submit(() -> handleClient(client, mp));
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

    static void handleClient(Socket client, ConcurrentHashMap<String, RedisValue> mp) {
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
                if (curCommand.equalsIgnoreCase("PING")) {
                    if (command.length == 1) {
                        writeSimpleString(output, "PONG");
                    } else if (command.length == 2) {
                        writeBulkString(output, command[1]);
                    } else {
                        writeError(output, "ERR wrong number of arguments");
                    }
                } else if (curCommand.equalsIgnoreCase("ECHO")) {
                    if (command.length != 2) {
                        writeError(output, "ERR wrong number of arguments");
                        continue;
                    }
                    writeBulkString(output, command[1]);
                } else if (curCommand.equalsIgnoreCase("GET")) {
                    if (command.length != 2) {
                        writeError(output, "ERR wrong number of arguments");
                        continue;
                    }
                    String key = command[1];
                    synchronized (mp) {
                        RedisValue value = getLiveEntry(mp, key);
                        if (value != null) {
                            writeBulkString(output, value.getVal());
                        } else {
                            writeNullBulkString(output);
                        }
                    }
                } else if (curCommand.equalsIgnoreCase("SET")) {
                    executeSet(output, mp, command);
                } else {
                    writeError(output, "ERR unknown command");
                }
            }
        } catch (IOException e) {
            System.out.println("Client connection error: " + e.getMessage());
        }
    }

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

    static String readLine(InputStream input) throws IOException {
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

    static byte[] readExactly(InputStream input, int length) throws IOException {
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

    static String readBulkString(InputStream input) throws IOException {
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

    static String[] readRESPArray(InputStream input) throws IOException {
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

    static RedisValue getLiveEntry(ConcurrentHashMap<String, RedisValue> mp, String key) {
        RedisValue entry = mp.get(key);
        if (entry == null) return null;
        if (entry.getExpiry() != 0 && System.currentTimeMillis() >= entry.getExpiry()) {
            mp.remove(key, entry);
            return null;
        }
        return entry;
    }

    static SetOptions parseSetOptions(String[] command) throws IOException {
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

    static void executeSet(OutputStream output, ConcurrentHashMap<String, RedisValue> mp, String[] command) throws IOException {
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