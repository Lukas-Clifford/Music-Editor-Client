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

    public void showAboutWindow() {
        try {
            MarkdownView markdownView = new MarkdownView(AppFileUtils.readManualMd()){
                @Override
                protected List<String> getDefaultStylesheets() {
                    return List.of(Objects.requireNonNull(MainApplication.class.getResource("styles/markdown.css")).toExternalForm());
                }
            };

            markdownView.setMaxWidth(700);
            markdownView.setPadding(new Insets(20,20,20,20));

            ScrollPane scrollPane = new ScrollPane(markdownView);
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("About Clifford's Music Editor");

            Scene scene = new Scene(scrollPane, 700, 800);

            stage.setScene(scene);
            stage.showAndWait();


        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("About window error");
            alert.setHeaderText(null);
            alert.setContentText("Unable to open the About window.");
            alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
            alert.showAndWait();
        }
    }
}
