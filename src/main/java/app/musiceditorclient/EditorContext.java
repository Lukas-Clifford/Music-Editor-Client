package app.musiceditorclient;

import app.musiceditorclient.services.*;
import app.musiceditorclient.state.EditorUiState;
import app.musiceditorclient.state.PlaybackState;
import app.musiceditorclient.state.ProjectState;
import app.musiceditorclient.state.SelectionState;

public class EditorContext {

    private EditorUiState editorUiState;
    private PlaybackState playbackState;
    private ProjectState projectState;
    private SelectionState selectionState;

    public EditorContext() {

        editorUiState = new EditorUiState();
        playbackState = new PlaybackState();
        projectState = new ProjectState();
        selectionState = new SelectionState();

    }

    public EditorUiState ui() { return editorUiState; }

    public PlaybackState playback() { return playbackState; }

    public ProjectState project() { return projectState; }

    public SelectionState selection() { return selectionState; }
























}





