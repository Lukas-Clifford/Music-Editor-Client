package app.musiceditorclient.view;

import app.musiceditorclient.models.Clip;
import app.musiceditorclient.models.Track;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;

import java.util.ArrayList;
import java.util.List;

public class TrackPane {

    private Track track;

    private final List<ClipPane> clipPanes = new ArrayList<>();

    private final Pane controlPane = new Pane();
    private final Pane timeLinePane = new Pane();

    private final FloatProperty zoomFactor;
    private final FloatProperty clipStartOffset = new SimpleFloatProperty(0f);
    private double lastMouseX = 0d;

    private final ContextMenu timeLinePaneContextMenu = new ContextMenu();
    private final ContextMenu controlPaneContextMenu = new ContextMenu();
    private EventHandler<ActionEvent> onDeleteAction;
    private EventHandler<ActionEvent> onAddClipAction;
    private EventHandler<ActionEvent> onAddReiterativeClipAction;

    public TrackPane(FloatProperty zoomFactor) {
        this.track = new Track();

        this.zoomFactor = new SimpleFloatProperty(1f);
        this.zoomFactor.bind(zoomFactor);

        timeLinePane.prefHeightProperty().bind(this.zoomFactor.multiply(100));
        timeLinePane.prefWidthProperty().bind(this.zoomFactor.multiply(36000000)); // max length 10 mins
        timeLinePane.setStyle("-fx-background-color:lightgray;");

        setupTimeLinePaneContextMenu();
        setupControlPaneContextMenu();
    }

    private void setupControlPaneContextMenu() {
        MenuItem deleteItem = new MenuItem("Eliminar");
        deleteItem.setOnAction(event -> {
            if (onDeleteAction != null) onDeleteAction.handle(event);
        });

        controlPaneContextMenu.getItems().add(deleteItem);

        controlPane.setOnMousePressed(event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                controlPaneContextMenu.show(controlPane, event.getScreenX(), event.getScreenY());
                event.consume();
            } else if (controlPaneContextMenu.isShowing()) {
                controlPaneContextMenu.hide();
            }
        });
    }

    private void setupTimeLinePaneContextMenu() {
        MenuItem addClipItem = new MenuItem("Añadir clip");
        addClipItem.setOnAction(event -> {
            if (onAddClipAction != null) onAddClipAction.handle(event);
        });

        MenuItem addReiterativeClipItem = new MenuItem("Añadir clip reiterativo");
        addReiterativeClipItem.setOnAction(event -> {
            if (onAddReiterativeClipAction != null) onAddReiterativeClipAction.handle(event);
        });

        timeLinePaneContextMenu.getItems().addAll(addClipItem, addReiterativeClipItem);

        timeLinePane.setOnMousePressed(event -> {
            lastMouseX = event.getX();
            if (event.getButton() == MouseButton.SECONDARY) {
                timeLinePaneContextMenu.show(timeLinePane, event.getScreenX(), event.getScreenY());
                event.consume();
            }
        });
    }

    public double getLastMouseX() {
        return lastMouseX;
    }

    public void setOnAddReiterativeClipAction(EventHandler<ActionEvent> onAddReiterativeClipAction) {
        this.onAddReiterativeClipAction = onAddReiterativeClipAction;
    }

    public void setOnDeleteAction(EventHandler<ActionEvent> onDeleteAction) {
        this.onDeleteAction = onDeleteAction;
    }

    public void setOnAddClipAction(EventHandler<ActionEvent> onAddClipAction) {
        this.onAddClipAction = onAddClipAction;
    }

    public FloatProperty clipStartOffsetProperty() {
        return clipStartOffset;
    }

    public void setClipStartOffset(float value) {
        clipStartOffset.set(value);
    }

    public float getClipStartOffset() {
        return clipStartOffset.get();
    }

    public Track getTrack() {
//        return new Track(clipPanes.stream().map(ClipPane::getAudioClip).toList());
        return track;
    }

    public void addAudioClip(Clip clip) {
        ClipPane clipPane = new ClipPane(clip, zoomFactor);
        clipPanes.add(clipPane);

        track.addClip(clip);

        clipPane.setOnRemoveAction(event -> removeAudioClip(clipPane));
        clipPane.prefHeightProperty().bind(timeLinePane.prefHeightProperty().subtract(10));
        clipPane.layoutXProperty().bind(clip.getTimelineMsPositionProperty().multiply(zoomFactor).subtract(clipStartOffset));
        timeLinePane.getChildren().add(clipPane);
    }

    private void removeAudioClip(ClipPane clipPane) {
        track.removeClip(clipPane.getAudioClip());
        clipPanes.remove(clipPane);
        timeLinePane.getChildren().remove(clipPane);
    }

    public Pane getControlPane() {
        return controlPane;
    }

    public Pane getTimeLinePane() {
        return timeLinePane;
    }
}
