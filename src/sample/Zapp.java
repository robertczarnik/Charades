package sample;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import server.Client;

import java.util.concurrent.TimeUnit;

public class Zapp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{

        TimeUnit.SECONDS.sleep(1);

        Client client = new Client("localhost",5001);


        FXMLLoader loader = new FXMLLoader(getClass().getResource("sample.fxml"));
        Parent root = loader.load();
        Controller controller = loader.getController();

        controller.initClient(client);

        Thread t = new Thread(controller);
        t.setDaemon(true);
        t.start();


        primaryStage.setTitle("Kalambury");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();


        primaryStage.setOnCloseRequest(we -> client.closeConnection());
    }


    public static void main(String[] args) {
        launch(args);
    }
}
