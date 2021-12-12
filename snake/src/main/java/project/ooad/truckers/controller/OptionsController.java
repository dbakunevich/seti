package project.ooad.truckers.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import project.ooad.truckers.model.GameModel;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

public class OptionsController implements Initializable {
    public ImageView backButton;
    public ImageView applyButton;
    private GameModel gameModel = GameModel.getInstance();

    @FXML
    private TextField playerNameField;

    public OptionsController() throws IOException {
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        playerNameField.setText(gameModel.getMasterPlayerName());
    }

    public void applyButtonPressed(MouseEvent event) throws IOException {
        Parent gameViewParent = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/project/ooad/truckers/fxml/menu.fxml")));

        Scene gameViewScene = new Scene(gameViewParent);

        Stage window = (Stage)((Node)event.getSource()).getScene().getWindow();

        window.setScene(gameViewScene);

        gameModel.setMasterPlayerName(playerNameField.getText());
        window.show();
    }


    public void backButtonPressed(MouseEvent event) throws IOException {
        Parent gameViewParent = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/project/ooad/truckers/fxml/menu.fxml")));

        Scene gameViewScene = new Scene(gameViewParent);

        Stage window = (Stage)((Node)event.getSource()).getScene().getWindow();

        window.setScene(gameViewScene);
        window.show();
    }

}
