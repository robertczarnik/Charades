package sample;

import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import objects.ColorRGB;
import objects.Guess;
import objects.Message;
import objects.Point;
import server.Client;
import java.io.IOException;



public class Controller implements Runnable{
    @FXML
    private Canvas canvas;

    @FXML
    private ColorPicker colorPicker;

    @FXML
    private Slider slider;

    @FXML
    private Label sizeLabel;

    @FXML
    private Label keyWordLabel;

    @FXML
    private TextArea textArea;

    @FXML
    private TextField textField;

    private Client client;
    private GraphicsContext g;

    //---Rysowanie---
    private double prevPosX=-1;
    private double prevPosY=-1;
    private double x;
    private double y;
    private int size = 7;
    private Color color = Color.BLACK;
    //---

    private boolean admin = false;
    private boolean drawPermission = false;
    private Message msg;
    private String guess;

    public void initialize(){
        colorPicker.setValue(Color.BLACK);
        g = canvas.getGraphicsContext2D();
    }

    @FXML
    private void onBrushSizeChange(){
        size = (int)slider.getValue();
        sizeLabel.setText(""+size);
    }

    @FXML
    private void onColorPicker(){
        color=colorPicker.getValue();
        try {
            client.getOut().writeObject(new ColorRGB(color)); // color
        } catch (IOException e) {
            e.printStackTrace();
        }
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
            client.getOut().writeObject(new Point(x,y,size,false)); //line
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
            client.getOut().writeObject(new Point(prevPosX,prevPosY,size,true)); // point
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onWordEnter(){
        String word = textField.getText() + "\n";
        textField.setText("");
        textArea.appendText(word);

        try {
            client.getOut().writeObject(new Guess(word));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        // the following loop performs the exchange of
        // information between client and client handler

        Object obj;
        Point point;


        while (true)
        {
            try {

                obj = client.getIn().readObject(); //get object

                if(obj instanceof Point){
                    point = (Point)obj;
                    size=point.getSize();
                    //color=point.getColor();

                    if(point.isSinglePoint()){
                        prevPosX=point.getX();
                        prevPosY=point.getY();
                        drawPoint(prevPosX,prevPosY);
                    }else{
                        x=point.getX();
                        y=point.getY();
                        drawLine(x,y);
                    }
                }else if (obj instanceof ColorRGB){
                    color = Color.rgb(((ColorRGB) obj).getR(),((ColorRGB) obj).getG(),((ColorRGB) obj).getB());
                }else if (obj instanceof Message){
                    msg = (Message)obj;

                    if(msg.getMessageType().equals("ADMIN")){
                        admin=msg.getMessage();
                    }else if(msg.getMessageType().equals("DRAWER")){
                        drawPermission=msg.getMessage();
                    }
                } else if(obj instanceof Guess){
                    guess = ((Guess)obj).getGuess();
                    textArea.appendText(guess);
                }



            } catch(IOException | ClassNotFoundException e) {
                //e.printStackTrace();
                //System.out.println("BYE");
            }
        }
    }
}
