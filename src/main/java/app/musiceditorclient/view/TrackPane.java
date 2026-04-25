package app.musiceditorclient.view;

import app.musiceditorclient.models.Clip;
import app.musiceditorclient.models.Track;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
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
    private transient EventHandler<ActionEvent> onMousePressedAction;
    private transient EventHandler<ActionEvent> onTrimAction;
    private transient EventHandler<ActionEvent> onClipSelection;
    private transient EventHandler<MouseEvent> onRightClickSelection;
    private transient EventHandler<ActionEvent> onPasteCopiedClips;
    private transient EventHandler<ActionEvent> onSplitClip;
    private transient EventHandler<ActionEvent> onRemoveClip;
    private transient EventHandler<ActionEvent> onMoveTrackUp;
    private transient EventHandler<ActionEvent> onMoveTrackDown;
    private transient EventHandler<ActionEvent> onMuteTrack;
    private transient Pane rulerPane;
    private transient BooleanProperty selectionToolEnabledProperty = new SimpleBooleanProperty(false);

    private transient boolean isMuted = false;

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

        if (zoomFactor != null) {
            timeLinePane.prefHeightProperty().bind(zoomFactor.multiply(100));
            timeLinePane.prefWidthProperty().bind(zoomFactor.multiply(600000));
        }
        timeLinePane.setStyle("-fx-background-color:lightgray;");

        rulerPane = new Pane();
        rulerPane.setMouseTransparent(true);
        if (timeLinePane != null) {
            rulerPane.prefWidthProperty().bind(timeLinePane.prefWidthProperty());
            rulerPane.prefHeightProperty().bind(timeLinePane.prefHeightProperty());
        }
        timeLinePane.getChildren().add(rulerPane);

        setupTimeLinePaneContextMenu();
        setupTimeLinePaneMouseEvents();

        setupControlPane();

    }

    private void setupControlPane() {
        setupControlPaneContextMenu();

        Button muteButton = new Button("Mute");
        Button removeButton = new Button("Remove");
        Button moveUpButton = new Button("/\\");
        Button moveDownButton = new Button("\\/");

        HBox controlButtonsHBox = new HBox();
        controlButtonsHBox.getChildren().addAll(muteButton, removeButton);

        VBox controlPaneVBox = new VBox();
        controlPaneVBox.getChildren().addAll(moveUpButton, controlButtonsHBox, moveDownButton);
        controlPaneVBox.setAlignment(Pos.CENTER);
        controlPaneVBox.setSpacing(5);

        controlPane.getChildren().add(controlPaneVBox);



        muteButton.setOnAction(event -> {
            if (onMuteTrack != null) onMuteTrack.handle(new ActionEvent(this, null));
        });

        removeButton.setOnAction(event -> {
            if (onDeleteAction != null) onDeleteAction.handle(new ActionEvent(this, null));
        });

        moveUpButton.setOnAction(event -> {
            if (onMoveTrackUp != null) onMoveTrackUp.handle(new ActionEvent(this, null));
        });

        moveDownButton.setOnAction(event -> {
            if (onMoveTrackDown != null) onMoveTrackDown.handle(new ActionEvent(this, null));
        });




    }

    private void setupRulerPainting() {
        if (rulerPane == null || zoomFactor == null || clipStartOffset == null) {
            return;
        }

        rulerPane.getChildren().clear();

        double msPerStep = 250.0;
        double visibleSpanMs = 600_000.0;

        for (double ms = 0; ms <= visibleSpanMs; ms += msPerStep) {
            Line line = new Line();

            line.startXProperty().bind(zoomFactor.multiply((float) ms).subtract(clipStartOffset));
            line.endXProperty().bind(zoomFactor.multiply((float) ms).subtract(clipStartOffset));

            boolean isSecond = ((int) ms) % 1000 == 0;
            line.setStartY(0);
            line.setEndY(isSecond ? 18 : 10);
            line.setStroke(isSecond ? Color.CRIMSON : Color.BLACK);
            line.setStrokeWidth(isSecond ? 1.5 : 1.0);

            rulerPane.getChildren().add(line);
        }
    }

    private void setupControlPaneContextMenu() {
        controlPaneContextMenu = new ContextMenu();

        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(event -> {
            if (onDeleteAction != null) onDeleteAction.handle(new ActionEvent(this,null));
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
            if (onAddClipAction != null) onAddClipAction.handle(new ActionEvent(this, null));
        });

        MenuItem addReiterativeClipItem = new MenuItem("Add recursive clip");
        addReiterativeClipItem.setOnAction(event -> {
            if (onAddReiterativeClipAction != null) onAddReiterativeClipAction.handle(new ActionEvent(this, null));
        });

        MenuItem pasteClipPanesItem = new MenuItem("Paste");
        pasteClipPanesItem.setOnAction(event -> {
            if (onPasteCopiedClips != null) onPasteCopiedClips.handle(new ActionEvent(this, null));
        });

        timeLinePaneContextMenu.getItems().addAll(addClipItem, addReiterativeClipItem, pasteClipPanesItem);

    }

    public double getLastMouseX() {
        return lastMouseX;
    }

    public void setOnAddReiterativeClipAction(EventHandler<ActionEvent> onAddReiterativeClipAction) {
        this.onAddReiterativeClipAction = onAddReiterativeClipAction;
    }

    public void setOnPasteCopiedClips(EventHandler<ActionEvent> onPasteCopiedClips) {
        this.onPasteCopiedClips = onPasteCopiedClips;
    }

    public void setOnDeleteAction(EventHandler<ActionEvent> onDeleteAction) {
        this.onDeleteAction = onDeleteAction;
    }

    public void setOnAddClipAction(EventHandler<ActionEvent> onAddClipAction) {
        this.onAddClipAction = onAddClipAction;
    }

    public void setOnMousePressedAction(EventHandler<ActionEvent> onMousePressedAction) {
        this.onMousePressedAction = onMousePressedAction;
    }

    public void setOnTrimAction(EventHandler<ActionEvent> onTrimAction) {
        this.onTrimAction = onTrimAction;
    }

    public void setOnRemoveClip(EventHandler<ActionEvent> onRemoveClip) {
        this.onRemoveClip = onRemoveClip;
    }

    public void setOnClipSelection(EventHandler<ActionEvent> onClipSelection) {
        this.onClipSelection = onClipSelection;
        for (ClipPane clipPane : clipPanes) {
            clipPane.setOnSelectionAction(onClipSelection);
        }
    }
    public void setOnRightClickSelection(EventHandler<MouseEvent> onRightClickSelection) {
        this.onRightClickSelection = onRightClickSelection;
        for (ClipPane clipPane : clipPanes) {
            clipPane.setOnRightClickSelectionAction(onRightClickSelection);
        }
    }

    public void setOnSplitClip(EventHandler<ActionEvent> onSplitClip) {
        this.onSplitClip = onSplitClip;
    }

    public void setOnMoveTrackUp(EventHandler<ActionEvent> onMoveTrackUp) {
        this.onMoveTrackUp = onMoveTrackUp;
    }

    public void setOnMoveTrackDown(EventHandler<ActionEvent> onMoveTrackDown) {
        this.onMoveTrackDown = onMoveTrackDown;
    }

    public void setOnMuteTrack(EventHandler<ActionEvent> onMuteTrack) {
        this.onMuteTrack = onMuteTrack;
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

    public boolean isMuted() {
        return isMuted;
    }

    public void setMuted(boolean muted) {
        isMuted = muted;
    }

    public void toggleMuted() {
        this.isMuted = !this.isMuted;
        if (isMuted)
            this.controlPane.setStyle("-fx-background-color:gray;");
        else
            this.controlPane.setStyle("-fx-background-color:transparent;");

    }


    public void bindSelectionEnabled(BooleanProperty isSelectionToolActiveProperty) {
        if (isSelectionToolActiveProperty == null) {
            return;
        }

        if (this.selectionToolEnabledProperty != null) {
            this.selectionToolEnabledProperty.bind(isSelectionToolActiveProperty);
        }
        for (ClipPane clipPane : clipPanes) {
            if (clipPane == null) {
                continue;
            }

            if (clipPane.selectionEnabledPropertyProperty() != null && this.selectionToolEnabledProperty != null) {
                clipPane.selectionEnabledPropertyProperty().bind(this.selectionToolEnabledProperty);
            }
            clipPane.setOnSelectionAction(onClipSelection);
            clipPane.setOnRightClickSelectionAction(onRightClickSelection);
        }
    }

    public void addAudioClip(Clip clip) {
        ClipPane clipPane = new ClipPane(clip, zoomFactor);
        addClipPane(clipPane);
    }

    public void addClipPane(ClipPane clipPane) {
        if (clipPane == null) {
            return;
        }

        clipPane.setTrackPane(this);
        registerClipPaneHandlers(clipPane);
        clipPanes.add(clipPane);

        if (clipPane.selectionEnabledPropertyProperty() != null && this.selectionToolEnabledProperty != null) {
            clipPane.selectionEnabledPropertyProperty().bind(this.selectionToolEnabledProperty);
        }
        clipPane.setOnSelectionAction(onClipSelection);
        clipPane.setOnRightClickSelectionAction(onRightClickSelection);

        if (timeLinePane != null) {
            clipPane.prefHeightProperty().bind(timeLinePane.heightProperty());
            clipPane.minHeightProperty().bind(timeLinePane.heightProperty());
            clipPane.maxHeightProperty().bind(timeLinePane.heightProperty());
        }

        if (zoomFactor != null && clipStartOffset != null) {
            clipPane.layoutXProperty().bind(
                    clipPane.getAudioClip().getTimelineMsPositionProperty().multiply(zoomFactor).subtract(clipStartOffset)
            );
        }
        timeLinePane.getChildren().add(clipPane);
        track.addClip(clipPane.getAudioClip());
    }

    public void registerClipPaneHandlers(ClipPane clipPane) {
        clipPane.setTrackPane(this);
        clipPane.setOnRemoveAction(onRemoveClip);
        clipPane.setOnTrimAction(onTrimAction);
        clipPane.setOnSelectionAction(onClipSelection);
        clipPane.setOnRightClickSelectionAction(onRightClickSelection);
        clipPane.setOnSplitAction(onSplitClip);

        if (clipPane.selectionEnabledPropertyProperty() != null && this.selectionToolEnabledProperty != null) {
            clipPane.selectionEnabledPropertyProperty().bind(this.selectionToolEnabledProperty);
        }
    }


    public void removeAudioClip(ClipPane clipPane) {
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

    public void bindClipStartOffset(FloatProperty clipStartOffset) {
        this.clipStartOffset = clipStartOffset;

        for (ClipPane clipPane : clipPanes) {
            if (clipPane == null || clipPane.getAudioClip() == null || zoomFactor == null || this.clipStartOffset == null) {
                continue;
            }
            clipPane.layoutXProperty().unbind();
            clipPane.layoutXProperty().bind(
                    clipPane.getAudioClip().getTimelineMsPositionProperty()
                            .multiply(zoomFactor)
                            .subtract(this.clipStartOffset)
            );
        }
        setupRulerPainting();
    }

    public void bindZoomFactor(FloatProperty zoomFactor) {
        this.zoomFactor.bind(zoomFactor);
        clipPanes.forEach(clipPane -> {
            if (clipPane != null) {
                clipPane.bindZoomFactor(zoomFactor);
            }
        });
        setupRulerPainting();
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.zoomFactor = new SimpleFloatProperty(1f);
        this.clipStartOffset = new SimpleFloatProperty(0f);
        this.selectionToolEnabledProperty = new SimpleBooleanProperty(false);
        initUi();

        for (ClipPane clipPane : clipPanes) {
            if (clipPane == null) {
                continue;
            }
            clipPane.rebuildAfterDeserialization(zoomFactor);
            registerClipPaneHandlers(clipPane);

            if (timeLinePane != null) {
                clipPane.prefHeightProperty().bind(timeLinePane.heightProperty());
                clipPane.minHeightProperty().bind(timeLinePane.heightProperty());
                clipPane.maxHeightProperty().bind(timeLinePane.heightProperty());
            }

            if (clipPane.getAudioClip() != null && zoomFactor != null && clipStartOffset != null) {
                clipPane.layoutXProperty().bind(
                        clipPane.getAudioClip().getTimelineMsPositionProperty()
                                .multiply(zoomFactor)
                                .subtract(clipStartOffset)
                );
            }
            timeLinePane.getChildren().add(clipPane);
        }
    }


    private void setupTimeLinePaneMouseEvents() {
        timeLinePane.setOnMousePressed(event -> {
            lastMouseX = event.getX();

            if (event.getButton() == MouseButton.PRIMARY) {
                if (onMousePressedAction != null) {
                    onMousePressedAction.handle(new ActionEvent(this, null));
                }
            } else if (event.getButton() == MouseButton.SECONDARY && !selectionToolEnabledProperty.get()) {
                timeLinePaneContextMenu.show(timeLinePane, event.getScreenX(), event.getScreenY());
                event.consume();
            }
        });
    }
}
