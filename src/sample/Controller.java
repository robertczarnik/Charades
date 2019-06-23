package sample;

import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ColorPicker;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import server.Client;
import java.io.IOException;



public class Controller implements Runnable{
    @FXML
    private Canvas canvas;

    @FXML
    private ColorPicker colorPicker;


    private Client client ;
    private GraphicsContext g;

    private double prevPosX=-1;
    private double prevPosY=-1;

    private double x;
    private double y;

    private double size = 7;
    private Color color = Color.BLACK;


    public void initialize(){
        colorPicker.setValue(Color.BLACK);
        g = canvas.getGraphicsContext2D();
    }

    @FXML
    private void onColorPicker(){
        color=colorPicker.getValue();
    }

    public void initClient(Client client) { //tylko raz mozna zainicjalizowac clienta
        if (this.client != null) {
            throw new IllegalStateException("Client can only be initialized once");
        }
        this.client = client ;
    }

    private void drawLine(double x, double y){
        g.setStroke(color);
        g.setLineWidth(size);
        g.setLineCap(StrokeLineCap.ROUND);
        //g.moveTo(prevPosX,prevPosY);
        //g.lineTo(x,y);
        //g.stroke();
        g.strokeLine(prevPosX,prevPosY,x,y);
        prevPosX=x;
        prevPosY=y;
    }

    private void drawPoint(double x, double y){
        x = x - size/2;
        y = y - size/2;

        g.setFill(color);
        g.fillOval(x,y,size,size);
    }

    @FXML
    private void onMouseDragged(MouseEvent event){ // draw line between prev pos and actual pos
        x = event.getX();
        y = event.getY();
        drawLine(x,y);

        try {
            client.getDos().writeUTF("l " + x + " " + y + " " + size); //line
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onMousePressed(MouseEvent event) { // draw a oval point and set previous postions of x and y
        prevPosX=event.getX();
        prevPosY=event.getY();
        drawPoint(prevPosX,prevPosY);

        try {
            client.getDos().writeUTF("p " + prevPosX + " " + prevPosY + " " + size); //prefix p means it is a point
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        // the following loop performs the exchange of
        // information between client and client handler

        String point;
        String prefix;
        String[] attributes;

        while (true)
        {
            try {

                point = client.getDis().readUTF(); //get point
                attributes = point.split(" ");
                prefix=attributes[0];
                size=Double.parseDouble(attributes[3]);

                if(prefix.equals("l")){
                    x=Double.parseDouble(attributes[1]);
                    y=Double.parseDouble(attributes[2]);
                    drawLine(x,y);
                }else{
                    prevPosX=Double.parseDouble(attributes[1]);;
                    prevPosY=Double.parseDouble(attributes[2]);;
                    drawPoint(prevPosX,prevPosY);
                }

            } catch(IOException e) {
                e.printStackTrace();
                System.out.println("BYE");
            }
        }
    }
}
