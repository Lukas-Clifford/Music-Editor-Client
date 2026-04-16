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
    }

    private void reloadPlaybackEngine() {
        pe.clearTracks();
        for (TrackPane trackPane : trackPanes) {
            pe.addTrack(trackPane.getTrack());
        }
    }

    private void configureTrackPane(TrackPane trackPane) {
        trackPane.setOnAddClipAction(event -> chooseAndAddClip(trackPane));
        trackPane.setOnAddReiterativeClipAction(event -> chooseAndAddReiterativeClip(trackPane));
        trackPane.clipStartOffsetProperty().bind(clipStartOffset);
    }

    private void chooseAndAddClip(TrackPane trackPane) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar clip");
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Archivos WAV (*.wav)", "*.wav");
        fileChooser.getExtensionFilters().add(extFilter);

        File samplesDir = resolveSamplesDir();
        if (samplesDir != null && samplesDir.isDirectory()) {
            fileChooser.setInitialDirectory(samplesDir);
        }

        File file = fileChooser.showOpenDialog(tracksTableView.getScene().getWindow());
        if (file == null) {
            return;
        }

        int startMs = calculateClipStartMs(trackPane);
        trackPane.addAudioClip(new Clip(file, startMs));
    }

    private int calculateClipStartMs(TrackPane trackPane) {
        float pixelsPerMs = zoomFactor.get();
        float ms = (float) (trackPane.getLastMouseX() / pixelsPerMs);
        return (int) ms;
    }

    private void chooseAndAddReiterativeClip(TrackPane trackPane) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar clip");
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Archivos WAV (*.wav)", "*.wav");
        fileChooser.getExtensionFilters().add(extFilter);

        File samplesDir = resolveSamplesDir();
        if (samplesDir != null && samplesDir.isDirectory()) {
            fileChooser.setInitialDirectory(samplesDir);
        }

        File file = fileChooser.showOpenDialog(tracksTableView.getScene().getWindow());
        if (file == null) {
            return;
        }

        TextInputDialog secondsDialog = new TextInputDialog("1");
        secondsDialog.setTitle("Añadir clip reiterativo");
        secondsDialog.setHeaderText("Cada cuántos segundos se repetirá el clip");
        secondsDialog.setContentText("Segundos:");

        Optional<String> secondsResult = secondsDialog.showAndWait();
        if (secondsResult.isEmpty()) {
            return;
        }

        TextInputDialog repetitionsDialog = new TextInputDialog("2");
        repetitionsDialog.setTitle("Añadir clip reiterativo");
        repetitionsDialog.setHeaderText("Cuántas repeticiones añadir");
        repetitionsDialog.setContentText("Repeticiones:");

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
        } catch (NumberFormatException ignored) {
            // Ignored
        }
    }

    private File resolveSamplesDir() {
        URL samplesUrl = getClass().getResource("/app/musiceditorclient/samples");
        if (samplesUrl != null) {
            return new File(samplesUrl.getPath());
        }
        return new File(System.getProperty("user.home"));
    }

    private void setupTrackHeaderContextMenu() {
        MenuItem addTrackItem = new MenuItem("Añadir track");
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

        TextInputDialog repetitionsDialog = new TextInputDialog("Proyecto");
        repetitionsDialog.setTitle("Crear nuevo proyecto");
        repetitionsDialog.setHeaderText("Nombre del proyecto");
        repetitionsDialog.setContentText("Nombre: ");

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

            System.out.println("Archivo guardado");
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    @FXML
    private void openProject() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar proyecto");
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Archivos musicproject (*.musicproject)", "*.musicproject");
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
    private void addNewTrack() {
        TrackPane newTrack = new TrackPane(zoomFactor);
        configureTrackPane(newTrack);
        trackPanes.add(newTrack);
        pe.addTrack(newTrack.getTrack());
        tracksTableView.refresh();
    }

    @FXML
    public void exportAudio() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exportar audio");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("WAV (*.wav)", "*.wav")
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

    @FXML
    public void play() {
        System.out.println("Reproduciendo...");
        new Thread(() -> pe.play()).start();
    }
}
