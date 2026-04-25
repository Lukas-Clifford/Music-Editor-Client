package app.musiceditorclient.commands;


import app.musiceditorclient.view.ClipPane;
import javafx.event.Event;

import java.util.ArrayList;
import java.util.List;

public class MoveSelectionCommand extends EditorCommand{

    private List<ClipPane> clipPanes = new ArrayList<>();
    private final double seconds;

    public MoveSelectionCommand(Event event, double seconds) {
        super(event);
        this.seconds = seconds;
    }

    @Override
    public void execute() {
        services.playbackService().pausePlayback();

        if (seconds < 0) return;

        clipPanes.addAll(context.selection().getSelectedClips());

        clipPanes.forEach(clipPane -> clipPane.setClipStartPosition(clipPane.getAudioClip().getTimelineMsPosition() + (int) (seconds * 1000)));

        services.playbackService().reloadPlaybackEngine();
    }

    @Override
    public void undo() {

        services.playbackService().pausePlayback();

        clipPanes.forEach(clipPane -> clipPane.setClipStartPosition(clipPane.getAudioClip().getTimelineMsPosition() - (int) (seconds * 1000)));

        services.playbackService().reloadPlaybackEngine();

    }
}
