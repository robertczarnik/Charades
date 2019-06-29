package server;
// Java implementation of  Server side
// It contains two classes : Server and ClientHandler
// Save file as Server.java

import objects.ColorRGB;
import objects.Guess;
import objects.Message;
import objects.Point;

import java.io.*;
import java.util.*;
import java.net.*;
import java.util.concurrent.Executors;

// Server class
public class Server {

    private static Set<ObjectOutputStream> clients = new LinkedHashSet<>(); // set z kolejnoscia w jakies zostaly dodawane elementy
    private static String secretWord = "";


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
        String response;
        Guess msgResponse;


        // Constructor
        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        //TODO: dopiescic
        private String checkGuess( String guess){
            if(guess.equals(secretWord))
                return "OK";
            else {
                return guess;
            }

        }

        @Override
        public void run() {
            try {
                System.out.println("nowy client!");
                this.out = new ObjectOutputStream(socket.getOutputStream());
                this.in = new ObjectInputStream(socket.getInputStream());

                if(clients.isEmpty()){ //ustanowienie 1 klienta jako admina stolika
                    clients.add(out);
                    out.writeObject(new Message("ADMIN",true));
                }else{
                    clients.add(out);
                }


                while (true) {
                    Object input = in.readObject();

                    if(input instanceof Guess){
                        response = checkGuess(((Guess)input).getGuess());
                        if(response.equals("OK")){
                            msgResponse = new Guess(response);

                            for (var client : clients) {// przeslanie do wszystkich ze trafione
                                if (client != out) {
                                    client.writeObject(msgResponse);
                                    client.flush();
                                }
                            }

                            //przeniesienie zezwolenia na rysowanie na kolejnego gracza (iterator na secie)

                        }else{
                            msgResponse = new Guess(response);

                            for (var client : clients) {
                                if (client != out) {
                                    client.writeObject(msgResponse);
                                    client.flush();
                                }
                            }
                        }
                    }
                    else if(input instanceof Point){
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