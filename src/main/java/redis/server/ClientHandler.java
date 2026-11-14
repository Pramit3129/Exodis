package redis.server;

import redis.datastructure.List;
import redis.model.RedisValue;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

import static redis.command.RpushCommand.executeRpush;
import static redis.command.SetCommand.executeSet;
import static redis.protocol.RespReader.readRESPArray;
import static redis.protocol.RespWriter.*;
import static redis.storage.StorageUtils.getLiveEntry;

public final class ClientHandler {
    private ClientHandler() {}

    public static void handleClient(Socket client, ConcurrentHashMap<String, RedisValue> mp, ConcurrentHashMap<String, List> listMap) {
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
                } else if(curCommand.equalsIgnoreCase("RPUSH")){
                     executeRpush(output, command, listMap);
                }else {
                    writeError(output, "ERR unknown command");
                }
            }
        } catch (IOException e) {
            System.out.println("Client connection error: " + e.getMessage());
        }
    }
}
