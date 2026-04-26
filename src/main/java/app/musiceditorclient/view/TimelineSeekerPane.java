package app.musiceditorclient.view;

import javafx.application.Platform;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

public class TimelineSeekerPane extends Pane {

    public SimpleFloatProperty seekerPosition; // place it aims in timeline (Frames)
    public FloatProperty zoomFactor = new SimpleFloatProperty(0f);
    private final SimpleFloatProperty songLengthProperty;
    private Runnable onSeekRequested;

    private final ProgressBar seekerProgressBar;
    private final Label seekerPosLabel;
    private final Line seekerLine;
    VBox container;



    public TimelineSeekerPane(FloatProperty zoomFactor) {
        this.zoomFactor.bind(zoomFactor);
        prefWidthProperty().bind(zoomFactor.multiply(600_000)); // max length 10 mins
        this.setPrefHeight(60);

        seekerPosition = new SimpleFloatProperty(1f);
        songLengthProperty = new SimpleFloatProperty(120f);

        seekerProgressBar = new ProgressBar(0);
        seekerProgressBar.prefWidthProperty().bind(zoomFactor.multiply(songLengthProperty));
        seekerPosLabel = new Label(formatSeconds(seekerPosition.get()));

        container = new VBox(8,  seekerPosLabel,seekerProgressBar);
        container.setLayoutX(0);
        container.setLayoutY(0);

        seekerLine = new Line();
        seekerLine.setStartY(0);
        seekerLine.setEndY(1000);
        seekerLine.setStroke(Color.BLACK);
        seekerLine.setStrokeWidth(2);
        seekerLine.layoutXProperty().bind(container.layoutXProperty());

        Rectangle rendererBounds = new Rectangle();
        rendererBounds.widthProperty().bind(this.widthProperty());
        rendererBounds.setHeight(1000);
        setClip(rendererBounds);


        seekerPosition.addListener((obs, oldValue, newValue) -> {
            updateSeeker(newValue.floatValue());
        });

        this.getChildren().addAll(container, seekerLine );
    }

    private void updateSeeker(float frames) {
        Runnable update = () -> {
            double progress = (songLengthProperty.get() <= 0f)  ?  0 : Math.min(1.0, Math.max(0,frames)/ (songLengthProperty.get()*44.1));

            seekerProgressBar.setProgress(progress);
            seekerPosLabel.setText(formatSeconds(Math.max(0,frames)));

            seekerLine.setStartX((zoomFactor.get()*(frames/44.1))-container.getLayoutX());
            seekerLine.setEndX((zoomFactor.get()*(frames/44.1))-container.getLayoutX());


        };

        if (Platform.isFxApplicationThread()) {
            update.run();
        } else {
            Platform.runLater(update);
        }
    }

    private String formatSeconds(float frames) {
        return String.format("%.2f s", (frames) / 44100);
    }

    public void bindSongLengthProperty(SimpleFloatProperty songLengthProperty) {
        this.songLengthProperty.bind(songLengthProperty);
    }

    public void bindStartOffsetProperty(FloatProperty startOffset) {
        container.layoutXProperty().bind(zoomFactor.subtract(startOffset));
    }
}
