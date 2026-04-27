package app.musiceditorclient.services;

import app.musiceditorclient.EditorContext;
import app.musiceditorclient.infrastructure.AppFileUtils;
import javafx.scene.control.Alert;
import javafx.scene.control.TextInputDialog;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Consumer;

public class ProjectPersistenceService {
    private final EditorContext context;

    public ProjectPersistenceService(EditorContext context) {
        this.context = context;
    }

    public void loadLastOpenedProject() {
        try {
            String lastOpenedProject = AppFileUtils.readProperty("LAST_OPENED_PROJECT");
            if (!lastOpenedProject.equals("null")) {
                loadProject(Path.of(lastOpenedProject));
            }
        } catch (IOException e) {
            showError("Read property error", "Could not read the last opened project.");
        }
    }

    public void loadProject(Path projectPath) {
        try {
            context.project().setCurrentProject(projectPath);

            AppFileUtils.MusicProjectData projectData = AppFileUtils.readMusicProject(projectPath);
            context.project().setTrackPanes(new ArrayList<>(projectData.trackPanes()));
            context.ui().getSampleTreeRoots().clear();
            context.ui().getSampleTreeRoots().addAll(projectData.sampleTreeRoots());

            context.selection().getSelectedClips().clear();
        } catch (IOException e) {
            showError("Load project error", "Could not load the project. File not found");
        } catch (ClassNotFoundException e) {
            showError("Load project error", "Could not load the project. File version not compatible");
        }
    }

    public void setOnProjectLoadedListener(Consumer<Path> onProjectLoadedListener) {
        context.project().setOnProjectLoadedListener(onProjectLoadedListener);
    }

    public void notifyProjectLoaded() {
        if (context.project().getOnProjectLoadedListener() != null && context.project().getCurrentProject() != null) {
            context.project().getOnProjectLoadedListener().accept(context.project().getCurrentProject());
        }
    }

    public void createProject() {
        TextInputDialog repetitionsDialog = new TextInputDialog("Project");
        repetitionsDialog.setTitle("Create new project");
        repetitionsDialog.setHeaderText("Project name");
        repetitionsDialog.setContentText("Name: ");

        Optional<String> repetitionsResult = repetitionsDialog.showAndWait();
        if (repetitionsResult.isEmpty()) {
            return;
        }

        try {
            context.project().setCurrentProject(AppFileUtils.createMusicProjectFile(repetitionsResult.get()));
        } catch (IOException e) {
            showError("Create project error", "Could not create the project.");
            return;
        }

        saveProject();
    }

    public void saveProject() {
        try {
            AppFileUtils.writeMusicProject(context.project().getCurrentProject(), context.project().getTrackPanes(), context.ui().getSampleTreeRoots());
            AppFileUtils.writeProperty("LAST_OPENED_PROJECT", context.project().getCurrentProject().toAbsolutePath().toString());
        } catch (IOException e) {
            showError("Save project error", "Could not save the project.");
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
