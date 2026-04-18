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
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class TrackPane implements Serializable {

    private static final long serialVersionUID = 1L;

    private Track track;
    private final List<ClipPane> clipPanes = new ArrayList<>();

    private transient Pane controlPane;
    private transient Pane timeLinePane;

    private transient FloatProperty zoomFactor;
    private transient FloatProperty clipStartOffset = new SimpleFloatProperty(0f);
    private double lastMouseX = 0d;

    private transient ContextMenu timeLinePaneContextMenu;
    private transient ContextMenu controlPaneContextMenu;
    private transient EventHandler<ActionEvent> onDeleteAction;
    private transient EventHandler<ActionEvent> onAddClipAction;
    private transient EventHandler<ActionEvent> onAddReiterativeClipAction;

    public TrackPane(FloatProperty zoomFactor) {
        this.track = new Track();
        this.zoomFactor = new SimpleFloatProperty(1f);
        this.zoomFactor.bind(zoomFactor);
        this.clipStartOffset = new SimpleFloatProperty(0f);
        initUi();
    }

    private void initUi() {
        controlPane = new Pane();
        timeLinePane = new Pane();
        timeLinePane.prefHeightProperty().bind(this.zoomFactor.multiply(100));
        timeLinePane.prefWidthProperty().bind(this.zoomFactor.multiply(36000000));
        timeLinePane.setStyle("-fx-background-color:lightgray;");
        setupTimeLinePaneContextMenu();
        setupControlPaneContextMenu();
    }

    private void setupControlPaneContextMenu() {
        controlPaneContextMenu = new ContextMenu();

        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(event -> {
            if (onDeleteAction != null) onDeleteAction.handle(event);
        });

        controlPaneContextMenu.getItems().add(deleteItem);

        controlPane.setOnMousePressed(event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                event.consume();
                controlPaneContextMenu.show(controlPane, event.getScreenX(), event.getScreenY());
            } else if (controlPaneContextMenu.isShowing()) {
                controlPaneContextMenu.hide();
            }
        });
    }

    private void setupTimeLinePaneContextMenu() {
        timeLinePaneContextMenu = new ContextMenu();

        MenuItem addClipItem = new MenuItem("Add clip");
        addClipItem.setOnAction(event -> {
            if (onAddClipAction != null) onAddClipAction.handle(event);
        });

        MenuItem addReiterativeClipItem = new MenuItem("Add recursive clip");
        addReiterativeClipItem.setOnAction(event -> {
            if (onAddReiterativeClipAction != null) onAddReiterativeClipAction.handle(event);
        });

        timeLinePaneContextMenu.getItems().addAll(addClipItem, addReiterativeClipItem);

        timeLinePane.setOnMousePressed(event -> {
            lastMouseX = event.getX();
            if (event.getButton() == MouseButton.SECONDARY) {
                event.consume();
                timeLinePaneContextMenu.show(timeLinePane, event.getScreenX(), event.getScreenY());
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

    public List<ClipPane> getClipPanes() {
        return clipPanes;
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.zoomFactor = new SimpleFloatProperty(1f);
        this.clipStartOffset = new SimpleFloatProperty(0f);
        initUi();

        for (ClipPane clipPane : clipPanes) {
            clipPane.rebuildAfterDeserialization(zoomFactor);
            clipPane.setOnRemoveAction(event -> removeAudioClip(clipPane));
            clipPane.prefHeightProperty().bind(timeLinePane.prefHeightProperty().subtract(10));
            timeLinePane.getChildren().add(clipPane);
        }
    }

    public void bindZoomFactor(FloatProperty zoomFactor) {
        this.zoomFactor.bind(zoomFactor);
        clipPanes.forEach(clipPane -> clipPane.bindZoomFactor(zoomFactor));
    }

    public void bindClipStartOffset(FloatProperty clipStartOffset) {
        clipPanes.forEach(clipPane ->
                clipPane.layoutXProperty().bind(clipPane.getAudioClip().getTimelineMsPositionProperty().multiply(zoomFactor).subtract(clipStartOffset))
        );

    }
}
