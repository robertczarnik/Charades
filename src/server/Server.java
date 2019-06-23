package server;
// Java implementation of  Server side
// It contains two classes : Server and ClientHandler
// Save file as Server.java

import sample.ColorRGB;
import sample.Point;

import java.io.*;
import java.text.*;
import java.util.*;
import java.net.*;
import java.util.concurrent.Executors;

// Server class
public class Server {

    private static Set<ObjectOutputStream> clients = new HashSet<>();

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
        ObjectInputStream in;
        ObjectOutputStream out;
        final Socket socket;


        // Constructor
        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                System.out.println("nowy client!");
                this.out = new ObjectOutputStream(socket.getOutputStream());
                this.in = new ObjectInputStream(socket.getInputStream());

                clients.add(out);

                while (true) {
                    Object input = in.readObject();

                    if(input instanceof Point){
                        for (var client : clients) {
                            if (client != out){
                                client.writeObject(input);
                                client.flush();
                            }
                        }
                    }else if(input instanceof ColorRGB){
                        for (var client : clients) {
                            if (client != out){
                                client.writeObject(input);
                                client.flush();
                            }
                        }
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("BYE BYE ");
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