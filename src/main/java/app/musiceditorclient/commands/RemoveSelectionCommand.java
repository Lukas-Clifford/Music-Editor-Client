package app.musiceditorclient.commands;


import app.musiceditorclient.view.ClipPane;
import javafx.event.Event;

import java.util.ArrayList;
import java.util.List;

public class RemoveSelectionCommand extends EditorCommand {
    private final List<ClipPane> clipPanes = new ArrayList<>();

    public RemoveSelectionCommand(Event event) {
        super(event);
    }

    @Override
    public void execute() {

        services.playbackService().stopPlaybackForEdit();

        clipPanes.addAll(context.selection().getSelectedClips());

        clipPanes.forEach(clipPane ->
                context.project().getTrackPanes().forEach(trackPane -> {
                    if (trackPane.getClipPanes().contains(clipPane)) trackPane.removeAudioClip(clipPane);
                })
        );

        services.selectionService().clearSelection();

        services.playbackService().reloadPlaybackEngine();

    }

    @Override
    public void undo() {

        services.playbackService().stopPlaybackForEdit();

        clipPanes.forEach(clipPane ->
                clipPane.getTrackPane().addClipPane(clipPane)
        );

        services.playbackService().reloadPlaybackEngine();

    }

    @Override
    public String getDescription() {
        return "Removed selected clips " + this.clipPanes;
    }
}
