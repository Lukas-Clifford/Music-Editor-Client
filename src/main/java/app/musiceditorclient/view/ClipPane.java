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

import java.util.Optional;

public class ClipPane extends Pane {

    private Label clipNameLabel;
    private Clip audioClip;

    public FloatProperty zoomFactor;

    private double[] offset = new double[2];

    private final ContextMenu contextMenu = new ContextMenu();

    // Context menu actions exposed for controller-level handling
    private EventHandler<ActionEvent> onMoveAction;
    private EventHandler<ActionEvent> onTrimAction;
    private EventHandler<ActionEvent> onRemoveAction;

    public ClipPane(Clip clip, FloatProperty zoomFactor) {

        this.audioClip = clip;

        // LABEL
        this.clipNameLabel = new Label(clip.getWavFile().getName() + " : " + (clip.getLength()) + "ms");
        this.clipNameLabel.setMouseTransparent(true);
        this.getChildren().add(clipNameLabel);
        clipNameLabel.layoutBoundsProperty().addListener((observable, oldValue, newValue) -> {
            if (this.clipNameLabel.getWidth() > this.getWidth()) this.setClipNameLabel("");
        });

        // PROPERTY BINDING
        this.zoomFactor = new SimpleFloatProperty(1f);
        this.zoomFactor.bind(zoomFactor);

        this.prefWidthProperty().bind(this.zoomFactor.multiply(clip.getLength()));
        this.layoutXProperty().bind(this.audioClip.getTimelineMsPositionProperty().multiply(zoomFactor));

        this.setStyle("-fx-background-color:aquamarine;");
        setupContextMenu();
    }

    private void setupContextMenu() {
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

        this.setOnMousePressed(event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                contextMenu.show(this, event.getScreenX(), event.getScreenY());
                event.consume();
            } else if (contextMenu.isShowing()) {
                contextMenu.hide();
            }
        });
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

    public Clip getAudioClip(){
        return audioClip;
    }

    public Label getClipNameLabel() {
        return clipNameLabel;
    }

    public void setClipNameLabel(String clipName) {
        this.clipNameLabel.setText(clipName);
    }
}
