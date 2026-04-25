package app.musiceditorclient;

import app.musiceditorclient.models.Clip;
import app.musiceditorclient.models.RecursiveClipDialogResult;
import app.musiceditorclient.models.Track;
import app.musiceditorclient.models.TrimClipDialogResult;
import app.musiceditorclient.view.ClipPane;
import app.musiceditorclient.view.TimelineSeekerPane;
import app.musiceditorclient.view.TrackPane;
import javafx.beans.property.SimpleFloatProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;

import java.io.File;
import java.util.Optional;

public class MainController extends EventController {

    public MainController(EditorContext context, EditorServices services) {
        super(context, services);
    }

    @FXML
    public void initialize() {
        setupTableView();
        setupTrackPanesEvents();
        setupSelectionContextMenu();

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
                this::onMoveTrackDown
        );


    }

    private void setupSelectionContextMenu() {
        services.selectionService().setupSelectionContextMenu(
                event -> {
                    if (!context.selection().getSelectedClips().isEmpty()) {
                        stopPlayback();
                        double seconds = services.dialogService().getSecondsToMoveSelection();
                        services.selectionService().moveSelectedClips(seconds);
                        services.playbackService().reloadPlaybackEngine();
                    }
                }, event -> {
                    if (!context.selection().getSelectedClips().isEmpty()) {
                        stopPlayback();
                        double seconds = services.dialogService().getSecondsToMoveSelection();
                        services.selectionService().moveToSecondsSelectedClips(seconds);
                        services.playbackService().reloadPlaybackEngine();
                    }
                }, event -> {
                    if (!context.selection().getSelectedClips().isEmpty()) {
                        stopPlayback();
                        services.selectionService().removeSelectedClips();
                        services.selectionService().clearSelection();
                        services.playbackService().reloadPlaybackEngine();
                    }
                }, event -> {
                    if (!context.selection().getSelectedClips().isEmpty()) {
                        stopPlayback();
                        services.selectionService().copySelectedClips();
                        services.playbackService().reloadPlaybackEngine();
                    }
                }
        );
    }

    public void onPasteCopiedClips(ActionEvent event) {
        if (!context.selection().getSelectedClips().isEmpty()) {
            services.playbackService().stopPlaybackForEdit();

            TrackPane trackPane = ((TrackPane) event.getSource());
            int startMs = services.trackService().calculateClipStartMs(trackPane);
            services.selectionService().pasteClipPanes(
                    startMs, context.selection().getCopiedClips(),
                    trackPane
            );

            services.playbackService().reloadPlaybackEngine();
        }
    }

    public void onClipSelection(ActionEvent event) {
        services.selectionService().toggleClipSelection(((ClipPane) event.getSource()));
    }

    public void onRightClickSelection(MouseEvent event) {
        services.playbackService().pausePlayback();
        services.selectionService().showSelectionContextMenu(event, ((Node) event.getSource()));
    }
    public void onLeftClickWhileFileSelected(ActionEvent event) {

        if(context.selection().getSelectedFile() != null) {
            services.playbackService().stopPlaybackForEdit();

            int startMs = services.trackService().calculateClipStartMs(((TrackPane) event.getSource()));
            ((TrackPane) event.getSource()).addAudioClip(new Clip(context.selection().getSelectedFile(), startMs));

            services.playbackService().reloadPlaybackEngine();
        }
    }

    public void onMoveTrackUp(ActionEvent event) {
        services.playbackService().stopPlaybackForEdit();
        TrackPane trackPane = ((TrackPane) event.getSource());
        int index = context.project().getTrackPanes().indexOf(trackPane);
        if (index != 0) {
            context.project().getTrackPanes().remove(trackPane);
            context.project().getTrackPanes().add(index-1, trackPane);
            tracksTableView.refresh();
        }
        services.playbackService().reloadPlaybackEngine();
    }

    public void onMoveTrackDown(ActionEvent event) {
        services.playbackService().stopPlaybackForEdit();
        TrackPane trackPane = ((TrackPane) event.getSource());
        int index = context.project().getTrackPanes().indexOf(trackPane);
        if (index != context.project().getTrackPanes().size()-1) {
            context.project().getTrackPanes().remove(trackPane);
            context.project().getTrackPanes().add(index+1, trackPane);
            tracksTableView.refresh();
        }
        services.playbackService().reloadPlaybackEngine();
    }

    public void onMuteTrack(ActionEvent event) {
        services.playbackService().stopPlaybackForEdit();
         ((TrackPane) event.getSource()).toggleMuted();
        services.playbackService().reloadPlaybackEngine();
    }

    public void onRemoveTrack(ActionEvent event) {
        services.playbackService().stopPlaybackForEdit();
        context.project().getTrackPanes().remove((TrackPane) event.getSource());
        services.playbackService().reloadPlaybackEngine();
    }

    public void onSplitClipPane(ActionEvent event) {
        services.playbackService().stopPlaybackForEdit();
        services.trackService().splitClipPane(
                ((TrackPane) event.getSource()),
                ((ClipPane) event.getTarget())
        );
        services.playbackService().reloadPlaybackEngine();
    }
    public void onTrimClip(ActionEvent event) {

        services.playbackService().stopPlaybackForEdit();

        Optional<TrimClipDialogResult> dialogResult = services.dialogService().showTrimClipDialog();
        if (dialogResult.isEmpty()) return;
        TrimClipDialogResult values = dialogResult.get();

        services.trackService().trimClip(((ClipPane) event.getSource()), values);

        services.playbackService().reloadPlaybackEngine();
    }

    public void onAddClip(ActionEvent event) {
        services.playbackService().stopPlaybackForEdit();

        File file = services.dialogService().selectSample(tracksTableView.getScene().getWindow());
        if (file == null) return;

        int startMs = services.trackService().calculateClipStartMs(((TrackPane) event.getSource()));

        ((TrackPane) event.getSource()).addAudioClip(new Clip(file, startMs));

        services.playbackService().reloadPlaybackEngine();
    }

    public void onAddReiterativeClip(ActionEvent event) {
        services.playbackService().stopPlaybackForEdit();

        File file = services.dialogService().selectSample(tracksTableView.getScene().getWindow());
        if (file == null) return;

        Optional<RecursiveClipDialogResult> dialogResult = services.dialogService().showRecursiveClipDialog();
        if (dialogResult.isEmpty()) return;
        RecursiveClipDialogResult values = dialogResult.get();

        services.trackService().addReiterativeClip(((TrackPane) event.getSource()), file, values);

        services.playbackService().reloadPlaybackEngine();

    }


}
