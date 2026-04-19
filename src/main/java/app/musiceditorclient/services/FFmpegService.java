package app.musiceditorclient.services;

import app.musiceditorclient.infrastructure.FfmpegInstaller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class FFmpegService {

    /**
     * Overwrites file to match project's sample rate standard : 44.1kHz
     * @param filePath - Absolute path
     */
    public static void setSampleRate(File filePath){
        Path originalPath = filePath.toPath();
        Path tempPath = originalPath.resolveSibling(
                filePath.getName().replaceFirst("(\\.[^.]+)?$", "_tmp_44100.wav")
        );

        List<String> cmd = List.of(
                FfmpegInstaller.getFfmpegPath().toString(),
                "-y",
                "-hide_banner",
                "-i", originalPath.toAbsolutePath().toString(),
                "-ar", "44100",
                tempPath.toAbsolutePath().toString()
        );

        try {
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            int exit = p.waitFor();

            if (exit != 0) {
                Files.deleteIfExists(tempPath);
                throw new RuntimeException("ffmpeg failed to set samplerate on " + filePath);
            }

            Files.move(tempPath, originalPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | InterruptedException e) {
            try {
                Files.deleteIfExists(tempPath);
            } catch (IOException ignored) {
            }
            throw new RuntimeException(e);
        }
    }
}
