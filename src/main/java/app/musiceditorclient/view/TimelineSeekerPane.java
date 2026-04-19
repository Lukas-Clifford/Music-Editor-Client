package app.musiceditorclient.view;

import javafx.beans.binding.Bindings;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.event.Event;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;

public class TimelineSeekerPane extends Pane {

    public SimpleIntegerProperty seekerPosition; // place it aims in timeline (Milliseconds)
    private ProgressBar seekerProgressBar;


    public FloatProperty zoomFactor;
    private SimpleIntegerProperty songLengthProperty;
    private Runnable onSeekRequested;


    public TimelineSeekerPane(FloatProperty zoomFactor) {

        prefWidthProperty().bind(zoomFactor.multiply(600_000)); // max length 10 mins
        prefHeightProperty().bind(zoomFactor.multiply(250));


        // Seeker config
        seekerPosition = new SimpleIntegerProperty(1);
        songLengthProperty = new SimpleIntegerProperty(120);
        seekerProgressBar = new ProgressBar();

        seekerProgressBar.prefWidthProperty().bind(zoomFactor.multiply(songLengthProperty));
        seekerProgressBar.prefHeightProperty().bind(this.prefHeightProperty());


        seekerProgressBar.progressProperty().bind(
                Bindings.createDoubleBinding(
                        () -> {
                            int length = Math.max(1, songLengthProperty.get());
                            return Math.max(0d, Math.min(1d, seekerPosition.get() / (double) length));
                        },
                        seekerPosition,
                        songLengthProperty
                )
        );

        this.getChildren().add(seekerProgressBar);

    }

    public void bindSongLengthProperty(SimpleIntegerProperty songLengthProperty) {
        this.songLengthProperty.bind(songLengthProperty);
    }

}
