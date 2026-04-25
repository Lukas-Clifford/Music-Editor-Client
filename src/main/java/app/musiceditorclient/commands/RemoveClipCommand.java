package app.musiceditorclient.commands;

import app.musiceditorclient.EditorContext;
import app.musiceditorclient.EditorServices;
import app.musiceditorclient.models.Clip;
import app.musiceditorclient.view.ClipPane;
import app.musiceditorclient.view.TrackPane;
import javafx.event.Event;

public class RemoveClipCommand extends EditorCommand {

    private TrackPane trackPane;
    private ClipPane clipPane;

    public RemoveClipCommand(Event event) {
        super(event);
    }

    @Override
    public void execute() {
        services.playbackService().stopPlaybackForEdit();

        trackPane = ((TrackPane) event.getSource());
        clipPane = ((ClipPane) event.getTarget());

        trackPane.getTrack().removeClip(clipPane.getAudioClip());
        trackPane.getClipPanes().remove(clipPane);
        trackPane.getTimeLinePane().getChildren().remove(clipPane);

        services.playbackService().reloadPlaybackEngine();
    }

    @Override
    public void undo() {

        services.playbackService().stopPlaybackForEdit();

        trackPane.addClipPane(clipPane);

        services.playbackService().reloadPlaybackEngine();

    }

    @Override
    public String getDescription() {
        return "Removed " + this.clipPane.getAudioClip().getWavFile().getName() + " at " + this.clipPane.getAudioClip().getTimelineMsPosition();

    }
}
