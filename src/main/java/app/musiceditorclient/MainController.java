package app.musiceditorclient;

import app.musiceditorclient.models.Clip;
import app.musiceditorclient.view.TimelineSeekerPane;
import app.musiceditorclient.view.TrackPane;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    @FXML
    public void initialize() {
        clipStartOffset.addListener((obs, oldValue, newValue) ->
                System.out.println("clipStartOffset = " + newValue.floatValue()));

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
    private void addNewTrack() {
        TrackPane newTrack = new TrackPane(zoomFactor);
        configureTrackPane(newTrack);
        trackPanes.add(newTrack);
        pe.addTrack(newTrack.getTrack());
        tracksTableView.refresh();
    }

    @FXML
    public void play() {
        System.out.println("Reproduciendo...");
        new Thread(() -> pe.play()).start();
    }
}
