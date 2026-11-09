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
                        InputStream input = new BufferedInputStream(client.getInputStream());
                        OutputStream output = new BufferedOutputStream(client.getOutputStream());

                        String[] command;

                        while ((command = readRESPArray(input)) != null) {
//                            System.out.println("Received Message from: "+client.getRemoteSocketAddress()+" "+ message);
                            String curCommand = command[0];
                            if (curCommand.equalsIgnoreCase("PING")) {
                                output.write("+PONG\r\n".getBytes(StandardCharsets.UTF_8));
                                output.flush();
                            }else if(curCommand.equalsIgnoreCase("ECHO")){
                                if(command.length != 2){
                                    output.write("-ERR wrong number of arguments\r\n".getBytes(StandardCharsets.UTF_8));
                                    output.flush();
                                    continue;
                                }
                                writeBulkString(output, command[1]);
                            }
                            else {
                                output.write("-ERR unknown command\r\n".getBytes(
                                        StandardCharsets.UTF_8));
                            }
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
  static String readLine(InputStream input) throws IOException {
      ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      while (true){
          int b = input.read();
          if(b == -1){
              throw new EOFException("Unexpected end of stream");
          }
          if(b == '\r'){
              int nextBit = input.read();
              if(nextBit != '\n'){
                  throw new IOException("Invalid CRLF");
              }
              return buffer.toString(StandardCharsets.UTF_8);
          }
          buffer.write(b);
      }
  }

  static byte[] readExactly(InputStream input, int maxReadLength) throws IOException {
      if (maxReadLength < 0) {
          throw new IllegalArgumentException("Negative length");
      }

      byte[] data = new byte[maxReadLength];

      int offset = 0;

      while (offset < maxReadLength){
          int read = input.read(data, offset, maxReadLength - offset);

          if(read == -1){
              throw new EOFException("Incomplete Bulk Stream");
          }

          offset += read;

      }

      return data;
  }

  static String readBulkString(InputStream input) throws IOException {
    int prefix = input.read();
    if(prefix != '$'){
        throw new IOException("Expected bulk string");
    }
    int sizeOfData = Integer.parseInt(readLine(input));

    if(sizeOfData == -1){
        return null;
    }

    byte[] data = readExactly(input, sizeOfData);

    int cr = input.read();
    int lf = input.read();

    if (cr != '\r' || lf != '\n') {
      throw new IOException("Invalid bulk string terminator");
    }

    return new String(data, StandardCharsets.UTF_8);
  }

  static void writeBulkString(OutputStream out, String value) throws IOException {
        byte[] data = value.getBytes(StandardCharsets.UTF_8);
        String header = "$" + data.length + "\r\n";

        out.write(header.getBytes(StandardCharsets.UTF_8));
        out.write(data);
        out.write("\r\n".getBytes(StandardCharsets.UTF_8));
        out.flush();
  }

  static String[] readRESPArray(InputStream input) throws IOException {
      // * <LengthOfArray> \r\n $ <LengthOfData> \r\n data \r\n

      int prefix = input.read();

      if(prefix == -1) return null;

      if(prefix != '*'){
          throw new IOException("Expected RESP array");
      }

      int commandsLength = Integer.parseInt(readLine(input));

      String[] commands = new String[commandsLength];

      for(int i = 0; i < commandsLength; i++){
          commands[i] = readBulkString(input);
      }

      return commands;
  }
}
