package app.musiceditorclient.services;

import app.musiceditorclient.EditorContext;
import app.musiceditorclient.models.Clip;
import app.musiceditorclient.view.ClipPane;
import app.musiceditorclient.view.TrackPane;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SelectionService {
    private final EditorContext context;

    public SelectionService(EditorContext context) {
        this.context = context;
    }

    public void setupSelectionContextMenu(
            EventHandler<ActionEvent> onMoveSelectedClips,
            EventHandler<ActionEvent> onMoveToSelectedClips,
            EventHandler<ActionEvent> onRemoveSelectedClips,
            EventHandler<ActionEvent> onCopySelectedClips,
            EventHandler<ActionEvent> onCutSelectedClips
    ) {

        MenuItem moveSelectedClipsMenuItem = new MenuItem("Move");
        moveSelectedClipsMenuItem.setOnAction(onMoveSelectedClips);

        MenuItem moveToSelectedClipsMenuItem = new MenuItem("Move to...");
        moveToSelectedClipsMenuItem.setOnAction(onMoveToSelectedClips);

        MenuItem removeSelectedClipsMenuItem = new MenuItem("Remove");
        removeSelectedClipsMenuItem.setOnAction(onRemoveSelectedClips);

        MenuItem copySelectedClipsMenuItem = new MenuItem("Copy");
        copySelectedClipsMenuItem.setOnAction(onCopySelectedClips);

        MenuItem cutSelectedClipsMenuItem = new MenuItem("Cut");
        cutSelectedClipsMenuItem.setOnAction(onCutSelectedClips);

        context.ui().getSelectionContextMenu().getItems().addAll(moveSelectedClipsMenuItem, moveToSelectedClipsMenuItem, removeSelectedClipsMenuItem, copySelectedClipsMenuItem, cutSelectedClipsMenuItem);

    }

    public void copySelectedClips() {
        context.selection().setCopiedClips(
                context.selection().getSelectedClips().stream()
                        .map(clipPane -> new ClipPane(
                                new Clip(
                                        clipPane.getAudioClip().getWavFile(),
                                        clipPane.getAudioClip().getTimelineMsPosition()
                                ),
                                clipPane.zoomFactor)
                        )
                        .collect(Collectors.toList())

        );
    }


    public void enableSelection() {
        context.selection().isSelectionToolActivePropertyProperty().set(true);
    }

    public void disableSelection() {
        context.selection().isSelectionToolActivePropertyProperty().set(false);
    }

    public void clearSelection() {
        for (ClipPane clipPane : context.selection().getSelectedClips()) {
            clipPane.setSelected(false);
        }
        context.selection().getSelectedClips().clear();
    }


    public void showSelectionContextMenu(MouseEvent event, Node contextNode) {

        if (!context.ui().getSelectionContextMenu().isShowing())
            context.ui().getSelectionContextMenu().show(contextNode, event.getScreenX(), event.getScreenY());
        else
            context.ui().getSelectionContextMenu().hide();

    }


    public void toggleClipSelection(ClipPane clipPane) {
        if (clipPane == null) {
            return;
        }

        if (clipPane.isSelected()) {
            clipPane.setSelected(false);
            context.selection().getSelectedClips().remove(clipPane);
        } else {
            clipPane.setSelected(true);
            if (!context.selection().getSelectedClips().contains(clipPane)) {
                context.selection().getSelectedClips().add(clipPane);
            }
        }
    }


}
