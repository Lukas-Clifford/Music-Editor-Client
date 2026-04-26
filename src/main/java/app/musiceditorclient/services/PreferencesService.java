package app.musiceditorclient.services;

import app.musiceditorclient.EditorContext;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class PreferencesService {

    private final EditorContext context;

    public PreferencesService(EditorContext context) {
        this.context = context;
    }

    public void showPreferencesWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/musiceditorclient/views/preferences-window.fxml"));
            AnchorPane root = loader.load();

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Preferences");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (IOException e) {
            System.err.println("Unable to open preferences window");
        }
    }
}
