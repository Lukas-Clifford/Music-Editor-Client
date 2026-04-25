package app.musiceditorclient.commands;

import app.musiceditorclient.EditorContext;
import app.musiceditorclient.EditorServices;
import app.musiceditorclient.view.TrackPane;
import javafx.event.Event;

public class RemoveTrackCommand extends EditorCommand {

    private TrackPane trackPane;

    public RemoveTrackCommand(Event event) {
        super(event);
    }

    @Override
    public void execute() {
        services.playbackService().stopPlaybackForEdit();

        trackPane = (TrackPane) event.getSource();
        context.project().getTrackPanes().remove(trackPane);

        services.playbackService().reloadPlaybackEngine();
    }

    @Override
    public void undo() {
        services.playbackService().stopPlaybackForEdit();

        context.project().getTrackPanes().add(trackPane);

        services.playbackService().reloadPlaybackEngine();

    }
}
