package app.musiceditorclient.state;

import app.musiceditorclient.view.ClipPane;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SelectionState {

    private BooleanProperty isSelectionToolActiveProperty = new SimpleBooleanProperty(false);
    private List<ClipPane> selectedClips = new ArrayList<>();
    private List<ClipPane> copiedClips = new ArrayList<>();
    private File selectedFile;


    public boolean isIsSelectionToolActiveProperty() {
        return isSelectionToolActiveProperty.get();
    }

    public BooleanProperty isSelectionToolActivePropertyProperty() {
        return isSelectionToolActiveProperty;
    }

    public void setIsSelectionToolActiveProperty(boolean isSelectionToolActiveProperty) {
        this.isSelectionToolActiveProperty.set(isSelectionToolActiveProperty);
    }

    public List<ClipPane> getSelectedClips() {
        return selectedClips;
    }

    public void setSelectedClips(List<ClipPane> selectedClips) {
        this.selectedClips = selectedClips;
    }

    public List<ClipPane> getCopiedClips() {
        return copiedClips;
    }

    public void setCopiedClips(List<ClipPane> copiedClips) {
        this.copiedClips = copiedClips;
    }

    public File getSelectedFile() {
        return selectedFile;
    }

    public void setSelectedFile(File selectedFile) {
        this.selectedFile = selectedFile;
    }
}
