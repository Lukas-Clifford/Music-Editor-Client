package app.musiceditorclient.infrastructure;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class FfmpegInstaller {

    private static final String RESOURCE_BASE = "/app/musiceditorclient/dependencies/ffmpegSuite/";

    public static void ensureInstalled() throws IOException {
        Path userDataDir = AppFileUtils.getAppDataDir();
        String platformFolderName = AppFileUtils.getPlatformFolderName();

        if (!Files.exists(userDataDir)) {
            Files.createDirectories(userDataDir);
        }

        String ffprobeFileName = AppFileUtils.isWindows() ? "ffprobe.exe" : "ffprobe";
        String ffmpegFileName = AppFileUtils.isWindows() ? "ffmpeg.exe" : "ffmpeg";

        Path ffprobePath = userDataDir.resolve(ffprobeFileName);
        Path ffmpegPath = userDataDir.resolve(ffmpegFileName);

        try (
                InputStream ffprobeStream = FfmpegInstaller.class.getResourceAsStream(
                        RESOURCE_BASE + platformFolderName + "/" + ffprobeFileName
                );
                InputStream ffmpegStream = FfmpegInstaller.class.getResourceAsStream(
                        RESOURCE_BASE + platformFolderName + "/" + ffmpegFileName
                )
        ) {
            if (ffprobeStream == null) throw new IOException("Missing resource: " + ffprobeFileName);
            Files.copy(ffprobeStream, ffprobePath, StandardCopyOption.REPLACE_EXISTING);
            if (!ffprobePath.toFile().setExecutable(true, false)) {
                throw new IOException("Could not set executable: " + ffprobePath);
            }

            if (ffmpegStream == null) throw new IOException("Missing resource: " + ffmpegFileName);
            Files.copy(ffmpegStream, ffmpegPath, StandardCopyOption.REPLACE_EXISTING);
            if (!ffmpegPath.toFile().setExecutable(true, false)) {
                throw new IOException("Could not set executable: " + ffmpegPath);
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
