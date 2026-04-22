package app.musiceditorclient.services;

import app.musiceditorclient.EditorContext;
import app.musiceditorclient.infrastructure.AppFileUtils;
import javafx.collections.FXCollections;
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
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("No se pudo cargar el ultimo proyecto");

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
            System.err.println(e.getMessage());
        }

        saveProject();
    }

    public void saveProject() {
        try {
            AppFileUtils.writeMusicProject(context.project().getCurrentProject(), context.project().getTrackPanes(), context.ui().getSampleTreeRoots());
            AppFileUtils.writeProperty("LAST_OPENED_PROJECT", context.project().getCurrentProject().toAbsolutePath().toString());
        } catch (IOException e) {
            System.err.println("Could not save project");
        }
    }
}
