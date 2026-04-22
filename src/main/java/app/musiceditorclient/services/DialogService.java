package app.musiceditorclient.services;

import app.musiceditorclient.EditorContext;
import app.musiceditorclient.infrastructure.AppFileUtils;
import app.musiceditorclient.models.RecursiveClipDialogResult;
import app.musiceditorclient.models.TrimClipDialogResult;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

public class DialogService {

    private final EditorContext context;

    public DialogService(EditorContext context) {
        this.context = context;
    }

    public File selectSample(Window window) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select clip");
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("WAV files (*.wav)", "*.wav");
        fileChooser.getExtensionFilters().add(extFilter);

        File samplesDir = AppFileUtils.resolveSamplesDir();
        if (samplesDir.isDirectory()) {
            fileChooser.setInitialDirectory(samplesDir);
        }

        return fileChooser.showOpenDialog(window);

    }

    public double getSecondsToMoveSelection() {
        double seconds = -1;

        var dialogResult = showTextInputDialog(
                "Move clip to...",
                "Enter seconds to move to",
                "Seconds",
                "0"
        );

        if (dialogResult.isEmpty()) return seconds;

        try {
            return Double.parseDouble(dialogResult.get().replace(",", "."));
        } catch (NumberFormatException ignored) {
            // Ignored
        }
        return seconds;
    }

    public Optional<RecursiveClipDialogResult> showRecursiveClipDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/app/musiceditorclient/views/add-recursive-clip-window.fxml"
            ));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Add recursive clip");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.setScene(new Scene(root));

            TextField startingSecondsField = (TextField) root.lookup("#startingSecondsField");
            TextField secondsBetweenRepetitionField = (TextField) root.lookup("#secondsBetweenRepetitionField");
            TextField numberOfRepetitionsField = (TextField) root.lookup("#numberOfRepetitionsField");
            Button acceptButton = (Button) root.lookup("#acceptButton");
            Button cancelButton = (Button) root.lookup("#cancelButton");

            final RecursiveClipDialogResult[] result = new RecursiveClipDialogResult[1];

            acceptButton.setOnAction(event -> {
                try {
                    float startingSeconds = Float.parseFloat(startingSecondsField.getText().trim().replace(",", "."));
                    float secondsBetweenRepetition = Float.parseFloat(secondsBetweenRepetitionField.getText().trim().replace(",", "."));
                    int numberOfRepetitions = Integer.parseInt(numberOfRepetitionsField.getText().trim());

                    result[0] = new RecursiveClipDialogResult(
                            startingSeconds,
                            secondsBetweenRepetition,
                            numberOfRepetitions
                    );
                    stage.close();
                } catch (NumberFormatException ignored) {
                    // Ignored
                }
            });

            cancelButton.setOnAction(event -> stage.close());

            stage.showAndWait();
            return Optional.ofNullable(result[0]);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<TrimClipDialogResult> showTrimClipDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/app/musiceditorclient/views/trim-clip-window.fxml"
            ));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Trim clip");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.setScene(new Scene(root));

            TextField frontField = (TextField) root.lookup("#frontField");
            TextField backField = (TextField) root.lookup("#backField");
            Button acceptButton = (Button) root.lookup("#acceptButton");
            Button cancelButton = (Button) root.lookup("#cancelButton");

            final TrimClipDialogResult[] result = new TrimClipDialogResult[1];

            acceptButton.setOnAction(event -> {
                try {
                    int frontMs = Integer.parseInt(frontField.getText().trim());
                    int backMs = Integer.parseInt(backField.getText().trim());

                    result[0] = new TrimClipDialogResult(frontMs, backMs);
                    stage.close();
                } catch (NumberFormatException ignored) {
                    // Ignored
                }
            });

            cancelButton.setOnAction(event -> stage.close());

            stage.showAndWait();
            return Optional.ofNullable(result[0]);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<String> showTextInputDialog(String title, String headerText, String contentText, String defaultValue) {
        return Optional.empty();
    }
}
