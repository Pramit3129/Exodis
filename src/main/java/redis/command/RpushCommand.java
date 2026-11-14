package redis.command;

import redis.datastructure.List;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ConcurrentHashMap;

import static redis.protocol.RespWriter.*;

public final class RpushCommand {
    private RpushCommand() {}

    public static void executeRpush(OutputStream output, String[] command,ConcurrentHashMap<String, List> listMap) throws IOException {
        if(command.length <  3){
            writeError(output, "ERR wrong number of arguments");
            return;
        }
        String listName = command[1];
        String element = command[2];
        int op;
        synchronized (listMap){
            if(listMap.containsKey(listName)){
                List existingList = listMap.get(listName);
                op = existingList.push(element);
            }else{
                // create a new list first
                List newList = new List();
                listMap.put(listName, newList);
                op = newList.push(element);
            }
        }
        writeInteger(output, op);

    }
}
