package app.musiceditorclient.services;

import app.musiceditorclient.EditorContext;
import app.musiceditorclient.MainController;
import app.musiceditorclient.infrastructure.AppFileUtils;
import app.musiceditorclient.models.Clip;
import app.musiceditorclient.models.RecursiveClipDialogResult;
import app.musiceditorclient.models.TrimClipDialogResult;
import app.musiceditorclient.view.ClipPane;
import app.musiceditorclient.view.TrackPane;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.Optional;

public class TrackService {
    private final EditorContext context;
    private EventHandler<ActionEvent> onPasteCopiedClipsEventHandler = null;
    private EventHandler<ActionEvent> onClipSelectionEventHandler = null;
    private EventHandler<MouseEvent>  onRightClickSelectionEventHandler = null;
    private EventHandler<ActionEvent>  onLeftClickWhileFileSelectedEventHandler = null;
    private EventHandler<ActionEvent> onMuteTrackEventHandler = null;
    private EventHandler<ActionEvent> onRemoveTrackEventHandler = null;
    private EventHandler<ActionEvent> onSplitClipPaneEventHandler = null;
    private EventHandler<ActionEvent> onTrimClipEventHandler = null;
    private EventHandler<ActionEvent> onAddClipEventHandler = null;
    private EventHandler<ActionEvent> onAddReiterativeClipEventHandler = null;
    private EventHandler<ActionEvent> onMoveTrackUpEventHandler = null;
    private EventHandler<ActionEvent> onMoveTrackDownClipEventHandler = null;
    private EventHandler<ActionEvent> onRemoveClipEventHandler = null;
    private EventHandler<ActionEvent> onTrackSelectionEventHandler = null;
    private EventHandler<ActionEvent> onMoveClipEventHandler = null;


    public TrackService(EditorContext context) {
        this.context = context;
    }

    public void setEventHandlers(
        EventHandler<ActionEvent> onPasteCopiedClipsEventHandler,
        EventHandler<ActionEvent> onClipSelectionEventHandler,
        EventHandler<MouseEvent>  onRightClickSelectionEventHandler,
        EventHandler<ActionEvent> onLeftClickWhileFileSelectedEventHandler,
        EventHandler<ActionEvent> onMuteTrackEventHandler,
        EventHandler<ActionEvent> onRemoveTrackEventHandler,
        EventHandler<ActionEvent> onSplitClipPaneEventHandler,
        EventHandler<ActionEvent> onTrimClipEventHandler,
        EventHandler<ActionEvent> onAddClipEventHandler,
        EventHandler<ActionEvent> onAddReiterativeClipEventHandler,
        EventHandler<ActionEvent> onMoveTrackUpEventHandler,
        EventHandler<ActionEvent> onMoveTrackDownClipEventHandler,
        EventHandler<ActionEvent> onRemoveClipEventHandler,
        EventHandler<ActionEvent> onTrackSelectionEventHandler,
        EventHandler<ActionEvent> onMoveClipEventHandler
    ) {

        this.onPasteCopiedClipsEventHandler = onPasteCopiedClipsEventHandler;
        this.onClipSelectionEventHandler = onClipSelectionEventHandler;
        this.onRightClickSelectionEventHandler = onRightClickSelectionEventHandler;
        this.onLeftClickWhileFileSelectedEventHandler = onLeftClickWhileFileSelectedEventHandler;
        this.onMuteTrackEventHandler = onMuteTrackEventHandler;
        this.onRemoveTrackEventHandler = onRemoveTrackEventHandler;
        this.onSplitClipPaneEventHandler = onSplitClipPaneEventHandler;
        this.onTrimClipEventHandler = onTrimClipEventHandler;
        this.onAddClipEventHandler = onAddClipEventHandler;
        this.onAddReiterativeClipEventHandler = onAddReiterativeClipEventHandler;
        this.onMoveTrackUpEventHandler = onMoveTrackUpEventHandler;
        this.onMoveTrackDownClipEventHandler = onMoveTrackDownClipEventHandler;
        this.onRemoveClipEventHandler = onRemoveClipEventHandler;
        this.onTrackSelectionEventHandler = onTrackSelectionEventHandler;
        this.onMoveClipEventHandler = onMoveClipEventHandler;
    }

    public void setupTrackHeaderContextMenu(EventHandler<ActionEvent> onAddTrack) {
        MenuItem addTrackItem = new MenuItem("Add track");
        addTrackItem.setOnAction(onAddTrack);

        context.ui().getAddTrackContextMenu().getItems().add(addTrackItem);
    }

    public void addTrack(TrackPane trackPane) {
        context.project().getTrackPanes().add(trackPane);
    }

    public int calculateClipStartMs(TrackPane trackPane) {
        float pixelsPerMs = context.ui().zoomFactorProperty().get();
        float ms = (float) (trackPane.getLastMouseX() / pixelsPerMs);
        return Math.max(0, (int) ms);
    }


    public void configureTrackPane(TrackPane trackPane) {
        trackPane.setOnDeleteAction(onRemoveTrackEventHandler);
        trackPane.setOnAddClipAction(onAddClipEventHandler);
        trackPane.setOnAddReiterativeClipAction(onAddReiterativeClipEventHandler);
        trackPane.setOnPasteCopiedClips(onPasteCopiedClipsEventHandler);
        trackPane.setOnTrimAction(onTrimClipEventHandler);
        trackPane.setOnClipSelection(onClipSelectionEventHandler);
        trackPane.setOnRightClickSelection(onRightClickSelectionEventHandler);
        trackPane.setOnMousePressedAction(onLeftClickWhileFileSelectedEventHandler);
        trackPane.setOnSplitClip(onSplitClipPaneEventHandler);
        trackPane.setOnMoveTrackUp(onMoveTrackUpEventHandler);
        trackPane.setOnMoveTrackDown(onMoveTrackDownClipEventHandler);
        trackPane.setOnMuteTrack(onMuteTrackEventHandler);
        trackPane.setOnRemoveClip(onRemoveClipEventHandler);
        trackPane.setOnTrackSelectionAction(onTrackSelectionEventHandler);
        trackPane.setOnMoveClipAction(onMoveClipEventHandler);

        trackPane.getClipPanes().forEach(trackPane::registerClipPaneHandlers);

        trackPane.bindZoomFactor(context.ui().zoomFactorProperty());
        trackPane.bindClipStartOffset(context.ui().clipStartOffsetProperty());
        trackPane.bindSelectionEnabled(context.selection().isSelectionToolActivePropertyProperty());
    }



}
