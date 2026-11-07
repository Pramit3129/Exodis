import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Main {
  public static void main(String[] args){
    // You can use print statements as follows for debugging, they'll be visible when running tests.
    System.out.println("Logs from your program will appear here!");

    //  Uncomment the code below to pass the first stage
        ServerSocket serverSocket = null;
        Socket clientSocket = null;
        int port = 6379;
        try {
          serverSocket = new ServerSocket(port);
          // Since the tester restarts your program quite often, setting SO_REUSEADDR
          // ensures that we don't run into 'Address already in use' errors
          serverSocket.setReuseAddress(true);

            while (true) {
                Socket client = serverSocket.accept();

//                System.out.println("New Client Connected: "
//                        + client.getRemoteSocketAddress());

                Thread t = new Thread(() -> {
                    try (client) {
                        BufferedReader input = new BufferedReader(
                                new InputStreamReader(
                                        client.getInputStream(),
                                        StandardCharsets.UTF_8
                                )
                        );

                        BufferedOutputStream output =
                                new BufferedOutputStream(
                                        client.getOutputStream()
                                );

                        String message;

                        while ((message = input.readLine()) != null) {
//                            System.out.println("Received Message from: "+client.getRemoteSocketAddress()+" "+ message);

                            if (message.equals("PING")) {
                                output.write("+PONG\r\n".getBytes(
                                        StandardCharsets.UTF_8));
                            }
//                            else {
//                                output.write("-ERR unknown command\r\n".getBytes(
//                                        StandardCharsets.UTF_8));
//                            }

                            output.flush();
                        }
//                        System.out.println("Client Disconnected: " + client.getRemoteSocketAddress());
                    } catch (Exception e) {
//                        System.out.println(
//                                "Client Disconnected (connection error): "
//                                        + client.getRemoteSocketAddress()
//                        );
                        e.printStackTrace();
                    }
                });

                t.start();
            }

        } catch (IOException e) {
          System.out.println("IOException: " + e.getMessage());
        } finally {
          try {
            if (clientSocket != null) {
              clientSocket.close();
            }
          } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
          }
        }
  }
}
