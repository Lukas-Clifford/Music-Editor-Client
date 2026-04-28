package app.musiceditorclient.services;

import app.musiceditorclient.EditorContext;
import app.musiceditorclient.MainApplication;
import app.musiceditorclient.infrastructure.AppFileUtils;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;
import one.jpro.platform.mdfx.MarkdownView;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class AboutWindowService {

    private final EditorContext context;

    public AboutWindowService(EditorContext context) {
        this.context = context;
    }

    public void showManualWindow() {
        try {
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("About Clifford's Music Editor");
            stage.setScene(createMarkdownScene(AppFileUtils.readManualMd(), 700, 700, 800));
            stage.showAndWait();
        } catch (IOException e) {
            Alert alert = createErrorAlert("About window error", "Unable to open the About window.");
            alert.showAndWait();
        }
    }

    public void showShortcutsWindow() {
        try {
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Shortcuts");
            stage.setScene(createMarkdownScene(AppFileUtils.readShortcutsMd(), 900, 900, 750));
            stage.showAndWait();
        } catch (IOException e) {
            Alert alert = createErrorAlert("About window error", "Unable to open the shortcut window.");
            alert.showAndWait();
        }
    }

    public void showInfoWindow() {
        try {
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("About Clifford's Music Editor");
            stage.setScene(createMarkdownScene(AppFileUtils.readAboutMd(), 700, 700, 430));
            stage.showAndWait();
        } catch (IOException e) {
            Alert alert = createErrorAlert("About window error", "Unable to open the info window.");
            alert.showAndWait();
        }
    }

    private Scene createMarkdownScene(String markdown, int maxWidth, double width, double height) {
        MarkdownView markdownView = new MarkdownView(markdown) {
            @Override
            protected List<String> getDefaultStylesheets() {
                return List.of(Objects.requireNonNull(MainApplication.class.getResource("styles/markdown.css")).toExternalForm());
            }
        };
        markdownView.setMaxWidth(maxWidth);
        markdownView.setPadding(new Insets(20, 20, 20, 20));

        ScrollPane scrollPane = new ScrollPane(markdownView);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return new Scene(scrollPane, width, height);
    }

    private Alert createErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        return alert;
    }
}
