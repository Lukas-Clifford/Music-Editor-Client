package app.musiceditorclient.commands;

import app.musiceditorclient.view.TrackPane;
import javafx.event.Event;
import javafx.scene.control.TableView;

public class AddTrackCommand extends EditorCommand{
    private final TableView<TrackPane> tracksTableView;
    private TrackPane trackPane;

    public AddTrackCommand(Event event, TableView<TrackPane> tracksTableView) {
        super(event);
        this.tracksTableView = tracksTableView;
    }

    @Override
    public void execute() {
        services.playbackService().stopPlaybackForEdit();

        trackPane = new TrackPane(context.ui().zoomFactorProperty());

        services.trackService().configureTrackPane(trackPane);
        context.project().getTrackPanes().add(trackPane);
        services.playbackService().reloadPlaybackEngine();

        tracksTableView.refresh();

    }

    @Override
    public void undo() {
        services.playbackService().stopPlaybackForEdit();

        context.project().getTrackPanes().remove(trackPane);

        services.playbackService().reloadPlaybackEngine();
    }

    @Override
    public String getDescription() {
        return "Added Track at position " + (context.project().getTrackPanes().size()-1);
    }
}
