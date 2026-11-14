package redis.command;

import redis.datastructure.List;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ConcurrentHashMap;

import static redis.protocol.RespWriter.*;

public final class ListCommands {
    private ListCommands() {}

    public static void executeRpush(OutputStream output, String[] command,ConcurrentHashMap<String, List> listMap) throws IOException {
        if(command.length <  3){
            writeError(output, "ERR wrong number of arguments");
            return;
        }
        String listName = command[1];
        // int totalElements = command.length - 2;
        int op = 0;
        synchronized (listMap){
            if(listMap.containsKey(listName)){
                List existingList = listMap.get(listName);
                for(int i = 2; i < command.length; i++){
                    String element = command[i];
                    op = existingList.push(element);
                }
            }else{
                // create a new list first
                List newList = new List();
                listMap.put(listName, newList);
                for(int i = 2; i < command.length; i++){
                    String element = command[i];
                    op = newList.push(element);
                }
            }
        }
        writeInteger(output, op);
    }

    public static void executeLRANGE(OutputStream output, String[] command, ConcurrentHashMap<String, List> listMap) throws IOException {
        if(command.length !=  4){
            writeError(output, "ERR wrong number of arguments");
            return;
        }
        String listName = command[1];
        int start = Integer.parseInt(command[2]);
        int stop = Integer.parseInt(command[3]);
        List list = listMap.get(listName);
        if(list == null){
            writeRESPArray(output, new String[]{});
        }
        writeRESPArray(output, list.getEle(start, stop));
    }

    public static void executeLpush(OutputStream output, String[] command,ConcurrentHashMap<String, List> listMap) throws IOException {
        if(command.length <  3){
            writeError(output, "ERR wrong number of arguments");
            return;
        }
        String listName = command[1];
        // int totalElements = command.length - 2;
        int op = 0;
        synchronized (listMap){
            if(listMap.containsKey(listName)){
                List existingList = listMap.get(listName);
                for(int i = 2; i < command.length; i++){
                    String element = command[i];
                    op = existingList.enque(element);
                }
            }else{
                // create a new list first
                List newList = new List();
                listMap.put(listName, newList);
                for(int i = 2; i < command.length; i++){
                    String element = command[i];
                    op = newList.enque(element);
                }
            }
        }
        writeInteger(output, op);
    }

    public static void executeLLEN(OutputStream output, String[] command, ConcurrentHashMap<String, List> listMap) throws IOException {
        if(command.length != 2){
            writeError(output, "ERR wrong number of arguments");
            return;
        }
        String listName = command[1];
        List list = listMap.get(listName);
        int len = list != null ? list.len : 0;
        writeInteger(output, len);
    }
}
