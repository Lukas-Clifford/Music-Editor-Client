package app.musiceditorclient;

import app.musiceditorclient.infrastructure.AppFileUtils;
import app.musiceditorclient.models.Clip;
import app.musiceditorclient.view.ClipPane;
import app.musiceditorclient.view.TimelineSeekerPane;
import app.musiceditorclient.view.TrackPane;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class MainController {

    public TableView<TrackPane> tracksTableView;
    public TableColumn<TrackPane, Pane> trackControlPaneColumn;
    public TableColumn<TrackPane, Pane> trackTimelinePaneColum;

    public FloatProperty zoomFactor = new SimpleFloatProperty(0.10f);
    public FloatProperty clipStartOffset = new SimpleFloatProperty(0f);
    public TimelineSeekerPane timelineSeekerPane;
    public Button addTrackButton;
    public Button playButton;
    public TreeView<String>  samplesFilesTreeView;
    public Button addTreeViewButton;
    public SplitPane samplesSplitPane;
    public Button selectionToolButton;

    private TreeView<String> baseSamplesTreeView;

    List<TrackPane> trackPanes = new ArrayList<>();
    PlaybackEngine pe;

    private final ContextMenu addTrackContextMenu = new ContextMenu();
    private final ContextMenu selectionContextMenu = new ContextMenu();


    private Path currentProject;

    private Consumer<Path> onProjectLoadedListener;

    private File selectedFile;

    private final List<Path> sampleTreeRoots = new ArrayList<>();

    private final BooleanProperty isSelectionToolActiveProperty = new SimpleBooleanProperty(false);
    private final List<ClipPane> selectedClips = new ArrayList<>();

    private final List<ClipPane> copiedClips = new ArrayList<>();

    private volatile boolean playbackRunning = false;
    private Thread playbackThread;



    public void setOnProjectLoadedListener(Consumer<Path> onProjectLoadedListener) {
        this.onProjectLoadedListener = onProjectLoadedListener;
    }

    private void notifyProjectLoaded() {
        if (onProjectLoadedListener != null && currentProject != null) {
            onProjectLoadedListener.accept(currentProject);
        }
    }

    @FXML
    public void initialize() {
        tracksTableView.setItems(FXCollections.observableList(trackPanes));
        tracksTableView.setSelectionModel(null);
        tracksTableView.setOnSort(Event::consume);

        trackControlPaneColumn.setSortable(false);
        trackTimelinePaneColum.setSortable(false);

        trackControlPaneColumn.setCellValueFactory(new PropertyValueFactory<>("controlPane"));
        trackTimelinePaneColum.setCellValueFactory(new PropertyValueFactory<>("timeLinePane"));

        timelineSeekerPane = new TimelineSeekerPane(zoomFactor);
        trackTimelinePaneColum.setGraphic(timelineSeekerPane);

        tracksTableView.setFixedCellSize(100);
        tracksTableView.fixedCellSizeProperty().bind(smoothTrackHeightProperty());

        setupTrackHeaderContextMenu();
        setupSelectionContextMenu();

        pe = new PlaybackEngine();
        timelineSeekerPane.seekerPosition.bind(pe.seeker);
        timelineSeekerPane.bindSongLengthProperty(pe.songLengthProperty);
        addNewTrack();
        addNewTrack();

        try {
            String lastOpenedProject = AppFileUtils.readProperty("LAST_OPENED_PROJECT");
            if (!lastOpenedProject.equals("null")) {
                loadProject(Path.of(lastOpenedProject));
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println(e.getMessage());
        }

    }

    private FloatProperty smoothTrackHeightProperty() {
        SimpleFloatProperty smoothHeight = new SimpleFloatProperty();
        smoothHeight.bind(zoomFactor.multiply(60).add(40));
        return smoothHeight;
    }

    private TreeView<String> createSamplesTreeViewForDirectory(File rootDir) {
        TreeView<String> treeView = new TreeView<>();
        TreeItem<String> rootItem = new TreeItem<>(rootDir.getPath());
        rootItem.setExpanded(true);

        addWavFilesRecursively(rootItem, rootDir);

        treeView.setRoot(rootItem);
        treeView.setShowRoot(true);
        setupSelectedSampleBehavior(treeView);
        return treeView;
    }

    private HBox createTreeViewBox(TreeView<String> treeView) {
        Button removeButton = new Button("-");
        removeButton.setOnAction(event -> removeTreeView(treeView));

        HBox box = new HBox(treeView, removeButton);
        box.setSpacing(6);
        HBox.setHgrow(treeView, javafx.scene.layout.Priority.ALWAYS);
        treeView.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    @FXML
    public void addTreeView() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select folder");
        directoryChooser.setInitialDirectory(AppFileUtils.resolveSamplesDir());

        File chosenDir = directoryChooser.showDialog(tracksTableView.getScene().getWindow());
        if (chosenDir == null || !chosenDir.isDirectory()) {
            return;
        }
        sampleTreeRoots.add(chosenDir.toPath());

        TreeView<String> newTreeView = createSamplesTreeViewForDirectory(chosenDir);
        samplesSplitPane.getItems().add(samplesSplitPane.getItems().size(), createTreeViewBox(newTreeView));
        normalizeSamplesSplitPane();
    }

    @FXML
    public void removeTreeView(TreeView<String> treeView) {
        if (treeView == null) {
            return;
        }

        Node parent = treeView.getParent();
        if (parent instanceof HBox box) {
            TreeItem<String> root = treeView.getRoot();
            if (root != null) {
                sampleTreeRoots.removeIf(rootPath -> rootPath.toString().equals(root.getValue()));
            }

            samplesSplitPane.getItems().remove(box);
            normalizeSamplesSplitPane();
        }
    }

    private void normalizeSamplesSplitPane() {
        List<javafx.scene.Node> treeItems = samplesSplitPane.getItems().stream()
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

    private void addWavFilesRecursively(TreeItem<String> parentItem, File directory) {
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

    private void configureTrackPane(TrackPane trackPane) {
        trackPane.setOnDeleteAction(event -> removeTrack(trackPane));
        trackPane.setOnAddClipAction(event -> chooseAndAddClip(trackPane));
        trackPane.setOnAddReiterativeClipAction(event -> chooseAndAddReiterativeClip(trackPane));
        trackPane.setOnPasteCopiedClips(event -> pasteClipPanes(copiedClips, trackPane));
        trackPane.setOnTrimAction(event -> {
            if (event.getSource() instanceof ClipPane clipPane) {
                trimClip(clipPane);
            }
        });
        trackPane.setOnClipSelection(event -> {
            if (event.getSource() instanceof ClipPane clipPane) {
                toggleClipSelection(clipPane);
            }
        });

        trackPane.setOnRightClickSelection(this::showSelectionContextMenu);

        trackPane.setOnMousePressedAction(event -> {
            if (event.getButton() == MouseButton.PRIMARY && selectedFile != null) {
                stopPlaybackForEdit();
                int startMs = calculateClipStartMs(trackPane);
                trackPane.addAudioClip(new Clip(selectedFile, startMs));
                reloadPlaybackEngine();
                tracksTableView.refresh();
            }
        });

        trackPane.setOnSplitClip(event -> {
            if (event.getSource() instanceof ClipPane clipPane) {
                splitClipPane(trackPane, clipPane);
            }
        });

        trackPane.bindZoomFactor(zoomFactor);
        trackPane.bindClipStartOffset(clipStartOffset);
        trackPane.bindSelectionEnabled(isSelectionToolActiveProperty);
    }

    private void splitClipPane(TrackPane trackPane, ClipPane clipPane) {
        stopPlaybackForEdit();

        double splittingPoint = clipPane.getLastMouseX();
        // Width = ZoomFactor * msLength -> l = w/z
        int splittingMs = (int) (splittingPoint/ zoomFactor.get());

        File wavFile = clipPane.getAudioClip().getWavFile();
        int timelineMsPosition = clipPane.getAudioClip().getTimelineMsPosition();

        Clip frontPartClip = new Clip(wavFile, timelineMsPosition);
        frontPartClip.setLength(splittingMs);

        Clip backPartClip = new Clip(wavFile, timelineMsPosition + splittingMs);
        backPartClip.setLength(clipPane.getAudioClip().getLength()-splittingMs);

        trackPane.addAudioClip(frontPartClip);
        trackPane.addAudioClip(backPartClip);

        trackPane.removeAudioClip(clipPane);

        reloadPlaybackEngine();

    }

    private void showSelectionContextMenu(MouseEvent event) {

        if (!selectionContextMenu.isShowing())
            selectionContextMenu.show(tracksTableView.getParent(), event.getScreenX(), event.getScreenY());
        else
            selectionContextMenu.hide();



    }

    private void setupSelectionContextMenu() {

        MenuItem moveSelectedClipsMenuItem = new MenuItem("Move");
        moveSelectedClipsMenuItem.setOnAction(this::onMoveSelectedClips);

        MenuItem moveToSelectedClipsMenuItem = new MenuItem("Move to...");
        moveToSelectedClipsMenuItem.setOnAction(this::onMoveToSelectedClips);

        MenuItem removeSelectedClipsMenuItem = new MenuItem("Remove");
        removeSelectedClipsMenuItem.setOnAction(this::onRemoveSelectedClips);

        MenuItem copySelectedClipsMenuItem = new MenuItem("Copy");
        copySelectedClipsMenuItem.setOnAction(this::onCopySelectedClips);

        selectionContextMenu.getItems().addAll(moveSelectedClipsMenuItem, moveToSelectedClipsMenuItem, removeSelectedClipsMenuItem, copySelectedClipsMenuItem);



    }

    private void onMoveSelectedClips(ActionEvent actionEvent) {
        if (!selectedClips.isEmpty()) {

            stopPlaybackForEdit();

            System.out.println("move clips");

            TextInputDialog dialog = new TextInputDialog("0.5");
            dialog.setTitle("Move clip");
            dialog.setHeaderText("Enter seconds to move ");
            dialog.setContentText("Seconds");

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(value -> {
                try {
                    double seconds = Double.parseDouble(value.replace(",", "."));

                    selectedClips.forEach(clipPane ->
                            clipPane.setClipStartPosition(
                                    clipPane.getAudioClip().getTimelineMsPosition() + (int) (seconds * 1000)
                            )
                    );

                } catch (NumberFormatException ignored) {
                    // Ignored
                }
            });
            reloadPlaybackEngine();
        }
    }

    private void onMoveToSelectedClips(ActionEvent actionEvent) {
        if (!selectedClips.isEmpty()) {

            stopPlaybackForEdit();


            TextInputDialog dialog = new TextInputDialog("0");
            dialog.setTitle("Move clip to...");
            dialog.setHeaderText("Enter seconds to move to");
            dialog.setContentText("Seconds");

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(value -> {
                try {
                    double seconds = Double.parseDouble(value.replace(",", "."));

                    int offset = selectedClips
                            .stream()
                            .sorted()
                            .toList()
                            .getFirst()
                            .getAudioClip()
                            .getTimelineMsPosition() - (int) (seconds * 1000);

                    selectedClips.forEach(clipPane ->
                            clipPane.setClipStartPosition(clipPane.getAudioClip().getTimelineMsPosition() - offset)
                    );

                } catch (NumberFormatException ignored) {
                    // Ignored
                }
            });
            reloadPlaybackEngine();
        }
    }

    private void onRemoveSelectedClips(ActionEvent actionEvent) {
        if (!selectedClips.isEmpty()) {

            stopPlaybackForEdit();


            selectedClips.forEach(clipPane ->
                    trackPanes.forEach(trackPane -> {
                        if (trackPane.getClipPanes().contains(clipPane)) trackPane.removeAudioClip(clipPane);
                    })
            );

            clearSelection();
            reloadPlaybackEngine();
        }
    }

    private void onCopySelectedClips(ActionEvent actionEvent) {
        copiedClips.addAll(selectedClips);
    }



    public void pasteClipPanes( List<ClipPane> clipPanes, TrackPane targetTrackPane) {
        if (!selectedClips.isEmpty()) {
            stopPlaybackForEdit();
            int startMs = calculateClipStartMs(targetTrackPane);

            int offset = selectedClips
                    .stream()
                    .sorted()
                    .toList()
                    .getFirst()
                    .getAudioClip()
                    .getTimelineMsPosition() - startMs;


            clipPanes.stream().sorted().forEach(clipPane -> {
                Clip pastedClip = new Clip(
                        clipPane.getAudioClip().getWavFile(),
                        clipPane.getAudioClip().getTimelineMsPosition() - offset
                );
                targetTrackPane.addAudioClip(pastedClip);
            });

            reloadPlaybackEngine();
        }
    }



    @FXML
    private void toggleSelectionTool() {
        isSelectionToolActiveProperty.set(!isSelectionToolActiveProperty.get());
        if (isSelectionToolActiveProperty.get()) {
            selectionToolButton.setStyle("-fx-background-color:blue;");
        } else {
            selectionToolButton.setStyle("-fx-background-color:transparent;");
        }
    }

    private void toggleClipSelection(ClipPane clipPane) {
        if (clipPane == null) {
            return;
        }

        if (clipPane.isSelected()) {
            clipPane.setSelected(false);
            selectedClips.remove(clipPane);
        } else {
            clipPane.setSelected(true);
            if (!selectedClips.contains(clipPane)) {
                selectedClips.add(clipPane);
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
                selectedFile = null;
                return;
            }

            if (!item.getChildren().isEmpty()) {
                selectedFile = null;
                return;
            }

            TreeItem<String> root = treeView.getRoot();
            if (root == null) {
                selectedFile = null;
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

            selectedFile = current.exists() && current.isFile() ? current : null;
            System.out.println(selectedFile);
        });
    }

    private void reloadPlaybackEngine() {
        pe.clearTracks();
        for (TrackPane trackPane : trackPanes) {
            pe.addTrack(trackPane.getTrack());
        }
    }


    private void trimClip(ClipPane clipPane) {
        TextInputDialog frontDialog = new TextInputDialog("0");
        frontDialog.setTitle("Trim clip");
        frontDialog.setHeaderText("Milliseconds to trim from the start");
        frontDialog.setContentText("Front:");

        Optional<String> frontResult = frontDialog.showAndWait();
        if (frontResult.isEmpty()) {
            return;
        }

        TextInputDialog backDialog = new TextInputDialog("0");
        backDialog.setTitle("Trim clip");
        backDialog.setHeaderText("Milliseconds to trim from the end");
        backDialog.setContentText("Back:");

        Optional<String> backResult = backDialog.showAndWait();
        if (backResult.isEmpty()) {
            return;
        }

        try {
            int trimFrontMs = Integer.parseInt(frontResult.get().trim());
            int trimBackMs = Integer.parseInt(backResult.get().trim());

            Clip clip = clipPane.getAudioClip();
            int oldLength = clip.getLength();
            int oldAudioStart = clip.getAudioStartMs();
            int oldTimeLineMsPos = clip.getTimelineMsPosition();

            int newAudioStart = Math.max(0, oldAudioStart + trimFrontMs);
            int newLength = Math.max(0, oldLength - trimFrontMs - trimBackMs);

            clip.setAudioStartMs(newAudioStart);
            clip.setTimelineMsPosition(oldTimeLineMsPos + trimFrontMs);
            clip.setLength(newLength);

            clipPane.setClipNameLabel(clip.getWavFile().getName() + " : " + clip.getLength() + "ms");
            clipPane.refreshSize();

            reloadPlaybackEngine();
            tracksTableView.refresh();
        } catch (NumberFormatException ignored) {
            // Ignored
        }
    }

    private void removeTrack(TrackPane trackPane) {
        trackPanes.remove(trackPane);
        reloadPlaybackEngine();
        tracksTableView.refresh();
    }

    private void stopPlaybackForEdit() {
        if (pe != null) {
            pe.requestStop();
        }

        playbackRunning = false;
        playButton.setText("▶");
        reloadPlaybackEngine();
    }

    private void addTrackAndReloadEngine(TrackPane newTrack) {
        stopPlaybackForEdit();
        configureTrackPane(newTrack);
        trackPanes.add(newTrack);
        reloadPlaybackEngine();
        tracksTableView.refresh();
    }

    @FXML
    private void addNewTrack() {
        TrackPane newTrack = new TrackPane(zoomFactor);
        addTrackAndReloadEngine(newTrack);
    }

    private void chooseAndAddClip(TrackPane trackPane) {
        stopPlaybackForEdit();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select clip");
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("WAV files (*.wav)", "*.wav");
        fileChooser.getExtensionFilters().add(extFilter);

        File samplesDir = AppFileUtils.resolveSamplesDir();
        if (samplesDir.isDirectory()) {
            fileChooser.setInitialDirectory(samplesDir);
        }

        File file = fileChooser.showOpenDialog(tracksTableView.getScene().getWindow());
        if (file == null) {
            return;
        }

        int startMs = calculateClipStartMs(trackPane);
        trackPane.addAudioClip(new Clip(file, startMs));
        reloadPlaybackEngine();
    }

    private int calculateClipStartMs(TrackPane trackPane) {
        float pixelsPerMs = zoomFactor.get();
        float ms = (float) (trackPane.getLastMouseX() / pixelsPerMs);
        return Math.max(0, (int) ms);
    }

    private void chooseAndAddReiterativeClip(TrackPane trackPane) {
        stopPlaybackForEdit();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select clip");
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("WAV files (*.wav)", "*.wav");
        fileChooser.getExtensionFilters().add(extFilter);

        File samplesDir = AppFileUtils.resolveSamplesDir();
        if (samplesDir.isDirectory()) {
            fileChooser.setInitialDirectory(samplesDir);
        }

        File file = fileChooser.showOpenDialog(tracksTableView.getScene().getWindow());
        if (file == null) {
            return;
        }

        TextInputDialog secondsDialog = new TextInputDialog("1");
        secondsDialog.setTitle("Add recursive clip");
        secondsDialog.setHeaderText("How many seconds between each repetition");
        secondsDialog.setContentText("Seconds:");

        Optional<String> secondsResult = secondsDialog.showAndWait();
        if (secondsResult.isEmpty()) {
            return;
        }

        TextInputDialog repetitionsDialog = new TextInputDialog("2");
        repetitionsDialog.setTitle("Add recursive clip");
        repetitionsDialog.setHeaderText("How many repetitions to add");
        repetitionsDialog.setContentText("Repetitions:");

        Optional<String> repetitionsResult = repetitionsDialog.showAndWait();
        if (repetitionsResult.isEmpty()) {
            return;
        }

        try {
            float seconds = Float.parseFloat(secondsResult.get().replace(",", "."));
            int repetitions = Integer.parseInt(repetitionsResult.get());
            float stepMs = seconds * 1000f;

            for (int i = 0; i < repetitions; i++) {
                trackPane.addAudioClip(new Clip(file, (int) (i * stepMs)));
            }
            reloadPlaybackEngine();
        } catch (NumberFormatException ignored) {
            // Ignored
        }
    }


    private void setupTrackHeaderContextMenu() {
        MenuItem addTrackItem = new MenuItem("Add track");
        addTrackItem.setOnAction(event -> addNewTrack());

        addTrackContextMenu.getItems().add(addTrackItem);
    }

    @FXML
    private void createNewProject() {
        trackPanes = new ArrayList<>();
        currentProject = null;
        sampleTreeRoots.clear();
        selectedClips.clear();

        tracksTableView.setItems(FXCollections.observableList(trackPanes));
        tracksTableView.getItems().clear();
        tracksTableView.refresh();

        addNewTrack();
        addNewTrack();

        reloadPlaybackEngine();

        try {
            createProject();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

        saveProject();
        notifyProjectLoaded();
    }

    private void createProject() {

        TextInputDialog repetitionsDialog = new TextInputDialog("Project");
        repetitionsDialog.setTitle("Create new project");
        repetitionsDialog.setHeaderText("Project name");
        repetitionsDialog.setContentText("Name: ");

        Optional<String> repetitionsResult = repetitionsDialog.showAndWait();
        if (repetitionsResult.isEmpty()) {
            return;
        }

        try {
            currentProject = AppFileUtils.createMusicProjectFile(repetitionsResult.get());
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }

        saveProject();
    }

    @FXML
    public void saveProject() {
        try {
            if (currentProject == null) {
                createProject();
                if (currentProject == null) return;
            }

            AppFileUtils.writeMusicProject(currentProject, trackPanes, sampleTreeRoots);
            AppFileUtils.writeProperty("LAST_OPENED_PROJECT", currentProject.toAbsolutePath().toString());

            System.out.println("File saved");
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    @FXML
    private void openProject() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select project");
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("musicproject files (*.musicproject)", "*.musicproject");
        fileChooser.getExtensionFilters().add(extFilter);

        fileChooser.setInitialDirectory(AppFileUtils.getProjectsDir().toFile());

        File file = fileChooser.showOpenDialog(tracksTableView.getScene().getWindow());
        if (file == null) {
            return;
        }

        try {
            loadProject(file.toPath());
            AppFileUtils.writeProperty("LAST_OPENED_PROJECT", file.getAbsolutePath());
            notifyProjectLoaded();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println(e.getMessage());
        }
    }

    private void loadProject(Path projectPath) throws IOException, ClassNotFoundException {
        currentProject = projectPath;

        AppFileUtils.MusicProjectData projectData = AppFileUtils.readMusicProject(projectPath);
        trackPanes = new ArrayList<>(projectData.trackPanes());
        sampleTreeRoots.clear();
        sampleTreeRoots.addAll(projectData.sampleTreeRoots());

        selectedClips.clear();

        tracksTableView.setItems(FXCollections.observableList(trackPanes));
        trackPanes.forEach(this::configureTrackPane);
        tracksTableView.refresh();
        reloadPlaybackEngine();

        restoreSampleTreeViews();
    }

    private void restoreSampleTreeViews() {
        samplesSplitPane.getItems().clear();

        File defaultSamples = AppFileUtils.resolveSamplesDir();

        if (defaultSamples.exists() && defaultSamples.isDirectory()) {
            baseSamplesTreeView = createSamplesTreeViewForDirectory(defaultSamples);
            samplesSplitPane.getItems().add(samplesSplitPane.getItems().size(), createTreeViewBox(baseSamplesTreeView));
        }

        System.out.println(sampleTreeRoots);
        for (Path rootPath : sampleTreeRoots) {
            File rootDir = rootPath.toFile();
            if (!rootDir.exists() || !rootDir.isDirectory()) {
                continue;
            }

            TreeView<String> newTreeView = createSamplesTreeViewForDirectory(rootDir);
            samplesSplitPane.getItems().add(samplesSplitPane.getItems().size(), createTreeViewBox(newTreeView));
        }



        normalizeSamplesSplitPane();
    }

    @FXML
    public void exportAudio() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export audio");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("WAV files (*.wav)", "*.wav")
        );
        fileChooser.setInitialFileName("song.wav");

        if (currentProject != null && currentProject.getParent() != null) {
            fileChooser.setInitialDirectory(currentProject.getParent().toFile());
        } else {
            File projectsDir = AppFileUtils.getProjectsDir().toFile();
            if (projectsDir.exists()) {
                fileChooser.setInitialDirectory(projectsDir);
            }
        }

        File outputFile = fileChooser.showSaveDialog(tracksTableView.getScene().getWindow());
        if (outputFile == null) {
            return;
        }

        if (!outputFile.getName().toLowerCase().endsWith(".wav")) {
            outputFile = new File(outputFile.getParentFile(), outputFile.getName() + ".wav");
        }

        pe.exportToWav(outputFile.toPath());
    }


    private void startPlayback() {
        if (playbackRunning) {
            return;
        }

        playbackRunning = true;

        playButton.setText("⏸");

        playbackThread = new Thread(() -> {
            try {
                pe.setPausedFrame((int) Math.round(pe.seeker.get() * 44.1));
                pe.clearPauseRequest();
                pe.play();
            } finally {
                playbackRunning = false;

                Platform.runLater(() -> playButton.setText("▶"));
            }
        });
        playbackThread.setDaemon(true);
        playbackThread.start();
    }

    private void pausePlayback() {
        pe.setPausedFrame(pe.seeker.get() * 44);
        pe.requestPause();
        playButton.setText("▶");
    }

    @FXML
    public void goToStart() {
        stopPlaybackForEdit();
        pe.requestStop();
        pe.clearPauseRequest();
        pe.setPausedFrame(0);
        pe.seeker.set(0);

        if (playbackThread != null && playbackThread.isAlive()) {
            try {
                playbackThread.join(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }


        playbackRunning = false;
        playButton.setText("▶");
    }

    @FXML
    public void play() {
        if (playbackRunning && !pe.isPaused()) {
            pausePlayback();
            return;
        }

        if (pe.isPaused()) {
            pe.clearPauseRequest();
            if (!playbackRunning) {
                startPlayback();
            } else {
                playButton.setText("⏸");
            }
            return;
        }

        startPlayback();
    }


    @FXML
    public void importSamplePack() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select pack");
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("sample packs (*.zip)", "*.zip");
        fileChooser.getExtensionFilters().add(extFilter);

        fileChooser.setInitialDirectory(AppFileUtils.getProjectsDir().toFile());

        File file = fileChooser.showOpenDialog(tracksTableView.getScene().getWindow());
        if (file == null) {
            return;
        }

        AppFileUtils.extractZipIntoSamplesDir(file);
        restoreSampleTreeViews();
    }


    @FXML
    public void clearSelection() {
        for (ClipPane clipPane : selectedClips) {
            clipPane.setSelected(false);
        }
        selectedClips.clear();
    }

    public void enableSelection() {
        isSelectionToolActiveProperty.set(true);
        selectionToolButton.setStyle("-fx-background-color:blue;");
    }

    public void disableSelection() {
        isSelectionToolActiveProperty.set(false);
        selectionToolButton.setStyle("-fx-background-color:transparent;");
    }

}
