package app.musiceditorclient.commands;

import app.musiceditorclient.EditorContext;
import app.musiceditorclient.EditorServices;
import app.musiceditorclient.models.Clip;
import app.musiceditorclient.models.RecursiveClipDialogResult;
import app.musiceditorclient.view.ClipPane;
import app.musiceditorclient.view.TrackPane;
import javafx.event.Event;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AddReiterativeClipCommand extends EditorCommand{
    private TrackPane trackPane;
    private File file;
    private RecursiveClipDialogResult values;
    private List<ClipPane> clipPanes;

    public AddReiterativeClipCommand( Event event, File file, RecursiveClipDialogResult values) {
        super(event);
        this.file = file;
        this.values = values;
        this.clipPanes = new ArrayList<>();
    }


    @Override
    public void execute() {
        services.playbackService().stopPlaybackForEdit();

        this.trackPane = (TrackPane) event.getSource();

        int startingMs = (int) (values.startingSeconds() * 1000f);
        int stepMs = (int) (values.secondsBetweenRepetition() * 1000f);

        for (int i = 0; i < values.numberOfRepetitions(); i++) {
            ClipPane clipPane = new ClipPane(new Clip(file, startingMs + (i * stepMs)), context.ui().zoomFactorProperty());
            clipPanes.add(clipPane);
            trackPane.addClipPane(clipPane);
        }

        services.playbackService().reloadPlaybackEngine();

    }

    @Override
    public void undo() {
        services.playbackService().stopPlaybackForEdit();

        clipPanes.forEach(clipPane -> trackPane.removeAudioClip(clipPane));

        services.playbackService().reloadPlaybackEngine();
    }

    @Override
    public String getDescription() {
        return this.file.getName() + " added " + this.values.numberOfRepetitions() + " times at " +
                this.values.startingSeconds() + " separated by " + this.values.secondsBetweenRepetition() + " seconds";
    }
}
