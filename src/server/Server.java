package server;


import collections.Pair;
import javafx.css.Match;
import objects.ColorRGB;
import objects.Guess;
import objects.Message;
import objects.Point;

import java.io.*;
import java.util.*;
import java.net.*;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Server class
public class Server {

    private static Set<Pair<ObjectOutputStream,String>> clients = new LinkedHashSet<>(); //set z kolejnoscia w jakies zostaly dodawane elementy
    private static List<Pair<String,Integer>> scoreboard = Collections.synchronizedList(new ArrayList<>());

    private static String[] words = {"Okoń","Rower","Okno","Arbuz","Kot"};

    private static String secretWord;
    private static int queueNumber=1;
    private static int time=10;
    private static String drawerName;

    private static long startTime;

    private static String regex = "\\d";
    private static Pattern pattern;


    private void compliePattern(){
        pattern = Pattern.compile(regex);
    }

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
        String msgType;
        String msg;
        String name;
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

        private void broadcast(Object obj,ObjectOutputStream out) throws IOException {
            for (var client : clients) {
                if (client.getFirst() != out) {
                    client.getFirst().writeObject(obj);
                    client.getFirst().flush();
                }
            }
        }

        private void broadcastAll(Object obj) throws IOException {
            for (var client : clients) {
                client.getFirst().writeObject(obj);
                client.getFirst().flush();
                client.getFirst().reset(); //reset cache bo zaszala zmiana w liscie scoreboard
            }
        }

        private void changeDrawer() throws IOException {
            Iterator<Pair<ObjectOutputStream,String>> iterator = clients.iterator();

            if(queueNumber>clients.size()) queueNumber=1;

            Random random = new Random();
            secretWord = words[random.nextInt(words.length)];


            int i=1;
            while(iterator.hasNext()) {
                Pair<ObjectOutputStream,String> setElement = iterator.next();
                if(i==queueNumber) {
                    ObjectOutputStream output = setElement.getFirst();
                    broadcast(new Message("DRAWER","false,"+" "+","+time),output);
                    output.writeObject(new Message("DRAWER","true,"+secretWord+","+time));
                    output.flush();
                    queueNumber++;
                    drawerName=setElement.getSecond();
                    break;
                }
                i++;
            }

            startTime = System.currentTimeMillis();
        }

        private void removePlayer(){
            if (out != null) {
                Iterator<Pair<ObjectOutputStream,String>> iterator = clients.iterator();

                while(iterator.hasNext()) {
                    Pair<ObjectOutputStream,String> setElement = iterator.next();
                    if(setElement.getFirst()==out) {
                        iterator.remove();
                        break;
                    }
                }
            }
        }

        @Override
        public void run() {
            try {
                System.out.println("nowy client!");
                this.out = new ObjectOutputStream(socket.getOutputStream());
                this.in = new ObjectInputStream(socket.getInputStream());

                if(clients.isEmpty()){ //ustanowienie 1 klienta jako admina stolika
                    clients.add(new Pair<>(out,""));
                    out.writeObject(new Message("ADMIN","true"));
                }else{
                    clients.add(new Pair<>(out,""));
                }


                while (true) {
                    Object input = in.readObject();

                    if(input instanceof Guess){
                        String guess = ((Guess)input).getGuess();
                        response = checkGuess(guess);
                        msgResponse = new Guess(name+ ": " + guess); // guess with player name
                        broadcast(msgResponse,out);

                        if(response.equals("OK")) {

                            synchronized (scoreboard) {
                                Iterator<Pair<String, Integer>> scoreboardIterator = scoreboard.iterator();
                                while (scoreboardIterator.hasNext()){
                                    Pair<String,Integer> element = scoreboardIterator.next();
                                    if(element.getFirst().equals(name)){ // ten co zgadl
                                        int points = (int)(40*(time - (System.currentTimeMillis()-startTime)/1000)/time); // ((time-elapsed)/time) * points
                                        element.setSecond(element.getSecond()+points);
                                    }else if(element.getFirst().equals(drawerName)){ // ten co rysowal
                                        int points = (int)(50*(time - (System.currentTimeMillis()-startTime)/1000)/time);
                                        element.setSecond(element.getSecond()+points);
                                    }
                                }
                            }


                            // sortowanie zeby gracz z najwyzszym wynikiem byl u gory
                            scoreboard.sort((Pair<String,Integer> ele1,Pair<String,Integer> ele2) -> ele2.getSecond()-ele1.getSecond());

                            broadcastAll(scoreboard); //przeslanie zaktualizowanej tabeli


                            changeDrawer();
                            broadcastAll(new Guess("BRAWO! : " + guess));
                        }
                    }
                    else if(input instanceof Point || input instanceof ColorRGB) {
                        broadcast(input,out);
                    }else if(input instanceof Message){
                        msgType=((Message)input).getMessageType();
                        msg=((Message)input).getMessage();

                        if(msgType.equals("NAME")){
                            scoreboard .add(new Pair<>(msg,0)); // synchronized call

                            Iterator<Pair<ObjectOutputStream,String>> iterator = clients.iterator();

                            while(iterator.hasNext()) {
                                Pair<ObjectOutputStream,String> setElement = iterator.next();
                                if(setElement.getFirst()==out) {
                                    setElement.setSecond(msg);
                                    name=msg;
                                    break;
                                }
                            }

                            Iterator<Pair<String,Integer>> iterators = scoreboard .iterator();

                            while(iterators.hasNext()){
                                Pair<String,Integer> ele = iterators.next();
                                System.out.println(ele.getFirst());
                            }

                            broadcastAll(scoreboard);


                        }else if(msgType.equals("START")){ //game start || time over
                            changeDrawer();
                        }else if(msgType.equals("EXIT")){
                            break;
                        }
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("BYE BYE ");
            }finally {
                removePlayer();

                try {
                    socket.close();
                }
                catch (IOException e) { }
            }

        }
    }
}