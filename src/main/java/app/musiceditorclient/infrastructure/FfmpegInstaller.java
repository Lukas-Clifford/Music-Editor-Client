package app.musiceditorclient.infrastructure;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FfmpegInstaller {

    private static final String PROJECT_DEPENDENCIES_DIR = "dependencies/ffmpegSuite";

    public static void ensureInstalled() throws IOException {
        Path userDataDir = AppFileUtils.getAppDataDir();
        String platformFolderName = AppFileUtils.getPlatformFolderName();
        String zipFileName = platformFolderName + ".zip";

        Files.createDirectories(userDataDir);

        String ffprobeFileName = AppFileUtils.isWindows() ? "ffprobe.exe" : "ffprobe";
        String ffmpegFileName = AppFileUtils.isWindows() ? "ffmpeg.exe" : "ffmpeg";

        Path ffprobePath = userDataDir.resolve(ffprobeFileName);
        Path ffmpegPath = userDataDir.resolve(ffmpegFileName);


        Path projectZip = userDataDir.resolve(PROJECT_DEPENDENCIES_DIR).resolve(zipFileName);
        if (Files.notExists(projectZip)) {
            showSuiteImportInfo(zipFileName);
            File selectedZip = promptForZip(zipFileName, null);
            if (selectedZip == null) {
                throw new IOException("FFmpeg installation is required.");
            }

            Files.createDirectories(projectZip.getParent());
            Files.copy(selectedZip.toPath(), projectZip, StandardCopyOption.REPLACE_EXISTING);
        }

        if (Files.exists(ffprobePath) && Files.exists(ffmpegPath)) {
            return;
        }
        extractZipToUserDir(projectZip, ffprobeFileName, ffmpegFileName, ffprobePath, ffmpegPath);
    }

    private static void showSuiteImportInfo(String zipFileName) {
        Runnable dialogTask = () -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("FFmpeg suite required");
            alert.setHeaderText(null);
            alert.setContentText("The FFmpegSuite is not installed yet.\nPlease import the " + zipFileName + " file to continue.");
            alert.showAndWait();
        };

        if (Platform.isFxApplicationThread()) {
            dialogTask.run();
        } else {
            Platform.runLater(dialogTask);
        }
    }

    private static File promptForZip(String zipFileName, Window owner) throws IOException {
        AtomicReference<File> result = new AtomicReference<>();
        Runnable dialogTask = () -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select " + zipFileName);
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ZIP files", "*.zip"));
            result.set(fileChooser.showOpenDialog(owner));
        };

        if (Platform.isFxApplicationThread()) {
            dialogTask.run();
            return result.get();
        }

        try {
            Platform.runLater(dialogTask);
            while (result.get() == null) {
                Thread.sleep(20);
            }
            return result.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while opening file chooser.", e);
        }
    }

    private static void extractZipToUserDir(Path zipPath, String ffprobeFileName, String ffmpegFileName,
                                            Path ffprobePath, Path ffmpegPath) throws IOException {
        try (InputStream zipStream = Files.newInputStream(zipPath);
             ZipInputStream zipInputStream = new ZipInputStream(zipStream)) {

            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }

                String entryName = Path.of(entry.getName()).getFileName().toString();
                Path target = entryName.equals(ffprobeFileName) ? ffprobePath
                        : entryName.equals(ffmpegFileName) ? ffmpegPath
                        : null;

                if (target == null) {
                    continue;
                }

                Files.copy(zipInputStream, target, StandardCopyOption.REPLACE_EXISTING);
                if (!target.toFile().setExecutable(true, false)) {
                    throw new IOException("Could not set executable: " + target);
                }
            }
        }
    }

    public static Path getFfprobePath() {
        Path userDataDir = AppFileUtils.getAppDataDir();
        String ffprobeFileName = AppFileUtils.isWindows() ? "ffprobe.exe" : "ffprobe";
        return userDataDir.resolve(ffprobeFileName);
    }

    public static Path getFfmpegPath() {
        Path userDataDir = AppFileUtils.getAppDataDir();
        String ffmpegFileName = AppFileUtils.isWindows() ? "ffmpeg.exe" : "ffmpeg";
        return userDataDir.resolve(ffmpegFileName);
    }
}
