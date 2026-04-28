module Music.Editor.Client {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires org.kordamp.bootstrapfx.core;
    requires com.gluonhq.charm.glisten;
    requires one.jpro.platform.mdfx;
    requires java.desktop;

    opens app.musiceditorclient to javafx.fxml;
    opens app.musiceditorclient.view to javafx.base, javafx.fxml;

    exports app.musiceditorclient;

}