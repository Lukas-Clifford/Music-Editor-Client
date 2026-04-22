package app.musiceditorclient.view;

import app.musiceditorclient.models.Clip;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Optional;

public class ClipPane extends Pane implements Serializable, Comparable<ClipPane> {

    private static final long serialVersionUID = 1L;

    private Clip audioClip;

    private transient Label clipNameLabel;
    public transient FloatProperty zoomFactor = new SimpleFloatProperty(1f);

    private double[] offset = new double[2];

    private transient ContextMenu contextMenu;

    private transient EventHandler<ActionEvent> onMoveAction;
    private transient EventHandler<ActionEvent> onTrimAction;
    private transient EventHandler<ActionEvent> onRemoveAction;
    private transient EventHandler<ActionEvent> onSplitAction;

    private transient BooleanProperty selectionEnabledProperty = new SimpleBooleanProperty(false);
    private transient EventHandler<ActionEvent> onSelectionAction;
    private transient EventHandler<MouseEvent> onRightClickSelectionAction;
    private boolean selected = false;

    private double lastMouseX = 0d;


    public ClipPane(Clip clip, FloatProperty zoomFactor) {
        this.audioClip = clip;
        this.zoomFactor = new SimpleFloatProperty(1f);
        this.zoomFactor.bind(zoomFactor);
        initUi();
    }

    private void initUi() {
        clipNameLabel = new Label(audioClip.getWavFile().getName() + " : " + audioClip.getLength() + "ms");
        clipNameLabel.setMouseTransparent(true);
        getChildren().add(clipNameLabel);
        clipNameLabel.layoutBoundsProperty().addListener((observable, oldValue, newValue) -> {
            if (clipNameLabel.getWidth() > getWidth()) setClipNameLabel("");
        });

        if (zoomFactor != null) {
            prefWidthProperty().bind(zoomFactor.multiply(audioClip.getLength()));
            layoutXProperty().bind(audioClip.getTimelineMsPositionProperty().multiply(zoomFactor));
        }

        updateSelectionStyle();
        setupContextMenu();
    }

    public void bindZoomFactor(FloatProperty zoomFactor) {
        this.zoomFactor.bind(zoomFactor);
        if (this.zoomFactor != null && audioClip != null) {
            prefWidthProperty().bind(this.zoomFactor.multiply(audioClip.getLength()));
        }
    }

    private void setupContextMenu() {
        contextMenu = new ContextMenu();

        MenuItem moveItem = new MenuItem("Move");
        moveItem.setOnAction(event -> {
            if (onMoveAction != null) {
                onMoveAction.handle(event);
                return;
            }

            TextInputDialog dialog = new TextInputDialog(String.valueOf(audioClip.getTimelineMsPositionProperty().get() / 1000.0));
            dialog.setTitle("Move clip");
            dialog.setHeaderText("Enter destination second");
            dialog.setContentText("Second:");

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(value -> {
                try {
                    double seconds = Double.parseDouble(value.replace(",", "."));
                    setClipStartPosition((int) (seconds * 1000));
                } catch (NumberFormatException ignored) {
                    // Ignored
                }
            });
        });

        MenuItem trimItem = new MenuItem("Trim");
        trimItem.setOnAction(event -> {
            if (onTrimAction != null) onTrimAction.handle(new ActionEvent(this, null));
        });

        MenuItem removeItem = new MenuItem("Delete");
        removeItem.setOnAction(event -> {
            if (onRemoveAction != null) {
                onRemoveAction.handle(new ActionEvent(this, null));
            }
        });

        MenuItem splitItem = new MenuItem("Split");
        splitItem.setOnAction(event -> {
            if (onSplitAction != null) {
                onSplitAction.handle(new ActionEvent(this, null));
            }
        });


        contextMenu.getItems().addAll(moveItem, trimItem, removeItem,splitItem);

        setOnMousePressed(event -> {
            lastMouseX = event.getX();

            if (event.getButton() == MouseButton.PRIMARY) {
                if (selectionEnabledProperty != null && selectionEnabledProperty.get() && onSelectionAction != null) {
                    onSelectionAction.handle(new ActionEvent(this, null));
                }
            } else if (event.getButton() == MouseButton.SECONDARY && selected && onRightClickSelectionAction != null) {
                event.consume();
                onRightClickSelectionAction.handle(event);

            }
            else if (event.getButton() == MouseButton.SECONDARY && !selectionEnabledProperty.get()) {
                event.consume();
                contextMenu.show(this, event.getScreenX(), event.getScreenY());
            } else if (contextMenu.isShowing()) {
                contextMenu.hide();
            }
        });
    }

    public void rebuildAfterDeserialization(FloatProperty zoomFactor) {
        this.zoomFactor = new SimpleFloatProperty(1f);
        if (zoomFactor != null) {
            this.zoomFactor.bind(zoomFactor);
        }
        initUi();
    }

    public void setOnMoveAction(EventHandler<ActionEvent> onMoveAction) {
        this.onMoveAction = onMoveAction;
    }

    public void setOnTrimAction(EventHandler<ActionEvent> onTrimAction) {
        this.onTrimAction = onTrimAction;
    }

    public void setOnRemoveAction(EventHandler<ActionEvent> onRemoveAction) {
        this.onRemoveAction = onRemoveAction;
    }

    public void setOnSplitAction(EventHandler<ActionEvent> onSplitAction) {
        this.onSplitAction = onSplitAction;
    }

    public void setClipStartPosition(int milliseconds) {
        audioClip.setTimelineMsPosition(milliseconds);
    }

    public void setAudioStartMs(int audioStartMs) {
        audioClip.setAudioStartMs(audioStartMs);
    }

    public int getAudioStartMs() {
        return audioClip.getAudioStartMs();
    }

    public Clip getAudioClip() {
        return audioClip;
    }

    public Label getClipNameLabel() {
        return clipNameLabel;
    }

    public void setClipNameLabel(String clipName) {
        this.clipNameLabel.setText(clipName);
    }

    public void setOnSelectionAction(EventHandler<ActionEvent> onSelectionAction) {
        this.onSelectionAction = onSelectionAction;
    }
    public void setOnRightClickSelectionAction(EventHandler<MouseEvent> onRightClickSelectionAction) {
        this.onRightClickSelectionAction = onRightClickSelectionAction;
    }


    public BooleanProperty selectionEnabledPropertyProperty() {
        if (selectionEnabledProperty == null) {
            selectionEnabledProperty = new SimpleBooleanProperty(false);
        }
        return selectionEnabledProperty;
    }

    public void setSelectionEnabledProperty(BooleanProperty selectionEnabledProperty) {
        this.selectionEnabledProperty = selectionEnabledProperty;
    }

    public boolean isSelectionEnabled() {
        return selectionEnabledProperty != null && selectionEnabledProperty.get();
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        updateSelectionStyle();
    }

    private void updateSelectionStyle() {
        setStyle(selected ? "-fx-background-color:cornflowerblue;" : "-fx-background-color:aquamarine;");
    }

    public void refreshSize() {
        prefWidthProperty().unbind();
        prefWidthProperty().bind(zoomFactor.multiply(audioClip.getLength()));
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
    }

    @Override
    public int compareTo(ClipPane o) {
        return Double.compare(this.audioClip.getTimelineMsPosition(), o.audioClip.getTimelineMsPosition());
    }


    public double getLastMouseX() {
        return lastMouseX;
    }
}
