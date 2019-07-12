package robertczarnik.client;

import java.io.*;
import java.net.*;


// Client class
public class Client
{

    private ObjectInputStream in;
    private ObjectOutputStream out;
    private Socket socket;

    public Client(String ip,int port)
    {
        try
        {
            socket = new Socket(ip, port);

            // obtaining input and out streams
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());


        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void closeConnection(){
        try {
            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ObjectInputStream getIn() {
        return in;
    }

    public ObjectOutputStream getOut() {
        return out;
    }

}