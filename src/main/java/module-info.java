module app.musiceditorclient {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.bootstrapfx.core;

    opens app.musiceditorclient to javafx.fxml;
    exports app.musiceditorclient;
}