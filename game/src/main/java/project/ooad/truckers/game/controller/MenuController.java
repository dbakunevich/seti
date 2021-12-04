package project.ooad.truckers.game.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import project.ooad.truckers.game.model.GameModel;

import java.io.IOException;
import java.util.Objects;

public class MenuController {
    private final GameModel gameModel = GameModel.getInstance();

    private final Image movedStartButton = new Image("project.ooad.truckers.game/buttons/new_game_button/new_game_2.png");
    private final Image exitedStartButton = new Image("project.ooad.truckers.game/buttons/new_game_button/new_game_1.png");

    private final Image movedConnectButton = new Image("project.ooad.truckers.game/buttons/connect_button/connect_2.png");
    private final Image exitedConnectButton = new Image("project.ooad.truckers.game/buttons/connect_button/connect_1.png");

    private final Image movedOptionsButton = new Image("project.ooad.truckers.game/buttons/options_button/options_2.png");
    private final Image exitedOptionsButton = new Image("project.ooad.truckers.game/buttons/options_button/options_1.png");

    private final Image movedExitButton = new Image("project.ooad.truckers.game/buttons/exit_button/exit_2.png");
    private final Image exitedExitButton = new Image("project.ooad.truckers.game/buttons/exit_button/exit_1.png");

    @FXML
    private ImageView newGameButton;

    @FXML
    private ImageView connectButton;

    @FXML
    private ImageView optionsButton;

    @FXML
    private ImageView exitButton;

    public MenuController() throws IOException {
    }

    public void newGameButtonPressed(MouseEvent event) throws IOException {
        Parent gameViewParent = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/project.ooad.truckers.game/fxml/new_game.fxml")));

        Scene gameViewScene = new Scene(gameViewParent);

        Stage window = (Stage)((Node)event.getSource()).getScene().getWindow();

        window.setScene(gameViewScene);
        window.show();
    }

    public void newGameButtonMoved() {
        newGameButton.setImage(movedStartButton);
    }

    public void newGameButtonExited() {
        newGameButton.setImage(exitedStartButton);
    }

    public void connectButtonPressed(MouseEvent event) throws IOException {
        gameModel.receiveAnnouncementMessages();

        Parent gameViewParent = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/project.ooad.truckers.game/fxml/connect.fxml")));

        Scene gameViewScene = new Scene(gameViewParent);

        Stage window = (Stage)((Node)event.getSource()).getScene().getWindow();

        window.setScene(gameViewScene);
        window.show();
    }

    public void connectButtonMoved() {
        connectButton.setImage(movedConnectButton);
    }

    public void connectButtonExited() {
        connectButton.setImage(exitedConnectButton);
    }

    public void optionsButtonPressed(MouseEvent event) throws IOException {
        Parent gameViewParent = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/project.ooad.truckers.game/fxml/options.fxml")));

        Scene gameViewScene = new Scene(gameViewParent);

        Stage window = (Stage)((Node)event.getSource()).getScene().getWindow();

        window.setScene(gameViewScene);
        window.show();
    }

    public void optionsButtonMoved() {
        optionsButton.setImage(movedOptionsButton);
    }

    public void optionsButtonExited() {
        optionsButton.setImage(exitedOptionsButton);
    }

    public void exitButtonPressed(MouseEvent event) {
        Stage window = (Stage)((Node)event.getSource()).getScene().getWindow();

        window.close();
    }

    public void exitButtonMoved() {
        exitButton.setImage(movedExitButton);
    }

    public void exitButtonExited() {
        exitButton.setImage(exitedExitButton);
    }
}
