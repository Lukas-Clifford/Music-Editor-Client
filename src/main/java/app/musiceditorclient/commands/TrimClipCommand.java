package app.musiceditorclient.commands;

import app.musiceditorclient.EditorContext;
import app.musiceditorclient.EditorServices;
import app.musiceditorclient.models.Clip;
import app.musiceditorclient.models.TrimClipDialogResult;
import app.musiceditorclient.view.ClipPane;
import javafx.event.Event;

public class TrimClipCommand extends EditorCommand{
    private ClipPane clipPane;
    private TrimClipDialogResult values;

    private int oldLength;
    private int oldAudioStart;
    private int oldTimeLineMsPos;

    private int newAudioStart;
    private int newLength;


    public TrimClipCommand(Event event, TrimClipDialogResult values) {
        super(event);
        this.values = values;
    }

    @Override
    public void execute() {
        services.playbackService().stopPlaybackForEdit();

        this.clipPane = (ClipPane) event.getTarget();

        Clip clip = clipPane.getAudioClip();
        oldLength = clip.getLength();
        oldAudioStart = clip.getAudioStartMs();
        oldTimeLineMsPos = clip.getTimelineMsPosition();

        newAudioStart = Math.max(0, oldAudioStart + values.frontMs());
        newLength = Math.max(0, oldLength - values.frontMs() - values.backMs());

        clip.setAudioStartMs(newAudioStart);
        clip.setTimelineMsPosition(oldTimeLineMsPos + values.frontMs());
        clip.setLength(newLength);

        clipPane.setClipNameLabel(clip.getWavFile().getName() + " : " + clip.getLength() + "ms");
        clipPane.refreshSize();

        services.playbackService().reloadPlaybackEngine();


    }

    @Override
    public void undo() {
        services.playbackService().stopPlaybackForEdit();

        Clip clip = clipPane.getAudioClip();
        clip.setAudioStartMs(oldAudioStart);
        clip.setTimelineMsPosition(oldTimeLineMsPos);
        clip.setLength(oldLength);

        clipPane.setClipNameLabel(clip.getWavFile().getName() + " : " + clip.getLength() + "ms");
        clipPane.refreshSize();
        services.playbackService().reloadPlaybackEngine();

    }

    @Override
    public String getDescription() {
        return "Trimmed clip " + this.values.frontMs() + " seconds from the frond and " + this.values.backMs() + " from the back";
    }
}
