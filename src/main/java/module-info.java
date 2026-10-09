module org.example.a311cardgame24 {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.example.a311cardgame24 to javafx.fxml;
    exports org.example.a311cardgame24;
}