package app.musiceditorclient.infrastructure;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class FfmpegInstaller {

    private static final String APP_DIR_NAME = "MusicEditor";
    private static final String RESOURCE_BASE = "/app/musiceditorclient/dependencies/ffmpegSuite/";

    private static Path ffprobePath;
    private static Path ffmpegPath ;

    public static void ensureInstalled() throws IOException {
        // platform and home properties to compare later
        String platform = System.getProperty("os.name");
        String home = System.getProperty("user.home");

        String platformFolderName;
        Path userDataDir;

        // checks for Windows, Mac and Linux and establishes the data folder
        if (platform.contains("win")) {
            String appData = System.getenv("APPDATA");
            userDataDir = Paths.get(appData, APP_DIR_NAME);
            platformFolderName = "windows";
        } else if (platform.contains("mac") || platform.contains("darwin")) {
            userDataDir = Paths.get(home, "Library", "Application Support", APP_DIR_NAME);
            platformFolderName = "mac";
        } else {
            userDataDir = Paths.get(home, ".local", "share", APP_DIR_NAME);
            platformFolderName = "linux";
        }

        // Create the folder
        if (!Files.exists(userDataDir)) Files.createDirectory(userDataDir);

        // In windows, files are executables
        String ffprobeFileName = platform.contains("win") ? "ffprobe.exe":"ffprobe";
        String ffmpegFileName = platform.contains("win") ? "ffmpeg.exe":"ffmpeg";

        // Final path where files are going to be copied
        ffprobePath = userDataDir.resolve(ffprobeFileName);
        ffmpegPath = userDataDir.resolve(ffmpegFileName);

        try (
                InputStream ffprobeStream = FfmpegInstaller.class.getResourceAsStream(RESOURCE_BASE + platformFolderName + "/" + ffprobeFileName);
                InputStream ffmpegStream = FfmpegInstaller.class.getResourceAsStream(RESOURCE_BASE + platformFolderName + "/" + ffmpegFileName)
                ) {

            // copies ffprobe
            if (ffprobeStream == null) throw new IOException("Missing resource: " + ffprobeFileName);
            Files.copy(
                    ffprobeStream, ffprobePath, StandardCopyOption.REPLACE_EXISTING
            );
            // gives exec permission
            boolean ffprobeHasPermission = ffprobePath.toFile().setExecutable(true, false);
            if (!ffprobeHasPermission) throw new IOException("Could not set executable: " + ffprobePath);

            // copies ffmpeg
            if (ffmpegStream == null) throw new IOException("Missing resource: " + ffmpegFileName);
            Files.copy(
                    ffmpegStream, ffmpegPath, StandardCopyOption.REPLACE_EXISTING
            );
            // gives exec permission
            boolean ffmpegHasPermission = ffmpegPath.toFile().setExecutable(true, false);
            if (!ffmpegHasPermission) throw new IOException("Could not set executable: " + ffmpegPath);

        }

    }

    public static Path getFfprobePath() {
        return ffprobePath;
    }

    public static Path getFfmpegPath() {
        return ffmpegPath;
    }
}
