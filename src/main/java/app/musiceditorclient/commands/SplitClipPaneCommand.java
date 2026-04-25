package app.musiceditorclient.commands;

import app.musiceditorclient.EditorContext;
import app.musiceditorclient.EditorServices;
import app.musiceditorclient.models.Clip;
import app.musiceditorclient.view.ClipPane;
import app.musiceditorclient.view.TrackPane;
import javafx.event.Event;

import java.io.File;

public class SplitClipPaneCommand extends EditorCommand {
    private TrackPane trackPane;
    private ClipPane originalClipPane;
    private ClipPane frontClipPane;
    private ClipPane backClipPane;
    private double splittingPoint = -1;

    public SplitClipPaneCommand(Event event) {
        super(event);
    }

    @Override
    public void execute() {

        services.playbackService().stopPlaybackForEdit();

        this.trackPane = (TrackPane) event.getSource();
        this.originalClipPane = (ClipPane) event.getTarget();

        if (splittingPoint == -1)
            splittingPoint = originalClipPane.getLastMouseX();

        // Width = ZoomFactor * msLength -> l = w/z
        int splittingMs = (int) (splittingPoint/ context.ui().zoomFactorProperty().get());

        File wavFile = originalClipPane.getAudioClip().getWavFile();
        int timelineMsPosition = originalClipPane.getAudioClip().getTimelineMsPosition();

        Clip frontPartClip = new Clip(wavFile, timelineMsPosition);
        frontPartClip.setLength(splittingMs);
        frontClipPane = new ClipPane(frontPartClip, context.ui().zoomFactorProperty());

        Clip backPartClip = new Clip(wavFile, timelineMsPosition + splittingMs);
        backPartClip.setLength(originalClipPane.getAudioClip().getLength()-splittingMs);
        backClipPane = new ClipPane(backPartClip, context.ui().zoomFactorProperty());

        trackPane.addClipPane(frontClipPane);
        trackPane.addClipPane(backClipPane);

        trackPane.removeAudioClip(originalClipPane);

        services.playbackService().reloadPlaybackEngine();
    }

    @Override
    public void undo() {

        services.playbackService().stopPlaybackForEdit();

        trackPane.removeAudioClip(frontClipPane);
        trackPane.removeAudioClip(backClipPane);

        trackPane.addClipPane(originalClipPane);

        services.playbackService().reloadPlaybackEngine();

    }
}
