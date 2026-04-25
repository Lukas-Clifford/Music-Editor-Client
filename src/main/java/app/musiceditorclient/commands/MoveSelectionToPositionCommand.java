package app.musiceditorclient.commands;

import app.musiceditorclient.view.ClipPane;
import javafx.event.Event;

import java.util.ArrayList;
import java.util.List;

public class MoveSelectionToPositionCommand extends EditorCommand{
    private double seconds;
    private int offset;
    private List<ClipPane> clipPanes = new ArrayList<>();

    public MoveSelectionToPositionCommand(Event event, double seconds) {
        super(event);
        this.seconds = seconds;
    }

    @Override
    public void execute() {
        services.playbackService().pausePlayback();

        if (seconds < 0) return;

        clipPanes.addAll(context.selection().getSelectedClips());

        offset = clipPanes
                .stream()
                .sorted()
                .toList()
                .getFirst()
                .getAudioClip()
                .getTimelineMsPosition() - (int) (seconds * 1000);

        clipPanes.forEach(clipPane ->
                clipPane.setClipStartPosition(clipPane.getAudioClip().getTimelineMsPosition() - offset)
        );

        services.playbackService().reloadPlaybackEngine();

    }

    @Override
    public void undo() {
        services.playbackService().pausePlayback();

        clipPanes.forEach(clipPane ->
                    clipPane.setClipStartPosition(clipPane.getAudioClip().getTimelineMsPosition() + offset)
        );

        services.playbackService().reloadPlaybackEngine();
    }

    @Override
    public String getDescription() {
        return "Moved " + this.clipPanes + " to " + this.seconds;
    }
}
