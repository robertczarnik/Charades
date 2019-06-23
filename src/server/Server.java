package server;
// Java implementation of  Server side
// It contains two classes : Server and ClientHandler
// Save file as Server.java

import java.io.*;
import java.text.*;
import java.util.*;
import java.net.*;
import java.util.concurrent.Executors;

// Server class
public class Server {

    private static Set<DataOutputStream> clients = new HashSet<>();

    public static void main(String[] args) throws IOException {
        System.out.println("The chat server is running...");

        var pool = Executors.newFixedThreadPool(4);

        try (var listener = new ServerSocket(5001)) {
            while (true) {
                pool.execute(new ClientHandler(listener.accept()));
            }
        }
    }


    // ClientHandler class
    static class ClientHandler implements Runnable {
        DataInputStream in;
        DataOutputStream out;
        final Socket socket;


        // Constructor
        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                this.in = new DataInputStream(socket.getInputStream());
                this.out = new DataOutputStream(socket.getOutputStream());

                clients.add(out);

                while (true) {
                    String input = in.readUTF();

                    for (var client : clients) {
                        if(client!=out)
                            client.writeUTF(input);
                    }
                }
            } catch (IOException e) {
                System.out.println("BYE BYE");
            }finally {
                if (out != null) {
                    clients.remove(out);
                }

                try {
                    socket.close();
                }
                catch (IOException e) { }
            }

        }
    }
}