module project.ooad.truckers.game {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.protobuf;
    requires java.sql;


    opens project.ooad.truckers.game to javafx.fxml;
    exports project.ooad.truckers.game;
}