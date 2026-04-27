package app.musiceditorclient;

import app.musiceditorclient.services.*;

public class EditorServices {

    private final DialogService dialogService;
    private final PlaybackService playbackService;
    private final ProjectPersistenceService projectPersistenceService;
    private final SelectionService selectionService;
    private final TrackService trackService;
    private final TreeSampleService treeSampleService;
    private final AboutWindowService aboutWindowService;

    public EditorServices(EditorContext context) {
        dialogService               = new DialogService(context);
        playbackService             = new PlaybackService(context);
        projectPersistenceService   = new ProjectPersistenceService(context);
        selectionService            = new SelectionService(context);
        trackService                = new TrackService(context);
        treeSampleService           = new TreeSampleService(context);
        aboutWindowService          = new AboutWindowService(context);
    }


    public DialogService dialogService() { return dialogService; }

    public PlaybackService playbackService() { return playbackService; }

    public ProjectPersistenceService projectPersistenceService() { return projectPersistenceService; }

    public SelectionService selectionService() { return selectionService; }

    public TrackService trackService() { return trackService; }

    public TreeSampleService treeSampleService() { return treeSampleService; }

    public AboutWindowService aboutWindowService() { return aboutWindowService; }

}
