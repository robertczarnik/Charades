package sample;

//przyznawanie punktow

import collections.Pair;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;
import objects.ColorRGB;
import objects.Guess;
import objects.Message;
import objects.Point;
import server.Client;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;


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
    private TextField textField;

    @FXML
    private TextFlow textFlow;

    @FXML
    private ScrollPane scrollPane;

    @FXML
    private Button start;

    @FXML
    private ListView<Pair<String,Integer>> listView;

    @FXML
    private Label timer;

    private Timeline timeline;
    private IntegerProperty timeSeconds = new SimpleIntegerProperty();

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
    private String name;

    private List<Pair<String,Integer>> scoreborad;
    private ObservableList<Pair<String,Integer>> players = FXCollections.observableArrayList();

    public void initialize(){
        colorPicker.setValue(Color.BLACK);
        g = canvas.getGraphicsContext2D();
        scrollPane.vvalueProperty().bind(textFlow.heightProperty()); //auto scroll down

        listView.setItems(players);

        timer.textProperty().bind(timeSeconds.asString()); //zbindowanie labela timer z licznikiem sekund
        //ObservableList<String> names = FXCollections.observableArrayList();
    }

    @FXML
    private void onStart(){
        start.setVisible(false);
        try {
            client.getOut().writeObject(new Message("START",""));
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    public void initClient(Client client, String name) { //tylko raz mozna zainicjalizowac clienta
        this.name=name;

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
        String word = textField.getText();
        textField.setText("");

        Text textName = new Text(name+": ");
        textName.setFont(Font.font("Verdana", FontWeight.BOLD, 12));

        textFlow.getChildren().add(textName);
        textFlow.getChildren().add(new Text(word + "\n"));

        try {
            client.getOut().writeObject(new Guess(word));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void timerManager(int time) {
        if (timeline != null) {
            timeline.stop();
        }
        timeSeconds.set(time);
        timeline = new Timeline();
        timeline.getKeyFrames().add(
                new KeyFrame(Duration.seconds(time+1),
                        new KeyValue(timeSeconds, 0)));

        timeline.setOnFinished(event -> {
            try {
                client.getOut().writeObject(new Message("START",""));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        timeline.playFromStart();

    }

    @SuppressWarnings("unchecked")
    public static <T> T castToAnything(Object obj) {
        return (T) obj;
    }

    @Override
    public void run() {
        // the following loop performs the exchange of
        // information between client and client handler

        Object obj;
        Point point;


        //send player name to server
        try {
            client.getOut().writeObject(new Message("NAME",name));
        } catch (IOException e) {
            e.printStackTrace();
        }

        while (true)
        {
            try {

                obj = client.getIn().readObject(); //get object


                if(obj instanceof Point){
                    point = (Point)obj;
                    size=point.getSize();

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
                        admin=Boolean.parseBoolean(msg.getMessage());
                        if(admin){
                            start.setVisible(true);
                        }
                    }else if(msg.getMessageType().equals("DRAWER")){
                        Object[] drawAndTime = msg.getMessage().split(",");
                        drawPermission = Boolean.parseBoolean(drawAndTime[0].toString());
                        int time = Integer.parseInt(drawAndTime[1].toString());

                        if(drawPermission){ //true
                            sizeLabel.setVisible(true);
                            keyWordLabel.setVisible(true);
                            slider.setVisible(true);
                            colorPicker.setVisible(true);
                            textField.setDisable(true);
                            canvas.setDisable(false);
                            g.clearRect(0,0,canvas.getWidth(),canvas.getHeight());

                        }else{
                            sizeLabel.setVisible(false);
                            keyWordLabel.setVisible(false);
                            slider.setVisible(false);
                            colorPicker.setVisible(false);
                            textField.setDisable(false);
                            canvas.setDisable(true);
                            g.clearRect(0,0,canvas.getWidth(),canvas.getHeight());
                        }

                        Platform.runLater(new Runnable() { // jak to dziala??
                            public void run() {
                                timerManager(time);
                            }
                        });
                    }
                } else if(obj instanceof Guess){
                    guess = ((Guess)obj).getGuess();
                    String tab[];
                    tab=guess.split(":");

                    Text textName = new Text(tab[0]+": ");
                    textName.setFont(Font.font("Verdana", FontWeight.BOLD, 12));


                    Platform.runLater(new Runnable() { // jak to dziala??
                        public void run() {
                            textFlow.getChildren().add(textName);
                            textFlow.getChildren().add(new Text(tab[1] + "\n"));
                        }
                    });
                } else if(obj instanceof List){
                    scoreborad = castToAnything(obj);

                    Iterator<Pair<String,Integer>> iterators = scoreborad.iterator();

                    while(iterators.hasNext()){
                        Pair<String,Integer> ele = iterators.next();
                        System.out.println(ele.getFirst());
                    }


                    Platform.runLater(new Runnable() { // jak to dziala??
                        public void run() {
                            players.setAll(scoreborad);
                        }
                    });
                }



            } catch(IOException | ClassNotFoundException e) {
                //e.printStackTrace();
                //System.out.println("BYE");
            }
        }
    }
}
