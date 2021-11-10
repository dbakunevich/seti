module com.example.lab {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires javafx.graphics;
    requires org.json;

    opens com.example.lab3 to javafx.fxml;
    exports com.example.lab3;
}