package robertczarnik.client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import robertczarnik.client.Client;
import robertczarnik.client.Controller;

import java.io.IOException;

public class WelcomeScreenController {
    @FXML
    TextField nameTextField;

    @FXML
    Button playButton;

    @FXML
    private void onPlayButton(ActionEvent event) throws IOException {
        Client client = new Client("localhost",5001);

        FXMLLoader gameLoader = new FXMLLoader(getClass().getResource("/gameScreen.fxml"));
        Parent gameParent = gameLoader.load();
        Controller controller = gameLoader.getController();

        controller.initClient(client,nameTextField.getText()); // zainiciowanie clienta  i jego nazwy

        Thread t = new Thread(controller);
        t.setDaemon(true);
        t.start();

        Scene gameScene = new Scene(gameParent);
        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();

        stage.setTitle("Kalambury");
        stage.setScene(gameScene);
        stage.show();

        stage.setOnCloseRequest(we -> client.closeConnection());
    }
}
