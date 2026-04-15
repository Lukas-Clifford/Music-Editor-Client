package app.musiceditorclient.view;

import app.musiceditorclient.models.Clip;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Optional;

public class ClipPane extends Pane implements Serializable {

    private static final long serialVersionUID = 1L;

    private Clip audioClip;

    private transient Label clipNameLabel;
    public transient FloatProperty zoomFactor = new SimpleFloatProperty(1f);

    private double[] offset = new double[2];

    private transient ContextMenu contextMenu;

    private transient EventHandler<ActionEvent> onMoveAction;
    private transient EventHandler<ActionEvent> onTrimAction;
    private transient EventHandler<ActionEvent> onRemoveAction;

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

        prefWidthProperty().bind(zoomFactor.multiply(audioClip.getLength()));
        layoutXProperty().bind(audioClip.getTimelineMsPositionProperty().multiply(zoomFactor));

        setStyle("-fx-background-color:aquamarine;");
        setupContextMenu();
    }

    private void setupContextMenu() {
        contextMenu = new ContextMenu();

        MenuItem moveItem = new MenuItem("Mover");
        moveItem.setOnAction(event -> {
            if (onMoveAction != null) {
                onMoveAction.handle(event);
                return;
            }

            TextInputDialog dialog = new TextInputDialog(String.valueOf(audioClip.getTimelineMsPositionProperty().get() / 1000.0));
            dialog.setTitle("Mover clip");
            dialog.setHeaderText("Introducir segundo destino");
            dialog.setContentText("Segundo:");

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

        MenuItem trimItem = new MenuItem("Recortar");
        trimItem.setOnAction(event -> {
            if (onTrimAction != null) onTrimAction.handle(event);
        });

        MenuItem removeItem = new MenuItem("Eliminar");
        removeItem.setOnAction(event -> {
            if (onRemoveAction != null) onRemoveAction.handle(event);
            else if (getParent() instanceof Pane parent) {
                parent.getChildren().remove(this);
            }
        });

        contextMenu.getItems().addAll(moveItem, trimItem, removeItem);

        setOnMousePressed(event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                contextMenu.show(this, event.getScreenX(), event.getScreenY());
                event.consume();
            } else if (contextMenu.isShowing()) {
                contextMenu.hide();
            }
        });
    }

    public void rebuildAfterDeserialization(FloatProperty zoomFactor) {
        this.zoomFactor = new SimpleFloatProperty(1f);
        this.zoomFactor.bind(zoomFactor);
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

    public void setClipStartPosition(int milliseconds) {
        audioClip.setTimelineMsPosition(milliseconds);
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

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
    }
}
