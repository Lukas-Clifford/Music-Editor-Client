package app.musiceditorclient.commands;


import app.musiceditorclient.view.TrackPane;
import javafx.event.Event;
import javafx.scene.control.TableView;

public class MoveTrackUpCommand extends EditorCommand{

    private TrackPane trackPane;
    private int index;
    private TableView<TrackPane> tableView;

    public MoveTrackUpCommand(Event event, TableView<TrackPane> tracksTableView) {
        super(event);
        this.tableView = tracksTableView;
    }

    @Override
    public void execute() {
        services.playbackService().stopPlaybackForEdit();

        if (trackPane == null)
            trackPane = ((TrackPane) event.getSource());
        index = context.project().getTrackPanes().indexOf(trackPane);

        if (index != 0) {
            context.project().getTrackPanes().remove(trackPane);
            context.project().getTrackPanes().add(index-1, trackPane);
        }
        tableView.refresh();
        services.playbackService().reloadPlaybackEngine();
    }

    @Override
    public void undo() {
        services.playbackService().stopPlaybackForEdit();

        context.project().getTrackPanes().remove(trackPane);
        context.project().getTrackPanes().add(index, trackPane);
        tableView.refresh();
        services.playbackService().reloadPlaybackEngine();
    }

    @Override
    public String getDescription() {
        return "Moved track " + this.trackPane + " up by one";
    }
}
