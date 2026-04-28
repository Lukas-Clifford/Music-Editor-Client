package app.musiceditorclient;

import app.musiceditorclient.commands.*;
import app.musiceditorclient.infrastructure.FfmpegInstaller;
import app.musiceditorclient.models.RecursiveClipDialogResult;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

public class MainApplication extends Application {

    private final EditorContext context = new EditorContext();
    private final EditorServices services = new EditorServices(context);
    private final CommandManager commandManager = new CommandManager(context, services);

    @Override
    public void start(Stage stage) throws IOException {
        FfmpegInstaller.ensureInstalled();

        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("main-view.fxml"));
        MainController controller = new MainController(context, services, commandManager);
        fxmlLoader.setController(controller);

        Scene scene = new Scene(fxmlLoader.load());

        services.projectPersistenceService().setOnProjectLoadedListener(projectPath ->
                stage.setTitle(projectPath.getFileName().toString().replace(".musicproject", "")));

        stage.setTitle("Music Editor");
        stage.setMaximized(true);
        stage.setFullScreen(false);
        Font.loadFont(MainApplication.class.getResourceAsStream("styles/fonts/Seven-Segment.ttf"), 11);
        Font.loadFont(MainApplication.class.getResourceAsStream("styles/fonts/w95f.ttf"), 11);
//        System.out.println(font.getName());
        scene.getStylesheets().add(Objects.requireNonNull(MainApplication.class.getResource("styles/studio.css")).toExternalForm());
        stage.setScene(scene);

        scene.setOnScroll(event -> {
            if (event.isControlDown()) {
                context.ui().zoomFactorProperty().set(
                        (float) (context.ui().zoomFactorProperty().get() + (Math.signum(event.getDeltaY()) / 100))
                );
                event.consume();

            } else if (event.isShiftDown()) {

                float nextOffset = context.ui().clipStartOffsetProperty().get() + (float) Math.signum(event.getDeltaX()) * 50;
                context.ui().clipStartOffsetProperty().set(Math.max(0f, nextOffset));
                event.consume();

            }
        });

        scene.setOnKeyPressed(event -> {

            if (event.isControlDown()) {
                if (event.getCode() == KeyCode.S) {
                    services.projectPersistenceService().saveProject();
                    controller.statusLabel.setText("PROJECT SAVED");
                }
                if (event.getCode() == KeyCode.A && event.isShiftDown())
                    services.selectionService().clearSelection();


                if (event.getCode() == KeyCode.X) {
                    if (!context.selection().getSelectedClips().isEmpty()) {
                        services.selectionService().copySelectedClips();
                        commandManager.executeCommand(new RemoveSelectionCommand(null));
                    }
                }

                if (event.getCode() == KeyCode.C)
                    services.selectionService().copySelectedClips();
                if (event.getCode() == KeyCode.V && !context.selection().getCopiedClips().isEmpty()) {

                    commandManager.executeCommand(new AddTrackCommand(null, controller.tracksTableView));
                    int startMs = (int) (services.dialogService().getSecondsToPasteClips(
                            context.selection().getCopiedClips().getFirst().getAudioClip().getTimelineMsPosition()
                    ) * 1000);
                    commandManager.executeCommand(
                            new PasteCopiedClipsCommand(
                                    new ActionEvent(context.project().getTrackPanes().getLast(), null),
                                    startMs
                            )
                    );
                }

                if (event.getCode() == KeyCode.R) {

                    File file = services.dialogService().selectSample(scene.getWindow());
                    if (file == null) return;

                    Optional<RecursiveClipDialogResult> dialogResult = services.dialogService().showRecursiveClipDialog();
                    if (dialogResult.isEmpty()) return;
                    RecursiveClipDialogResult values = dialogResult.get();

                    commandManager.executeCommand(new AddTrackCommand(null, controller.tracksTableView));

                    commandManager.executeCommand(new AddReiterativeClipCommand(
                            new ActionEvent(context.project().getTrackPanes().getLast(), null),
                            file, values));

                }

                if (event.getCode() == KeyCode.M) {
                    if (!context.selection().getSelectedClips().isEmpty()) {
                        double seconds = services.dialogService().getSecondsToMoveSelection();
                        commandManager.executeCommand(new MoveSelectionToPositionCommand(event, seconds));
                    }
                }


                if (event.getCode() == KeyCode.Z)
                    commandManager.undo();
                if (event.getCode() == KeyCode.Y)
                    commandManager.redo();

            } else if (event.getCode() == KeyCode.M) {
                if (!context.selection().getSelectedClips().isEmpty())  {
                    double seconds = services.dialogService().getSecondsToMoveSelection();
                    commandManager.executeCommand(new MoveSelectionCommand(event, seconds));
                }
            }


             if (event.getCode() == KeyCode.DELETE)
                commandManager.executeCommand(new RemoveSelectionCommand(null));

            if (event.getCode() == KeyCode.DIGIT1)
                    services.playbackService().goToFrame(
                            context.playback().getPlaybackEngine().getPausedFrame()-44100);
            if (event.getCode() == KeyCode.DIGIT2)
                    services.playbackService().goToFrame(
                            context.playback().getPlaybackEngine().getPausedFrame()+44100);
            if (event.getCode() == KeyCode.DIGIT3)
                    services.playbackService().goToFrame(
                            context.playback().getPlaybackEngine().getPausedFrame()-11025);
            if (event.getCode() == KeyCode.DIGIT4)
                    services.playbackService().goToFrame(
                            context.playback().getPlaybackEngine().getPausedFrame()+11025);


            if (event.getCode() == KeyCode.CONTROL)
                services.selectionService().enableSelection();


        });

        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.SPACE) {
                controller.onPlay();
                event.consume();
            }

            if (event.isControlDown() && event.getCode() == KeyCode.A && !event.isShiftDown())
                context.project().getTrackPanes().forEach(
                        trackPane -> trackPane.getClipPanes().forEach(clipPane -> {
                            clipPane.setSelected(true);
                            context.selection().getSelectedClips().add(clipPane);
                        })

                );

        });

        scene.setOnKeyReleased(event -> {
            if (event.getCode() == KeyCode.CONTROL)
                services.selectionService().disableSelection();
        });

        stage.show();
    }
}
