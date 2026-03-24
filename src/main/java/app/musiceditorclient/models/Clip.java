package app.musiceditorclient.models;

import app.musiceditorclient.services.FFmpegService;
import app.musiceditorclient.services.FfprobeService;

import java.io.File;
import java.util.Objects;

public class Clip implements Comparable<Clip>{

    private File wavFile;
    private int timelineMsPosition = 0; // where it stands in timeline
    private int length = 0;


    public Clip() {
        this.wavFile = null;
    }
    public Clip(int timelineMsPosition, int length) {
        this.wavFile = null;
    }

    public Clip(File wavFile, int timelineStartSample) {
        this.wavFile = wavFile;
        this.timelineMsPosition = timelineStartSample;

        this.length = FfprobeService.getFileLength(wavFile);

        if (FfprobeService.getFileSampleRate(this.wavFile) != 48000)
            FFmpegService.setSampleRate(this.wavFile);

    }

    @Override
    public int compareTo(Clip o) {
        return Double.compare(this.timelineMsPosition, o.timelineMsPosition);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Clip clip = (Clip) o;
        return timelineMsPosition == clip.timelineMsPosition && Double.compare(length, clip.length) == 0 && Objects.equals(wavFile, clip.wavFile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(wavFile, timelineMsPosition, length);
    }

    @Override
    public String toString() {
        return wavFile.getName();
    }

    public File getWavFile() {
        return wavFile;
    }

    public void setWavFile(File wavFile) {
        this.wavFile = wavFile;
    }

    public int getTimelineMsPosition() {
        return timelineMsPosition;
    }

    public void setTimelineMsPosition(int timelineMsPosition) {
        this.timelineMsPosition = timelineMsPosition;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getEndPosition() { return this.timelineMsPosition + this.length; }
}
