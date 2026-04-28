package app.musiceditorclient.infrastructure;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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

        InputStream ffprobeStream = FfmpegInstaller.class.getResourceAsStream(
                RESOURCE_BASE + platformFolderName + "/" + ffprobeFileName
        );
        InputStream ffmpegStream = FfmpegInstaller.class.getResourceAsStream(
                RESOURCE_BASE + platformFolderName + "/" + ffmpegFileName
        );

        if (ffprobeStream != null && ffmpegStream != null) {
            try (
                    ffprobeStream;
                    ffmpegStream
            ) {
                Files.copy(ffprobeStream, ffprobePath, StandardCopyOption.REPLACE_EXISTING);
                if (!ffprobePath.toFile().setExecutable(true, false)) {
                    throw new IOException("Could not set executable: " + ffprobePath);
                }

                Files.copy(ffmpegStream, ffmpegPath, StandardCopyOption.REPLACE_EXISTING);
                if (!ffmpegPath.toFile().setExecutable(true, false)) {
                    throw new IOException("Could not set executable: " + ffmpegPath);
                }
            }
            return;
        }

        String zipResource = RESOURCE_BASE + "/" + platformFolderName + ".zip";
        try (InputStream zipStream = FfmpegInstaller.class.getResourceAsStream(zipResource)) {
            if (zipStream == null) {
                throw new IOException("Missing resources: " + ffprobeFileName + ", " + ffmpegFileName + " or " + platformFolderName + ".zip");
            }

            try (ZipInputStream zipInputStream = new ZipInputStream(zipStream)) {
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
