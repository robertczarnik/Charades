package server;

import java.io.*;
import java.net.*;


// Client class
public class Client
{

    private DataInputStream dis;
    private DataOutputStream dos;
    private Socket socket;

    public Client(String ip,int port)
    {
        try
        {
            socket = new Socket(ip, port);

            // obtaining input and out streams
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());


        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void closeConnection(){
        try {
            dis.close();
            dos.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public DataInputStream getDis() {
        return dis;
    }

    public DataOutputStream getDos() {
        return dos;
    }

}