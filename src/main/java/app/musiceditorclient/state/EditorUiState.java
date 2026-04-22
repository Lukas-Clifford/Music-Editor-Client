package app.musiceditorclient.state;

import app.musiceditorclient.view.TimelineSeekerPane;
import app.musiceditorclient.view.TrackPane;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class EditorUiState {

    private final FloatProperty zoomFactor = new SimpleFloatProperty(0.10f);
    private final FloatProperty clipStartOffset = new SimpleFloatProperty(0f);

    private final List<Path> sampleTreeRoots = new ArrayList<>();
    private final ContextMenu addTrackContextMenu = new ContextMenu();
    private final ContextMenu selectionContextMenu = new ContextMenu();
    private TimelineSeekerPane timelineSeekerPane;


    public float getZoomFactor() {
        return zoomFactor.get();
    }

    public FloatProperty zoomFactorProperty() {
        return zoomFactor;
    }

    public void setZoomFactor(float zoomFactor) {
        this.zoomFactor.set(zoomFactor);
    }

    public float getClipStartOffset() {
        return clipStartOffset.get();
    }

    public FloatProperty clipStartOffsetProperty() {
        return clipStartOffset;
    }

    public void setClipStartOffset(float clipStartOffset) {
        this.clipStartOffset.set(clipStartOffset);
    }

    public List<Path> getSampleTreeRoots() {
        return sampleTreeRoots;
    }

    public ContextMenu getAddTrackContextMenu() {
        return addTrackContextMenu;
    }

    public ContextMenu getSelectionContextMenu() {
        return selectionContextMenu;
    }

    public TimelineSeekerPane getTimelineSeekerPane() {
        return timelineSeekerPane;
    }

    public void setTimelineSeekerPane(TimelineSeekerPane timelineSeekerPane) {
        this.timelineSeekerPane = timelineSeekerPane;
    }
}
