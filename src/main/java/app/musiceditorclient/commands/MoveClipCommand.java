package app.musiceditorclient.commands;

import app.musiceditorclient.view.ClipPane;
import javafx.event.Event;

public class MoveClipCommand extends EditorCommand{

    private ClipPane clipPane;
    private int oldMsSeconds = -1;
    private int newMsSeconds = -1;

    public MoveClipCommand(Event event) {
        super(event);
    }

    @Override
    public void execute() {
        services.playbackService().pausePlayback();

        if (newMsSeconds == -1) newMsSeconds =(int)( services.dialogService().getSecondsToMoveSelection() * 1000);

        clipPane = (ClipPane) event.getTarget();

        if (oldMsSeconds == -1) oldMsSeconds = clipPane.getAudioClip().getTimelineMsPosition();

        clipPane.getAudioClip().setTimelineMsPosition(newMsSeconds);

        services.playbackService().reloadPlaybackEngine();
    }

    @Override
    public void undo() {
        services.playbackService().pausePlayback();

        clipPane.getAudioClip().setTimelineMsPosition(oldMsSeconds);

        services.playbackService().reloadPlaybackEngine();
    }

    @Override
    public String getDescription() {
        return "Moved clip " + clipPane.getAudioClip().getWavFile().getName() + " from " + ((double)oldMsSeconds/1000) + " to " + ((double)newMsSeconds/1000);
    }
}
