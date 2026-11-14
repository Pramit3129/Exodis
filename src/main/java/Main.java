import redis.datastructure.List;
import redis.model.RedisValue;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static redis.server.ClientHandler.handleClient;

public class Main {
    public static void main(String[] args) {
        System.out.println("Logs from your program will appear here!");
        int port = 6379;
        ConcurrentHashMap<String, RedisValue> mp = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, List> listMap = new ConcurrentHashMap<>();
        ExecutorService threadPool = Executors.newCachedThreadPool();


        try (ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(port));

            while (true) {
                Socket client = serverSocket.accept();
                threadPool.submit(() -> handleClient(client, mp, listMap));
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }
}
