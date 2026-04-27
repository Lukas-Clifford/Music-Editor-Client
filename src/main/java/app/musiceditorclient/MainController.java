package app.musiceditorclient;

import app.musiceditorclient.commands.*;
import app.musiceditorclient.infrastructure.AppFileUtils;
import app.musiceditorclient.models.RecursiveClipDialogResult;
import app.musiceditorclient.models.TrimClipDialogResult;
import app.musiceditorclient.view.ClipPane;
import app.musiceditorclient.view.TimelineSeekerPane;
import app.musiceditorclient.view.TrackPane;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

public class MainController extends EventController {

    public MainController(EditorContext context, EditorServices services, CommandManager commandManager) {
        super(context, services, commandManager);
    }

    @FXML
    public void initialize() {
        try {
            versionLabel.setText(AppFileUtils.readProperty("APPVERSION"));
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Property error");
            alert.setHeaderText(null);
            alert.setContentText("Couldn't read app version property");
            alert.showAndWait();
        }

        setupTableView();
        setupTrackPanesEvents();
        setupSelectionContextMenu();
        setupLogListView();

        context.playback().setPlaybackEngine(new PlaybackEngine());
        context.ui().getTimelineSeekerPane().seekerPosition.bind(context.playback().getPlaybackEngine().seeker);
        context.ui().getTimelineSeekerPane().bindSongLengthProperty(context.playback().getPlaybackEngine().songLengthProperty);
        context.ui().getTimelineSeekerPane().bindStartOffsetProperty(context.ui().clipStartOffsetProperty());
        onAddNewTrack();
        onAddNewTrack();

        services.projectPersistenceService().loadLastOpenedProject();
        tracksTableView.setItems(FXCollections.observableList(context.project().getTrackPanes()));
        context.project().getTrackPanes().forEach(services.trackService()::configureTrackPane);
        tracksTableView.refresh();
        services.playbackService().reloadPlaybackEngine();
        services.treeSampleService().restoreSampleTreeViews(samplesSplitPane);

        commandManager.getActionLog().addListener((ListChangeListener.Change<? extends String> cambio) -> {
            cambio.next();
            statusLabel.setText(cambio.getList().getFirst());
        });
    }

    private void setupLogListView() {
        logListView.setItems(commandManager.getActionLog());
    }

    private void setupTableView() {
        tracksTableView.setItems(FXCollections.observableList(context.project().getTrackPanes()));
        tracksTableView.setSelectionModel(null);
        tracksTableView.setOnSort(Event::consume);
        tracksTableView.setFixedCellSize(100);
        tracksTableView.fixedCellSizeProperty().bind(context.ui().zoomFactorProperty().multiply(500).add(40));

        trackControlPaneColumn.setSortable(false);
        trackTimelinePaneColum.setSortable(false);

        trackControlPaneColumn.setCellValueFactory(new PropertyValueFactory<>("controlPane"));
        trackTimelinePaneColum.setCellValueFactory(new PropertyValueFactory<>("timeLinePane"));

        context.ui().setTimelineSeekerPane(new TimelineSeekerPane(context.ui().zoomFactorProperty()));
        trackTimelinePaneColum.setGraphic(context.ui().getTimelineSeekerPane());
    }


    private void setupTrackPanesEvents() {

        services.trackService().setEventHandlers(
                this::onPasteCopiedClips,
                this::onClipSelection,
                this::onRightClickSelection,
                this::onLeftClickWhileFileSelected,
                this::onMuteTrack,
                this::onRemoveTrack,
                this::onSplitClipPane,
                this::onTrimClip,
                this::onAddClip,
                this::onAddReiterativeClip,
                this::onMoveTrackUp,
                this::onMoveTrackDown,
                this::onRemoveClipPane,
                this::onTrackSelection,
                this::onMoveClip
        );


    }


    private void setupSelectionContextMenu() {
        services.selectionService().setupSelectionContextMenu(
                event -> {
                    if (!context.selection().getSelectedClips().isEmpty()) {
                        double seconds = services.dialogService().getSecondsToMoveSelection();
                        commandManager.executeCommand(new MoveSelectionCommand(event, seconds));
                    }
                }, event -> {
                    if (!context.selection().getSelectedClips().isEmpty()) {
                        double seconds = services.dialogService().getSecondsToMoveSelection();
                        commandManager.executeCommand(new MoveSelectionToPositionCommand(event, seconds));
                    }
                }, event -> {
                    if (!context.selection().getSelectedClips().isEmpty()) {
                        commandManager.executeCommand(new RemoveSelectionCommand(event));
                    }
                }, event -> {
                    if (!context.selection().getSelectedClips().isEmpty()) {
                        services.selectionService().copySelectedClips();
                    }
                }, event ->{
                    if (!context.selection().getSelectedClips().isEmpty()) {
                        services.selectionService().copySelectedClips();
                        commandManager.executeCommand(new RemoveSelectionCommand(event));
                    }
                }
        );
    }

    public void onPasteCopiedClips(ActionEvent event) {
        commandManager.executeCommand(new PasteCopiedClipsCommand(event));
    }

    public void onClipSelection(ActionEvent event) {
        services.selectionService().toggleClipSelection(((ClipPane) event.getSource()));
    }

    public void onRightClickSelection(MouseEvent event) {
        services.playbackService().pausePlayback();
        services.selectionService().showSelectionContextMenu(event, ((Node) event.getSource()));
    }

    public void onLeftClickWhileFileSelected(ActionEvent event) {

        if(context.selection().getSelectedFile() != null && !context.selection().isIsSelectionToolActiveProperty()) {
            int startMs = services.trackService().calculateClipStartMs(((TrackPane) event.getSource()));
            commandManager.executeCommand(new AddClipCommand(event, context.selection().getSelectedFile(), startMs));

        }

    }

    public void onMoveTrackUp(ActionEvent event) {
        commandManager.executeCommand(new MoveTrackUpCommand(event, tracksTableView));
    }

    public void onMoveTrackDown(ActionEvent event) {
        commandManager.executeCommand(new MoveTrackDownCommand(event, tracksTableView));
    }

    public void onMuteTrack(ActionEvent event) {
        services.playbackService().stopPlaybackForEdit();
         ((TrackPane) event.getSource()).toggleMuted();
        services.playbackService().reloadPlaybackEngine();
    }

    public void onRemoveTrack(ActionEvent event) {
        commandManager.executeCommand(new RemoveTrackCommand(event, tracksTableView));
    }

    public void onSplitClipPane(ActionEvent event) {
        commandManager.executeCommand(new SplitClipPaneCommand(event));
    }

    public void onTrimClip(ActionEvent event) {

        Optional<TrimClipDialogResult> dialogResult = services.dialogService().showTrimClipDialog();
        if (dialogResult.isEmpty()) return;
        TrimClipDialogResult values = dialogResult.get();

        commandManager.executeCommand(new TrimClipCommand(event, values));

    }

    public void onAddClip(ActionEvent event) {

        File file = services.dialogService().selectSample( ( (TrackPane) event.getSource() ).getTimeLinePane().getScene().getWindow() );
        if (file == null) return;
        int clipStartingPos = services.trackService().calculateClipStartMs( (TrackPane) event.getSource() );
        commandManager.executeCommand(new AddClipCommand(event, file, clipStartingPos));

    }

    public void onAddReiterativeClip(ActionEvent event) {

        File file = services.dialogService().selectSample(tracksTableView.getScene().getWindow());
        if (file == null) return;

        Optional<RecursiveClipDialogResult> dialogResult = services.dialogService().showRecursiveClipDialog();
        if (dialogResult.isEmpty()) return;
        RecursiveClipDialogResult values = dialogResult.get();

        commandManager.executeCommand(new AddReiterativeClipCommand(event, file, values));


    }
    public void onRemoveClipPane(ActionEvent event) {
        commandManager.executeCommand(new RemoveClipCommand(event));
   }

    private void onTrackSelection(ActionEvent event) {
        TrackPane trackPane = (TrackPane) event.getSource();
        trackPane.getClipPanes().forEach(clipPane -> {
            clipPane.setSelected(!clipPane.isSelected());
            if (clipPane.isSelected()) context.selection().getSelectedClips().add(clipPane);
            else context.selection().getSelectedClips().remove(clipPane);
        });
    }

    private void onMoveClip(ActionEvent event) {
        commandManager.executeCommand(new MoveClipCommand(event));
    }
}
