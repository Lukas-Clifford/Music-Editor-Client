package app.musiceditorclient.services;

import app.musiceditorclient.infrastructure.FfmpegInstaller;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class FfprobeService {


    /**
     * Returns audio length in milliseconds
     * @param filePath - Absolute path
     * @return Milliseconds
     */
    public static int getFileLength(File filePath){

        List<String> cmd = List.of(
                FfmpegInstaller.getFfprobePath().toString(),
                "-v", "error",
                "-show_entries", "format=duration",
                "-of", "default=nw=1:nk=1",
                filePath.getAbsolutePath()
        );

        try {
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();

            String output = new String (
                    p.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
            ).trim();
            int exit = p.waitFor();

            if (exit != 0)  throw new RuntimeException("ffprobe failed: " + output);

            return Math.toIntExact(Math.round(Double.parseDouble(output) * 1000));

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }


    }


    /**
     * Returns sample rate from audio file in Hz (project standard: 44100)
     * @param filePath - Absolute path
     * @return Sample rate in Hz
     */

    public static int getFileSampleRate(File filePath){

        List<String> cmd = List.of(
                FfmpegInstaller.getFfprobePath().toString(),
                "-v", "error",
                "-show_entries", "stream=sample_rate",
                "-of", "default=nw=1:nk=1",
                filePath.getAbsolutePath()
        );

        try {
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();

            String output = new String (
                    p.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
            ).trim();
            int exit = p.waitFor();

            if (exit != 0)  throw new RuntimeException("ffprobe failed: " + output);

            return Integer.parseInt(output);

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }


    }




}
