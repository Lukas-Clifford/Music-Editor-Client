package app.musiceditorclient.state;

import app.musiceditorclient.view.TrackPane;
import javafx.scene.control.TreeView;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ProjectState {

    private List<TrackPane> trackPanes = new ArrayList<>();
    private Path currentProject;
    private TreeView<String> samplesFilesTreeView;
    private Consumer<Path> onProjectLoadedListener;


    public List<TrackPane> getTrackPanes() {
        return trackPanes;
    }

    public void setTrackPanes(List<TrackPane> trackPanes) {
        this.trackPanes = trackPanes;
    }

    public Path getCurrentProject() {
        return currentProject;
    }

    public void setCurrentProject(Path currentProject) {
        this.currentProject = currentProject;
    }

    public TreeView<String> getSamplesFilesTreeView() {
        return samplesFilesTreeView;
    }

    public void setSamplesFilesTreeView(TreeView<String> samplesFilesTreeView) {
        this.samplesFilesTreeView = samplesFilesTreeView;
    }

    public Consumer<Path> getOnProjectLoadedListener() {
        return onProjectLoadedListener;
    }

    public void setOnProjectLoadedListener(Consumer<Path> onProjectLoadedListener) {
        this.onProjectLoadedListener = onProjectLoadedListener;
    }
}
