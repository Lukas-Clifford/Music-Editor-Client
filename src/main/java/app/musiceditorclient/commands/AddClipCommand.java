package app.musiceditorclient.commands;

import app.musiceditorclient.EditorContext;
import app.musiceditorclient.EditorServices;
import app.musiceditorclient.models.Clip;
import app.musiceditorclient.view.ClipPane;
import app.musiceditorclient.view.TrackPane;
import javafx.event.Event;

import java.io.File;

public class AddClipCommand extends EditorCommand{
    private ClipPane clipPane;
    private TrackPane trackPane;
    private final int clipStartingPos;
    private final File file;

    public AddClipCommand(Event event, File file, int clipStartingPos) {
        super(event);
        this.file = file;
        this.clipStartingPos = clipStartingPos;
    }

    @Override
    public void execute() {

        services.playbackService().stopPlaybackForEdit();

        this.trackPane = ((TrackPane) event.getSource());

        this.clipPane = new ClipPane(new Clip(this.file, this.clipStartingPos), context.ui().zoomFactorProperty());
        this.trackPane.addClipPane(clipPane);

        services.playbackService().reloadPlaybackEngine();

    }

    @Override
    public void undo() {

        services.playbackService().stopPlaybackForEdit();

        this.trackPane.getTrack().removeClip(this.clipPane.getAudioClip());
        this.trackPane.getClipPanes().remove(this.clipPane);
        this.trackPane.getTimeLinePane().getChildren().remove(this.clipPane);

        services.playbackService().reloadPlaybackEngine();


    }
}
