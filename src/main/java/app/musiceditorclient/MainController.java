package app.musiceditorclient;

import app.musiceditorclient.infrastructure.AppFileUtils;
import app.musiceditorclient.models.Clip;
import app.musiceditorclient.view.TimelineSeekerPane;
import app.musiceditorclient.view.TrackPane;
import javafx.application.Platform;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;

import javax.crypto.spec.PSource;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class MainController {

    public TableView<TrackPane> tracksTableView;
    public TableColumn<TrackPane, Pane> trackControlPaneColumn;
    public TableColumn<TrackPane, Pane> trackTimelinePaneColum;

    public FloatProperty zoomFactor = new SimpleFloatProperty(1f);
    public FloatProperty clipStartOffset = new SimpleFloatProperty(0f);
    public TimelineSeekerPane timelineSeekerPane;
    public Button addTrackButton;
    public Button playButton;
    public TreeView<String>  samplesFilesTreeView;

    List<TrackPane> trackPanes = new ArrayList<>();
    PlaybackEngine pe;

    private final ContextMenu addTrackContextMenu = new ContextMenu();

    private Path currentProject;

    private Consumer<Path> onProjectLoadedListener;

    public void setOnProjectLoadedListener(Consumer<Path> onProjectLoadedListener) {
        this.onProjectLoadedListener = onProjectLoadedListener;
    }

    private void notifyProjectLoaded() {
        if (onProjectLoadedListener != null && currentProject != null) {
            onProjectLoadedListener.accept(currentProject);
        }
    }

    private File selectedFile;

    @FXML
    public void initialize() {
        tracksTableView.setItems(FXCollections.observableList(trackPanes));
        tracksTableView.setSelectionModel(null);
        tracksTableView.setOnSort(Event::consume);

        trackControlPaneColumn.setSortable(false);
        trackTimelinePaneColum.setSortable(false);

        trackControlPaneColumn.setCellValueFactory(new PropertyValueFactory<>("controlPane"));
        trackTimelinePaneColum.setCellValueFactory(new PropertyValueFactory<>("timeLinePane"));

        timelineSeekerPane = new TimelineSeekerPane(zoomFactor);
        trackTimelinePaneColum.setGraphic(timelineSeekerPane);

        tracksTableView.setFixedCellSize(100);
        tracksTableView.fixedCellSizeProperty().bind(zoomFactor.multiply(100));

        setupTrackHeaderContextMenu();

        pe = new PlaybackEngine();
        timelineSeekerPane.seekerPosition.bind(pe.seeker);

        addNewTrack();
        addNewTrack();

        try {
            String lastOpenedProject = AppFileUtils.readProperty("LAST_OPENED_PROJECT");
            if (!lastOpenedProject.equals("null")) {
                loadProject(Path.of(lastOpenedProject));
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println(e.getMessage());
        }

        setupSamplesTreeView();
        setupSelectedSampleBehavior();
    }

    private void setupSamplesTreeView() {
        File samplesDir = AppFileUtils.resolveSamplesDir();
        TreeItem<String> rootItem = new TreeItem<>(samplesDir.getPath());
        rootItem.setExpanded(true);

        if (samplesDir.exists() && samplesDir.isDirectory()) {
            addWavFilesRecursively(rootItem, samplesDir);
        }

        samplesFilesTreeView.setRoot(rootItem);
        samplesFilesTreeView.setShowRoot(true);
    }

    private void addWavFilesRecursively(TreeItem<String> parentItem, File directory) {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                TreeItem<String> dirItem = new TreeItem<>(file.getName());
                parentItem.getChildren().add(dirItem);
                addWavFilesRecursively(dirItem, file);
            } else if (file.getName().toLowerCase().endsWith(".wav")) {
                parentItem.getChildren().add(new TreeItem<>(file.getName()));
            }
        }
    }

    private void configureTrackPane(TrackPane trackPane) {
        trackPane.setOnDeleteAction(event -> removeTrack(trackPane));
        trackPane.setOnAddClipAction(event -> chooseAndAddClip(trackPane));
        trackPane.setOnAddReiterativeClipAction(event -> chooseAndAddReiterativeClip(trackPane));
        trackPane.setOnTrimAction(event -> {
            if (event.getSource() instanceof app.musiceditorclient.view.ClipPane clipPane) {
                trimClip(clipPane);
            }
        });
        trackPane.setOnMousePressedAction(event -> {
            if (event.getButton() == MouseButton.PRIMARY && selectedFile != null) {
                stopPlaybackForEdit();
                int startMs = calculateClipStartMs(trackPane);
                trackPane.addAudioClip(new Clip(selectedFile, startMs));
                reloadPlaybackEngine();
                tracksTableView.refresh();
            }
        });
        trackPane.bindZoomFactor(zoomFactor);
        trackPane.bindClipStartOffset(clipStartOffset);
    }

    private void setupSelectedSampleBehavior() {
        samplesFilesTreeView.setOnMousePressed(event -> {
            if (event.getButton() != MouseButton.PRIMARY) {
                return;
            }

            TreeItem<String> item = samplesFilesTreeView.getSelectionModel().getSelectedItem();
            if (item == null) {
                selectedFile = null;
                return;
            }

            if (!item.getChildren().isEmpty()) {
                selectedFile = null;
                return;
            }

            TreeItem<String> root = samplesFilesTreeView.getRoot();
            if (root == null) {
                selectedFile = null;
                return;
            }

            File current = new File(root.getValue());
            List<String> parents = new ArrayList<>();

            TreeItem<String> cursor = item;
            while (cursor != null && cursor != root) {
                parents.add(0, cursor.getValue());
                cursor = cursor.getParent();
            }

            for (String segment : parents) {
                current = new File(current, segment);
            }

            selectedFile = current.exists() && current.isFile() ? current : null;
            System.out.println(selectedFile);
        });
    }

    private void reloadPlaybackEngine() {
        pe.clearTracks();
        for (TrackPane trackPane : trackPanes) {
            pe.addTrack(trackPane.getTrack());
        }
    }


    private void trimClip(app.musiceditorclient.view.ClipPane clipPane) {
        TextInputDialog frontDialog = new TextInputDialog("0");
        frontDialog.setTitle("Trim clip");
        frontDialog.setHeaderText("Milliseconds to trim from the start");
        frontDialog.setContentText("Front:");

        Optional<String> frontResult = frontDialog.showAndWait();
        if (frontResult.isEmpty()) {
            return;
        }

        TextInputDialog backDialog = new TextInputDialog("0");
        backDialog.setTitle("Trim clip");
        backDialog.setHeaderText("Milliseconds to trim from the end");
        backDialog.setContentText("Back:");

        Optional<String> backResult = backDialog.showAndWait();
        if (backResult.isEmpty()) {
            return;
        }

        try {
            int trimFrontMs = Integer.parseInt(frontResult.get().trim());
            int trimBackMs = Integer.parseInt(backResult.get().trim());

            Clip clip = clipPane.getAudioClip();
            int oldLength = clip.getLength();
            int oldAudioStart = clip.getAudioStartMs();
            int oldTimeLineMsPos = clip.getTimelineMsPosition();

            int newAudioStart = Math.max(0, oldAudioStart + trimFrontMs);
            int newLength = Math.max(0, oldLength - trimFrontMs - trimBackMs);

            clip.setAudioStartMs(newAudioStart);
            clip.setTimelineMsPosition(oldTimeLineMsPos + trimFrontMs);
            clip.setLength(newLength);

            clipPane.setClipNameLabel(clip.getWavFile().getName() + " : " + clip.getLength() + "ms");
            clipPane.refreshSize();

            reloadPlaybackEngine();
            tracksTableView.refresh();
        } catch (NumberFormatException ignored) {
            // Ignored
        }
    }

    private void removeTrack(TrackPane trackPane) {
        trackPanes.remove(trackPane);
        reloadPlaybackEngine();
        tracksTableView.refresh();
    }

    private void stopPlaybackForEdit() {
        if (pe != null) {
            pe.requestStop();
        }
        isPlaying = false;
        playbackRunning = false;
        playButton.setText("▶");
    }

    private void addTrackAndReloadEngine(TrackPane newTrack) {
        stopPlaybackForEdit();
        configureTrackPane(newTrack);
        trackPanes.add(newTrack);
        reloadPlaybackEngine();
        tracksTableView.refresh();
    }

    @FXML
    private void addNewTrack() {
        TrackPane newTrack = new TrackPane(zoomFactor);
        addTrackAndReloadEngine(newTrack);
    }

    private void chooseAndAddClip(TrackPane trackPane) {
        stopPlaybackForEdit();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select clip");
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("WAV files (*.wav)", "*.wav");
        fileChooser.getExtensionFilters().add(extFilter);

        File samplesDir = AppFileUtils.resolveSamplesDir();
        if (samplesDir.isDirectory()) {
            fileChooser.setInitialDirectory(samplesDir);
        }

        File file = fileChooser.showOpenDialog(tracksTableView.getScene().getWindow());
        if (file == null) {
            return;
        }

        int startMs = calculateClipStartMs(trackPane);
        trackPane.addAudioClip(new Clip(file, startMs));
        reloadPlaybackEngine();
    }

    private int calculateClipStartMs(TrackPane trackPane) {
        float pixelsPerMs = zoomFactor.get();
        float ms = (float) (trackPane.getLastMouseX() / pixelsPerMs);
        return Math.max(0, (int) ms);
    }

    private void chooseAndAddReiterativeClip(TrackPane trackPane) {
        stopPlaybackForEdit();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select clip");
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("WAV files (*.wav)", "*.wav");
        fileChooser.getExtensionFilters().add(extFilter);

        File samplesDir = AppFileUtils.resolveSamplesDir();
        if (samplesDir.isDirectory()) {
            fileChooser.setInitialDirectory(samplesDir);
        }

        File file = fileChooser.showOpenDialog(tracksTableView.getScene().getWindow());
        if (file == null) {
            return;
        }

        TextInputDialog secondsDialog = new TextInputDialog("1");
        secondsDialog.setTitle("Add recursive clip");
        secondsDialog.setHeaderText("How many seconds between each repetition");
        secondsDialog.setContentText("Seconds:");

        Optional<String> secondsResult = secondsDialog.showAndWait();
        if (secondsResult.isEmpty()) {
            return;
        }

        TextInputDialog repetitionsDialog = new TextInputDialog("2");
        repetitionsDialog.setTitle("Add recursive clip");
        repetitionsDialog.setHeaderText("How many repetitions to add");
        repetitionsDialog.setContentText("Repetitions:");

        Optional<String> repetitionsResult = repetitionsDialog.showAndWait();
        if (repetitionsResult.isEmpty()) {
            return;
        }

        try {
            float seconds = Float.parseFloat(secondsResult.get().replace(",", "."));
            int repetitions = Integer.parseInt(repetitionsResult.get());
            float stepMs = seconds * 1000f;

            for (int i = 0; i < repetitions; i++) {
                trackPane.addAudioClip(new Clip(file, (int) (i * stepMs)));
            }
            reloadPlaybackEngine();
        } catch (NumberFormatException ignored) {
            // Ignored
        }
    }


    private void setupTrackHeaderContextMenu() {
        MenuItem addTrackItem = new MenuItem("Add track");
        addTrackItem.setOnAction(event -> addNewTrack());

        addTrackContextMenu.getItems().add(addTrackItem);
    }

    @FXML
    private void createNewProject() {
        trackPanes = new ArrayList<>();
        currentProject = null;

        tracksTableView.setItems(FXCollections.observableList(trackPanes));
        tracksTableView.getItems().clear();
        tracksTableView.refresh();

        addNewTrack();
        addNewTrack();

        reloadPlaybackEngine();

        try {
            createProject();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

        saveProject();
        notifyProjectLoaded();
    }

    private void createProject() {

        TextInputDialog repetitionsDialog = new TextInputDialog("Project");
        repetitionsDialog.setTitle("Create new project");
        repetitionsDialog.setHeaderText("Project name");
        repetitionsDialog.setContentText("Name: ");

        Optional<String> repetitionsResult = repetitionsDialog.showAndWait();
        if (repetitionsResult.isEmpty()) {
            return;
        }

        try {
            currentProject = AppFileUtils.createMusicProjectFile(repetitionsResult.get());
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }

        saveProject();
    }

    @FXML
    public void saveProject() {
        try {
            if (currentProject == null) {
                createProject();
                if (currentProject == null) return;
            }

            AppFileUtils.writeTrackPanesToMusicProject(currentProject, trackPanes);
            AppFileUtils.writeProperty("LAST_OPENED_PROJECT", currentProject.toAbsolutePath().toString());

            System.out.println("File saved");
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    @FXML
    private void openProject() {
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
            loadProject(file.toPath());
            AppFileUtils.writeProperty("LAST_OPENED_PROJECT", file.getAbsolutePath());
            notifyProjectLoaded();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println(e.getMessage());
        }
    }

    private void loadProject(Path projectPath) throws IOException, ClassNotFoundException {
        currentProject = projectPath;
        trackPanes = AppFileUtils.readTrackPanesFromMusicProject(projectPath);
        tracksTableView.setItems(FXCollections.observableList(trackPanes));
        trackPanes.forEach(this::configureTrackPane);
        tracksTableView.refresh();
        reloadPlaybackEngine();
    }


    @FXML
    public void exportAudio() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export audio");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("WAV files (*.wav)", "*.wav")
        );
        fileChooser.setInitialFileName("song.wav");

        if (currentProject != null && currentProject.getParent() != null) {
            fileChooser.setInitialDirectory(currentProject.getParent().toFile());
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

        pe.exportToWav(outputFile.toPath());
    }

    private boolean isPlaying = false;
    private volatile boolean playbackRunning = false;
    private Thread playbackThread;

    private void startPlayback() {
        if (playbackRunning) {
            return;
        }

        playbackRunning = true;
        isPlaying = true;
        playButton.setText("⏸");

        playbackThread = new Thread(() -> {
            try {
                pe.play();
            } finally {
                playbackRunning = false;
                isPlaying = false;
                Platform.runLater(() -> playButton.setText("▶"));
            }
        });
        playbackThread.setDaemon(true);
        playbackThread.start();
    }

    private void pausePlayback() {
        isPlaying = false;
        pe.requestPause();
        playButton.setText("Play");
    }

    @FXML
    public void play() {
        if (playbackRunning && !pe.isPaused()) {
            pausePlayback();
            return;
        }

        if (pe.isPaused()) {
            pe.clearPauseRequest();
            if (!playbackRunning) {
                startPlayback();
            } else {
                isPlaying = true;
                playButton.setText("⏸");
            }
            return;
        }

        startPlayback();
    }
}
