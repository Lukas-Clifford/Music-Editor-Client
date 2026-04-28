package app.musiceditorclient.services;

import app.musiceditorclient.EditorContext;
import app.musiceditorclient.infrastructure.AppFileUtils;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class TreeSampleService {
    private final EditorContext context;

    public TreeSampleService(EditorContext context) {
        this.context = context;
    }


    public TreeView<String> createSamplesTreeViewForDirectory(File rootDir) {
        TreeView<String> treeView = new TreeView<>();
        TreeItem<String> rootItem = new TreeItem<>(rootDir.getPath());
        rootItem.setExpanded(true);

        addWavFilesRecursively(rootItem, rootDir);

        treeView.setRoot(rootItem);
        treeView.setShowRoot(true);
        setupSelectedSampleBehavior(treeView);
        return treeView;
    }

    public HBox createTreeViewBox(TreeView<String> treeView, SplitPane splitPane) {
        Button removeButton = new Button("-");
        removeButton.setOnAction(event -> removeTreeView(treeView, splitPane));

        HBox box = new HBox(treeView, removeButton);
        box.setSpacing(6);
        HBox.setHgrow(treeView, javafx.scene.layout.Priority.ALWAYS);
        treeView.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    public void restoreSampleTreeViews(SplitPane samplesSplitPane) {
        samplesSplitPane.getItems().clear();

        File defaultSamples = AppFileUtils.resolveSamplesDir();

        if (defaultSamples.exists() && defaultSamples.isDirectory()) {
            TreeView<String> baseSamplesTreeView = createSamplesTreeViewForDirectory(defaultSamples);
            samplesSplitPane.getItems().add(
                    samplesSplitPane.getItems().size(),
                    createTreeViewBox(baseSamplesTreeView, samplesSplitPane)
            );
        }

        for (Path rootPath : context.ui().getSampleTreeRoots()) {
            File rootDir = rootPath.toFile();
            if (!rootDir.exists() || !rootDir.isDirectory()) {
                continue;
            }

            TreeView<String> newTreeView = createSamplesTreeViewForDirectory(rootDir);
            samplesSplitPane.getItems().add(
                    samplesSplitPane.getItems().size(),
                    createTreeViewBox(newTreeView, samplesSplitPane)
            );
        }

        normalizeSamplesSplitPane(samplesSplitPane);
    }


    public void normalizeSamplesSplitPane(SplitPane samplesSplitPane) {
        List<Node> treeItems = samplesSplitPane.getItems().stream()
                .filter(node -> node instanceof TreeView<?>)
                .toList();

        int itemCount = treeItems.size();
        if (itemCount < 2) {
            return;
        }

        double[] dividerPositions = new double[itemCount - 1];
        double step = 1.0 / itemCount;

        for (int i = 0; i < dividerPositions.length; i++) {
            dividerPositions[i] = step * (i + 1);
        }

        samplesSplitPane.setDividerPositions(dividerPositions);
    }

    public void addWavFilesRecursively(TreeItem<String> parentItem, File directory) {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        List<File> sortedFiles = new ArrayList<>(List.of(files));
        sortedFiles.sort((a, b) -> {
            if (a.isDirectory() && !b.isDirectory()) return -1;
            if (!a.isDirectory() && b.isDirectory()) return 1;
            return a.getName().compareToIgnoreCase(b.getName());
        });

        for (File file : sortedFiles) {
            if (file.isDirectory()) {
                TreeItem<String> dirItem = new TreeItem<>(file.getName());
                parentItem.getChildren().add(dirItem);
                addWavFilesRecursively(dirItem, file);
            } else if (file.getName().toLowerCase().endsWith(".wav")) {
                parentItem.getChildren().add(new TreeItem<>(file.getName()));
            }
        }
    }


    private void setupSelectedSampleBehavior(TreeView<String> treeView) {
        if (treeView == null) {
            return;
        }

        treeView.setOnMousePressed(event -> {
            if (event.getButton() != MouseButton.PRIMARY) {
                return;
            }

            TreeItem<String> item = treeView.getSelectionModel().getSelectedItem();
            if (item == null) {
                context.selection().setSelectedFile(null);
                return;
            }

            if (!item.getChildren().isEmpty()) {
                context.selection().setSelectedFile(null);
                return;
            }

            TreeItem<String> root = treeView.getRoot();
            if (root == null) {
                context.selection().setSelectedFile(null);
                return;
            }

            File current = new File(root.getValue());
            List<String> parents = new ArrayList<>();

            TreeItem<String> cursor = item;
            while (cursor != null && cursor != root) {
                parents.addFirst(cursor.getValue());
                cursor = cursor.getParent();
            }

            for (String segment : parents) {
                current = new File(current, segment);
            }

            context.selection().setSelectedFile(current.exists() && current.isFile() ? current : null);
        });
    }

    public void removeTreeView(TreeView<String> treeView, SplitPane samplesSplitPane) {
        if (treeView == null) {
            return;
        }

        Node parent = treeView.getParent();
        if (parent instanceof HBox box) {
            TreeItem<String> root = treeView.getRoot();
            if (root != null) {
                context.ui().getSampleTreeRoots().removeIf(rootPath -> rootPath.toString().equals(root.getValue()));
            }

            samplesSplitPane.getItems().remove(box);
            normalizeSamplesSplitPane(samplesSplitPane);
        }
    }

}
