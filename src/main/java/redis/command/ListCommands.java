package redis.command;

import redis.datastructure.RedisList;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

import static redis.protocol.RespWriter.*;

public final class ListCommands {
    private ListCommands() {}

    public static void executeRpush( OutputStream output, String[] command, ConcurrentHashMap<String, RedisList> listMap) throws IOException {

        if(command.length < 3){
            writeError(output, "ERR wrong number of arguments");
            return;
        }

        String listName = command[1];

        RedisList redisList = listMap.computeIfAbsent(
                listName,
                key -> new RedisList()
        );

        int len = redisList.rpush(Arrays.copyOfRange(command, 2, command.length));

        writeInteger(output, len);
    }

    public static void executeLRANGE( OutputStream output, String[] command, ConcurrentHashMap<String, RedisList> listMap) throws IOException {

        if(command.length != 4){
            writeError(output, "ERR wrong number of arguments");
            return;
        }

        String listName = command[1];

        int start = Integer.parseInt(command[2]);
        int stop = Integer.parseInt(command[3]);

        RedisList redisList = listMap.get(listName);

        if(redisList == null){
            writeRESPArray(output, new String[]{});
            return;
        }

        writeRESPArray(output, redisList.getEle(start, stop));
    }

    public static void executeLpush(OutputStream output, String[] command, ConcurrentHashMap<String, RedisList> listMap) throws IOException {

        if(command.length < 3){
            writeError(output, "ERR wrong number of arguments");
            return;
        }

        String listName = command[1];

        RedisList redisList = listMap.computeIfAbsent(
                listName,
                key -> new RedisList()
        );

        int len = redisList.lpush(Arrays.copyOfRange(command, 2, command.length));

        writeInteger(output, len);
    }

    public static void executeLLEN( OutputStream output, String[] command, ConcurrentHashMap<String, RedisList> listMap) throws IOException {

        if(command.length != 2){
            writeError(output, "ERR wrong number of arguments");
            return;
        }

        RedisList redisList = listMap.get(command[1]);

        int len = redisList != null ? redisList.length() : 0;

        writeInteger(output, len);
    }

    public static void executeLPOP(OutputStream output, String[] command, ConcurrentHashMap<String, RedisList> listMap) throws IOException {
        if(command.length < 2){
            writeError(output, "ERR wrong number of arguments");
            return;
        }
        String listName = command[1];

        RedisList redisList = listMap.get(listName);
        if(redisList == null){
            writeNullBulkString(output);
            return;
        }
       if(command.length == 3){
           int howManyPop = Integer.parseInt(command[2]);
           String[] popItems = new String[howManyPop];
           for(int i = 0 ; i < howManyPop;  i++){
               String value = redisList.Lpop();
               popItems[i] = value;
           }
           writeRESPArray(output, popItems);
       }else{
           String value = redisList.Lpop();

           if(value == null){
               writeNullBulkString(output);
           }else{
               writeBulkString(output, value);
           }
       }
    }
}
