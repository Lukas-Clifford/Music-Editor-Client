package app.musiceditorclient.commands;


import app.musiceditorclient.models.Clip;
import app.musiceditorclient.view.ClipPane;
import app.musiceditorclient.view.TrackPane;
import javafx.event.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PasteCopiedClipsCommand extends EditorCommand{
    private TrackPane trackPane;
    private List<ClipPane> copiedClipPanes = new ArrayList<>();
    private int startMs = -1;
    private int offset = -1;


    public PasteCopiedClipsCommand(Event event) {
        super(event);
    }

    public PasteCopiedClipsCommand(Event event, int startMs) {
        super(event);
        this.startMs = startMs;
    }

    @Override
    public void execute() {
        if (!context.selection().getSelectedClips().isEmpty() || !context.selection().getCopiedClips().isEmpty()) {
            services.playbackService().stopPlaybackForEdit();

            trackPane = ((TrackPane) event.getSource());
            startMs = (startMs == -1) ? services.trackService().calculateClipStartMs(trackPane) : startMs;

            if (offset == -1)
                offset = context.selection().getCopiedClips()
                    .stream()
                    .sorted()
                    .toList()
                    .getFirst()
                    .getAudioClip()
                    .getTimelineMsPosition() - startMs;


            copiedClipPanes = context.selection().getCopiedClips().stream()
                    .map(clipPane -> new ClipPane(
                            new Clip(
                                    clipPane.getAudioClip().getWavFile(),
                                    clipPane.getAudioClip().getTimelineMsPosition() - offset
                                    ),
                            clipPane.zoomFactor)
                    )
                    .collect(Collectors.toList());

            copiedClipPanes.forEach(clipPane -> trackPane.addClipPane(clipPane));

            services.playbackService().reloadPlaybackEngine();
        }
    }

    @Override
    public void undo() {
        services.playbackService().stopPlaybackForEdit();

        copiedClipPanes.forEach(clipPane -> trackPane.removeAudioClip(clipPane));

        services.playbackService().reloadPlaybackEngine();


    }

    @Override
    public String getDescription() {
        return "Pasted " + copiedClipPanes + " at " + this.startMs/1000;
    }
}
