package robertczarnik.server;

import java.net.ServerSocket;

public class Main {
    public static void main(String[] args) throws Exception {
        new Server(new ServerSocket(5001),4).getConnections();
    }
}
