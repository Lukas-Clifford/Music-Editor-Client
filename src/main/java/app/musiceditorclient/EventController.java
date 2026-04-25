package app.musiceditorclient;

import app.musiceditorclient.infrastructure.AppFileUtils;
import app.musiceditorclient.models.RecursiveClipDialogResult;
import app.musiceditorclient.view.TrackPane;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class EventController {

    @FXML protected TableView<TrackPane> tracksTableView;
    @FXML protected TableColumn<TrackPane, Pane> trackControlPaneColumn;
    @FXML protected TableColumn<TrackPane, Pane> trackTimelinePaneColum;

    @FXML protected Button addTrackButton;
    @FXML protected Button playButton;
    @FXML protected Button addTreeViewButton;
    @FXML protected SplitPane samplesSplitPane;
    @FXML protected Button selectionToolButton;
    @FXML protected ListView<String> logListView;

    protected final EditorContext context;
    protected final EditorServices services;
    protected final CommandManager commandManager;

    protected EventController(EditorContext context, EditorServices services, CommandManager commandManager) {
        this.context = context;
        this.services = services;
        this.commandManager = commandManager;
    }

    protected void stopPlayback() {
        playButton.setText("▶");
        services.playbackService().stopPlaybackForEdit();
    }




    @FXML
    protected void onAddNewTrack() {
        TrackPane newTrack = new TrackPane(context.ui().zoomFactorProperty());
        stopPlayback();
        services.trackService().configureTrackPane(newTrack);
        context.project().getTrackPanes().add(newTrack);
        services.playbackService().reloadPlaybackEngine();
        tracksTableView.refresh();
    }

    @FXML
    protected void onCreateNewProject() {
        context.project().setTrackPanes(new ArrayList<>());
        context.project().setCurrentProject(null);
        context.ui().getSampleTreeRoots().clear();
        context.selection().getSelectedClips().clear();

        tracksTableView.setItems(FXCollections.observableList(context.project().getTrackPanes()));
        tracksTableView.getItems().clear();
        tracksTableView.refresh();

        onAddNewTrack();
        onAddNewTrack();


        services.playbackService().reloadPlaybackEngine();


        onSaveProject();
        services.projectPersistenceService().notifyProjectLoaded();
    }

    @FXML
    protected void onSaveProject() {
        if (context.project().getCurrentProject() == null) {
            services.projectPersistenceService().createProject();
            if (context.project().getCurrentProject() == null) return;
        }
        services.projectPersistenceService().saveProject();
    }

    @FXML
    protected void onOpenProject() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select project");
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("musicproject files (*.musicproject)", "*.musicproject");
        fileChooser.getExtensionFilters().add(extFilter);

        fileChooser.setInitialDirectory(AppFileUtils.getProjectsDir().toFile());

        File file = fileChooser.showOpenDialog(tracksTableView.getScene().getWindow());
        if (file == null) {
            return;
        }

        try {
            services.projectPersistenceService().loadProject(file.toPath());

            tracksTableView.setItems(FXCollections.observableList(context.project().getTrackPanes()));

            context.project().getTrackPanes().forEach(services.trackService()::configureTrackPane);

            tracksTableView.refresh();
            services.playbackService().reloadPlaybackEngine();

            services.treeSampleService().restoreSampleTreeViews(samplesSplitPane);

            AppFileUtils.writeProperty("LAST_OPENED_PROJECT", file.getAbsolutePath());
            services.projectPersistenceService().notifyProjectLoaded();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    @FXML
    protected void onExportAudio() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export audio");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("WAV files (*.wav)", "*.wav")
        );
        fileChooser.setInitialFileName("song.wav");

        if (context.project().getCurrentProject() != null && context.project().getCurrentProject().getParent() != null) {
            fileChooser.setInitialDirectory(context.project().getCurrentProject().getParent().toFile());
        } else {
            File projectsDir = AppFileUtils.getProjectsDir().toFile();
            if (projectsDir.exists()) {
                fileChooser.setInitialDirectory(projectsDir);
            }
        }

        File outputFile = fileChooser.showSaveDialog(tracksTableView.getScene().getWindow());
        if (outputFile == null) {
            return;
        }

        if (!outputFile.getName().toLowerCase().endsWith(".wav")) {
            outputFile = new File(outputFile.getParentFile(), outputFile.getName() + ".wav");
        }

        context.playback().getPlaybackEngine().exportToWav(outputFile.toPath());
    }

    @FXML
    protected void onGoToStart() {
        stopPlayback();
        context.playback().getPlaybackEngine().requestStop();
        context.playback().getPlaybackEngine().clearPauseRequest();
        context.playback().getPlaybackEngine().setPausedFrame(0);
        context.playback().getPlaybackEngine().seeker.set(0);

        if (context.playback().getPlaybackThread() != null && context.playback().getPlaybackThread().isAlive()) {
            try {
                context.playback().getPlaybackThread().join(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }


        context.playback().setPlaybackRunning(false);
        playButton.setText("▶");
    }

    @FXML
    protected void onPlay() {
        if (context.playback().isPlaybackRunning() && !context.playback().getPlaybackEngine().isPaused()) {
            playButton.setText("▶");
            services.playbackService().pausePlayback();
            return;
        }

        if (context.playback().getPlaybackEngine().isPaused()) {
            context.playback().getPlaybackEngine().clearPauseRequest();
            if (!context.playback().isPlaybackRunning()) {
                playButton.setText("⏸");
                services.playbackService().startPlayback();
            } else {
                playButton.setText("⏸");
            }
            return;
        }

        playButton.setText("⏸");
        services.playbackService().startPlayback();
    }

    @FXML
    protected void onImportSamplePack() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select pack");
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("sample packs (*.zip)", "*.zip");
        fileChooser.getExtensionFilters().add(extFilter);

        fileChooser.setInitialDirectory(AppFileUtils.getProjectsDir().toFile());

        File file = fileChooser.showOpenDialog(tracksTableView.getScene().getWindow());
        if (file == null) {
            return;
        }

        AppFileUtils.extractZipIntoSamplesDir(file);
        services.treeSampleService().restoreSampleTreeViews(samplesSplitPane);
    }

    @FXML
    protected void onClearSelection() {
        services.selectionService().clearSelection();
    }

    @FXML
    protected void onAddTreeView() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select folder");
        directoryChooser.setInitialDirectory(AppFileUtils.resolveSamplesDir());

        File chosenDir = directoryChooser.showDialog(tracksTableView.getScene().getWindow());
        if (chosenDir == null || !chosenDir.isDirectory()) {
            return;
        }
        context.ui().getSampleTreeRoots().add(chosenDir.toPath());

        TreeView<String> newTreeView = services.treeSampleService().createSamplesTreeViewForDirectory(chosenDir);
        samplesSplitPane.getItems().add(
                samplesSplitPane.getItems().size(),
                services.treeSampleService().createTreeViewBox(newTreeView, samplesSplitPane
                ));
        services.treeSampleService().normalizeSamplesSplitPane(samplesSplitPane);
    }

    @FXML
    protected void onToggleSelectionTool() {
        context.selection().isSelectionToolActivePropertyProperty().set(!context.selection().isSelectionToolActivePropertyProperty().get());
        if (context.selection().isSelectionToolActivePropertyProperty().get()) {
            selectionToolButton.setStyle("-fx-background-color:blue;");
        } else {
            selectionToolButton.setStyle("-fx-background-color:transparent;");
        }
    }
}
