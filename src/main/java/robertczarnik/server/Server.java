package robertczarnik.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import robertczarnik.collections.Pair;
import robertczarnik.objects.ColorRGB;
import robertczarnik.objects.Guess;
import robertczarnik.objects.Message;
import robertczarnik.objects.Point;


public class Server {
    //locks
    private final Object clientsLock = new Object();
    private final Object scoreboardLock = new Object();
    private final Object lock = new Object();

    //server variables
    private ServerSocket serverSocket;
    private int numberOfThreads;

    //collections
    private Set<ClientHandler> clients = Collections.synchronizedSet(new LinkedHashSet<>());
    private List<Pair<String,Integer>> scoreboard = Collections.synchronizedList(new ArrayList<>());

    //game variables
    private Words words = new Words();
    private String actualWord;
    private String actualWordNormalized;
    private Pattern pattern;
    private int queueNumber=1;
    private boolean gamestarted=false;

    //actual important roles
    private String drawerName;
    private ClientHandler admin;

    //round time
    private final int time=100;
    private long startTime;

    public Server(ServerSocket serverSocket, int numberOfThreads){
        this.serverSocket = serverSocket;
        this.numberOfThreads = numberOfThreads;
    }

    /**
     * The usual loop of accepting connections and firing off new threads to handle them in thread pool
     */
    public void getConnections() throws IOException {
        var pool = Executors.newFixedThreadPool(numberOfThreads);

        while (true) {
            ClientHandler client = new ClientHandler(serverSocket.accept(),this);
            clients.add(client);
            pool.execute(client);
        }
    }

    /**
     * make proper regex and copile it to use it later
     */
    private void compliePattern(){
        actualWordNormalized = actualWord;
        actualWordNormalized = actualWordNormalized.toLowerCase();
        actualWordNormalized = StringUtils.stripAccents(actualWordNormalized); // eliminate polish diacritics

        StringBuilder strBuilder = new StringBuilder();
        String [] words = actualWordNormalized .split(" ");
        for(String word : words){ // words under 3 letters - skip , words between 3-4 letters - match all, words above 4 letters - match first 3 letters
            if(word.length()>2){
                if(word.length()<5){
                    strBuilder.append(".*").append(word).append(".*|");
                }else{
                    strBuilder.append(".*").append(word, 0, 3).append(".*|");
                }
            }
        }

        pattern = Pattern.compile(strBuilder.substring(0, strBuilder.length() - 1));
    }

    private void broadcast(Object obj,ObjectOutputStream out) throws IOException {
        for (var client : clients) {
            if (client.out != out) {
                client.out.writeObject(obj);
                client.out.flush();
            }
        }
    }

    private void broadcastAll(Object obj) throws IOException {
        for (var client : clients) {
            client.out.writeObject(obj);
            client.out.flush();
            client.out.reset(); //reset cache because scoreboard was changed
        }
    }

    /**
     * get a new word and grant drawing permission to next player
     * @throws IOException when writeObject or flush fails
     */
    private void changeDrawer() throws IOException {
        Iterator<ClientHandler> iterator = clients.iterator();

        if(queueNumber>clients.size()) queueNumber=1;

        // get a word
        actualWord = words.getRandomWord();

        compliePattern();

        int i=1;

        synchronized(clientsLock) {
            while (iterator.hasNext()) {
                ClientHandler setElement = iterator.next();
                if (i == queueNumber) {
                    ObjectOutputStream output = setElement.out;
                    broadcast(new Message("DRAWER", "false," + " " + "," + time), output);
                    output.writeObject(new Message("DRAWER", "true," + actualWord + "," + time));
                    output.flush();
                    queueNumber++;
                    drawerName = setElement.name;
                    break;
                }
                i++;
            }
        }

        startTime = System.currentTimeMillis(); // time when round starts
    }


    public class ClientHandler implements Runnable{

        //references
        private final Server server;
        private final Socket clientSocket;

        //streams
        private final ObjectInputStream in;
        private final ObjectOutputStream out;

        //player
        private String name;

        //guess
        private String response;
        private Guess msgResponse;
        private Matcher matcher;

        //message
        private String msg;
        private String msgType;


        private ClientHandler(Socket clientSocket, Server server) throws IOException {
            this.clientSocket = clientSocket;
            this.server = server;
            this.out = new ObjectOutputStream(this.clientSocket.getOutputStream());
            this.in = new ObjectInputStream(this.clientSocket.getInputStream());
        }

        /**
         * remove this instance of ClientHandler from clients and scoreboard
         */
        private void removePlayer() throws IOException {
            if (out != null) {
                Iterator<Pair<String,Integer>> iteratorScoreboard = scoreboard.iterator();
                synchronized(scoreboardLock){
                    while(iteratorScoreboard.hasNext()){
                        Pair<String,Integer> setElement = iteratorScoreboard.next();
                        if(setElement.getFirst().equals(name)){
                            iteratorScoreboard .remove();
                            break;
                        }
                    }
                }

                Iterator<ClientHandler> iterator = clients.iterator();

                synchronized(clientsLock) {
                    while (iterator.hasNext()) {
                        ClientHandler setElement = iterator.next();
                        if (setElement.out == out) {
                            iterator.remove();
                            changeDrawer();

                            if(setElement==admin){
                                if(iterator.hasNext()) {
                                    admin=iterator.next();
                                    admin.out.writeObject(new Message("ADMIN","true,"+gamestarted));
                                }
                                else {
                                    admin=null;
                                    gamestarted=false;
                                }
                            }

                            break;
                        }
                    }
                }

                broadcastAll(scoreboard);
            }

        }

        /**
         * guess comparison to actualWord and regex
         * @param guess
         * @return
         */
        private String checkGuess( String guess){
            guess = StringUtils.stripAccents(guess.toLowerCase());

            if(guess.equals(actualWordNormalized))
                return "OK";
            else{
                matcher = pattern.matcher(guess);
                if(matcher.matches()){
                    return "CLOSE";
                }
            }
            return "";
        }

        /**
         * Send guess to all player, if its a good answer points are spread among players,
         * scoreboard is updated and new drawer is chosen.
         * In some cases additional message is sent
         * @param guess player guess
         * @throws IOException stream fails
         */
        private void guessMenagment(String guess) throws IOException {
            synchronized(lock) { // each quess is separately checked  (not parallel)
                response = checkGuess(guess);
                msgResponse = new Guess(name + ": " + guess); // guess with player name
                server.broadcast(msgResponse, out);

                if(response.equals("CLOSE")){ //close guess
                    broadcastAll(new Guess("BLISKO! : " + guess));
                }
                else if (response.equals("OK")) { //points and scoreboard actualization
                    synchronized (scoreboardLock) {
                        Iterator<Pair<String, Integer>> scoreboardIterator = scoreboard.iterator();
                        while (scoreboardIterator.hasNext()) {
                            Pair<String, Integer> element = scoreboardIterator.next();
                            if (element.getFirst().equals(name)) { // the one who guessed
                                int points = (int) (40 * (time - (System.currentTimeMillis() - startTime) / 1000) / time); // ((time-elapsed)/time) * points
                                element.setSecond(element.getSecond() + points);
                            } else if (element.getFirst().equals(drawerName)) { // the one who drawed
                                int points = (int) (50 * (time - (System.currentTimeMillis() - startTime) / 1000) / time);
                                element.setSecond(element.getSecond() + points);
                            }
                        }
                    }


                    // sorting, player with highest score is on top
                    scoreboard.sort((Pair<String, Integer> ele1, Pair<String, Integer> ele2) -> ele2.getSecond() - ele1.getSecond());

                    //server.broadcast updated scoreboard
                    broadcastAll(scoreboard);

                    broadcastAll(new Guess("BRAWO! : " + guess));
                    server.changeDrawer();
                }
            }
        }

        /**
         * depending on msgType do some actions
         * @param msg
         * @param msgType
         * @throws IOException stream fails
         */
        private void messageMenagment(String msg,String msgType) throws IOException {
            if(msgType.equals("NAME")){
                scoreboard.add(new Pair<>(msg,0)); // synchronized call
                name=msg;
                broadcastAll(scoreboard);
            }else if(msgType.equals("START")){ //game start || time over
                gamestarted=true;
                server.changeDrawer();
            }
        }

        /**
         * main loop to get requests from clients and process them
         */
        @Override
        public void run() {
            try {
                //first client is admin
                synchronized(lock) {
                    if(admin == null) {
                        out.writeObject(new Message("ADMIN", "true,"+gamestarted));
                        admin = this;
                    }
                }

                while (true) {
                    Object input = in.readObject();

                    if(input instanceof Guess){
                        String guess = ((Guess)input).getGuess();
                        guessMenagment(guess);
                    }
                    else if(input instanceof Point || input instanceof ColorRGB) {
                        server.broadcast(input,out);
                    }
                    else if(input instanceof Message){
                        msg=((Message)input).getMessage();
                        msgType=((Message)input).getMessageType();

                        if(msgType.equals("CLEAR")){
                            broadcast(input,out);
                            continue;
                        }
                        else if(msgType.equals("EXIT")){
                            break;
                        }

                        messageMenagment(msg,msgType);
                    }
                }

                
            } catch (IOException | ClassNotFoundException e) {

            }finally {
                try {
                    removePlayer();
                    clientSocket.close();
                }
                catch (IOException e) {

                }
            }
        }
    }
}