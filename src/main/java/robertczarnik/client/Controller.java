package robertczarnik.client;

// server z stolikami i dolaczanie do nich


import javafx.beans.binding.DoubleBinding;
import javafx.scene.Node;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import robertczarnik.collections.Pair;
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
import robertczarnik.objects.ColorRGB;
import robertczarnik.objects.Guess;
import robertczarnik.objects.Message;
import robertczarnik.objects.Point;
import robertczarnik.client.Client;

import java.io.IOException;
import java.util.List;


public class Controller implements Runnable{
    @FXML
    public Rectangle niebieski;

    @FXML
    public Rectangle pomarancza;

    @FXML
    public AnchorPane anchorPane;

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

    @FXML
    public GridPane gridPane;

    private Timeline timeline;
    private IntegerProperty timeSeconds = new SimpleIntegerProperty();

    private Client client;
    private GraphicsContext g;

    //---Drawing---
    private double prevPosX=-1;
    private double prevPosY=-1;
    private double x;
    private double y;
    private int size = 8;
    private Color color = Color.BLACK;
    //---

    private boolean admin = false;
    private Message msg;
    private String guess;
    private String name;

    private boolean drawPermission=false;

    private List<Pair<String,Integer>> scoreboard;
    private ObservableList<Pair<String,Integer>> players = FXCollections.observableArrayList();

    //window size
    private Stage stage;
    private double actualH=500;
    private double actualW=600;
    private double scaleY=1;
    private double scaleX=1;


    @FXML
    private void onMinus(){
        if(stage==null) stage = (Stage) anchorPane.getScene().getWindow(); // get stage to change size later

        if(actualH>500){
            actualH-=250;
            actualW-=300;
            stage.setHeight(actualH);
            stage.setWidth(actualW);
        }
    }

    @FXML
    private void onPlus(){
        if(stage==null) stage = (Stage) anchorPane.getScene().getWindow(); // get stage to change size later

        if(actualH<1500){
            actualH+=250;
            actualW+=300;
            stage.setHeight(actualH);
            stage.setWidth(actualW);
        }
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
    private void onColorPicker(){
        if(drawPermission) {
            color = colorPicker.getValue();
            try {
                client.getOut().writeObject(new ColorRGB(color)); // color
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void onMouseDragged(MouseEvent event){ // draw line between prev pos and actual pos
        scaleY=anchorPane.getHeight()/500;
        scaleX=anchorPane.getWidth()/600;
        if(drawPermission){
            x = event.getX();
            y = event.getY();

            x/=scaleX;
            y/=scaleY;

            drawLine(x,y);

            try {
                client.getOut().writeObject(new Point(x,y,size,false)); //line
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void onMousePressed(MouseEvent event) { // draw a oval point and set previous postions of x and y
        scaleY=anchorPane.getHeight()/500;
        scaleX=anchorPane.getWidth()/600;
        if(drawPermission) {
            prevPosX = event.getX();
            prevPosY = event.getY();

            prevPosX/=scaleX;
            prevPosY/=scaleY;

            drawPoint(prevPosX, prevPosY);

            try {
                client.getOut().writeObject(new Point(prevPosX, prevPosY, size, true)); // point
            } catch (IOException e) {
                e.printStackTrace();
            }

            prevPosX*=scaleX;
            prevPosY*=scaleY;
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

    public void initialize(){
        colorPicker.setValue(Color.BLACK);
        g = canvas.getGraphicsContext2D();

        listView.setItems(players);

        scrollPane.vvalueProperty().bind(textFlow.heightProperty()); //auto scroll down

        sizeLabel.textProperty().bind((slider.valueProperty().asString("%.0f"))); //bind sizeLabel with Integer value of slider
        slider.valueProperty().addListener(e -> size = (int)slider.getValue()); //change size variable when moving slider

        timer.textProperty().bind(timeSeconds.asString()); //bind timer label with countdown timer


        //GRIDPANE
        pomarancza.widthProperty().bind(gridPane.widthProperty());
        pomarancza.heightProperty().bind(gridPane.heightProperty().multiply((10.0/100.0))); //15%

        niebieski.heightProperty().bind(gridPane.heightProperty());
        niebieski.widthProperty().bind(gridPane.widthProperty().multiply((25.0/100.0)));

        canvas.heightProperty().bind(anchorPane.heightProperty().multiply(90.0/100.0));
        canvas.widthProperty().bind(anchorPane.widthProperty().multiply(75.0/100.0));


    }

    public void initClient(Client client, String name) { //tylko raz mozna zainicjalizowac clienta
        this.name=name;

        if (this.client != null) {
            throw new IllegalStateException("Client can only be initialized once");
        }
        this.client = client ;
    }


    //tutaj przemnozyc przez scale punkty x y
    private void drawLine(double x, double y){
        x*=scaleX;
        y*=scaleY;

        g.setStroke(color);
        g.setLineWidth(size);
        g.setLineCap(StrokeLineCap.ROUND);
        g.strokeLine(prevPosX,prevPosY,x,y);
        prevPosX=x;
        prevPosY=y;
    }

    private void drawPoint(double x, double y){
        x*=scaleX;
        y*=scaleY;

        x = x - size/2.0;
        y = y - size/2.0;

        g.setFill(color);
        g.fillOval(x,y,size,size);
    }

    private void roundStart(int time,String word) {
        keyWordLabel.setText(word);

        if (timeline != null) {
            timeline.stop();
        }
        timeSeconds.set(time);
        timeline = new Timeline();
        timeline.getKeyFrames().add(new KeyFrame(Duration.seconds(time+1), new KeyValue(timeSeconds, 0)));

        if(drawPermission){ // only one time send START message
            timeline.setOnFinished(event -> {
                try {
                    client.getOut().writeObject(new Message("START",""));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }

        timeline.playFromStart();
    }

    @SuppressWarnings("unchecked")
    private static <T> T castToAnything(Object obj) {
        return (T) obj;
    }

    /** setting visibility of components and resetting size, color and canvas */
    private void setProperties(boolean drawPermission){
        sizeLabel.setVisible(drawPermission);
        keyWordLabel.setVisible(drawPermission);
        slider.setVisible(drawPermission);
        colorPicker.setVisible(drawPermission);
        textField.setDisable(drawPermission);
        canvas.setDisable(!drawPermission);

        g.clearRect(0,0,canvas.getWidth(),canvas.getHeight());
        size = 8;
        color = Color.BLACK;
        colorPicker.setValue(color);
        slider.setValue(size);
    }

    @Override
    public void run() {
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
                    scaleY=anchorPane.getHeight()/500;
                    scaleX=anchorPane.getWidth()/600;
                    point = (Point)obj;
                    size=point.getSize();


                    System.out.println(scaleX);
                    if(point.isSinglePoint()){
                        prevPosX=point.getX();
                        prevPosY=point.getY();
                        prevPosX*=scaleX;
                        prevPosY*=scaleY;

                        drawPoint(point.getX(),point.getY());
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
                        Object[] drawProperties = msg.getMessage().split(",");

                        drawPermission = Boolean.parseBoolean(drawProperties[0].toString());
                        String word = drawProperties[1].toString();
                        int time = Integer.parseInt(drawProperties[2].toString());

                        boolean finalDrawPermission = drawPermission;
                        Platform.runLater(() -> {
                            roundStart(time,word);
                            setProperties(finalDrawPermission);
                        });
                    }
                } else if(obj instanceof Guess){
                    guess = ((Guess)obj).getGuess();
                    String tab[];
                    tab=guess.split(":");

                    Text textName = new Text(tab[0]+": ");
                    textName.setFont(Font.font("Verdana", FontWeight.BOLD, 12));

                    Platform.runLater(() -> {
                        textFlow.getChildren().add(textName);
                        textFlow.getChildren().add(new Text(tab[1] + "\n"));
                    });

                } else if(obj instanceof List){
                    scoreboard = castToAnything(obj);
                    Platform.runLater(() -> players.setAll(scoreboard));
                }



            } catch(IOException | ClassNotFoundException e) {
                //e.printStackTrace();
                //System.out.println("BYE");
            }
        }
    }
}
