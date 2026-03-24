package app.musiceditorclient.services;

import app.musiceditorclient.infrastructure.FfmpegInstaller;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

public class FFmpegService {


    /**
     * Overwrites file to match project's sample rate standard : 48kHz
     * @param filePath - Absolute path
     */

    public static void setSampleRate(File filePath){

        List<String> cmd = List.of(
                FfmpegInstaller.getFfmpegPath().toString(),
                "-y",
                "-hide_banner",
                "-i", filePath.getAbsolutePath(),
                "-ar", "48000",
                filePath.getAbsolutePath()
        );

        try {
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();

            int exit = p.waitFor();

            if (exit != 0)  throw new RuntimeException("ffprobe failed to set samplerate on " + filePath);

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }


    }

}
