package app.musiceditorclient.commands;

import app.musiceditorclient.view.TrackPane;
import javafx.event.Event;
import javafx.scene.control.TableView;

public class RemoveTrackCommand extends EditorCommand {

    private TrackPane trackPane;
    private int index;
    private TableView<TrackPane> trackPaneTableView;

    public RemoveTrackCommand(Event event, TableView<TrackPane> tracksTableView) {
        super(event);
        this.trackPaneTableView = tracksTableView;
    }

    @Override
    public void execute() {
        services.playbackService().stopPlaybackForEdit();

        trackPane = (TrackPane) event.getSource();
        index = context.project().getTrackPanes().indexOf(trackPane);

        context.project().getTrackPanes().remove(trackPane);

        trackPaneTableView.refresh();
        services.playbackService().reloadPlaybackEngine();
    }

    @Override
    public void undo() {
        services.playbackService().stopPlaybackForEdit();

        context.project().getTrackPanes().add(trackPane);

        trackPaneTableView.refresh();
        services.playbackService().reloadPlaybackEngine();

    }

    @Override
    public String getDescription() {
        return "Removed track " + trackPane + " at index " + index;
    }
}
