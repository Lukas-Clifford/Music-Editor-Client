package app.musiceditorclient.commands;


import app.musiceditorclient.view.TrackPane;
import javafx.event.Event;
import javafx.scene.control.TableView;

public class MoveTrackUpCommand extends EditorCommand{

    private TrackPane trackPane;
    private int index;

    public MoveTrackUpCommand(Event event, TableView<TrackPane> tracksTableView) {
        super(event);
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

        services.playbackService().reloadPlaybackEngine();
    }

    @Override
    public void undo() {
        services.playbackService().stopPlaybackForEdit();

        context.project().getTrackPanes().remove(trackPane);
        context.project().getTrackPanes().add(index, trackPane);

        services.playbackService().reloadPlaybackEngine();
    }
}
