package app.musiceditorclient.models;

import app.musiceditorclient.services.FFmpegService;
import app.musiceditorclient.services.FfprobeService;
import javafx.beans.property.SimpleIntegerProperty;

import java.io.*;
import java.util.Objects;

public class Clip implements Comparable<Clip>, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private File wavFile;
    private transient SimpleIntegerProperty timelineMsPosition = new SimpleIntegerProperty(0);
    private int timelineMsPositionValue = 0;
    private int length = 0;
    private transient SimpleIntegerProperty audioStartMs = new SimpleIntegerProperty(0);
    private int audioStartMsValue = 0;
    private boolean isTrimmed = false;

    public Clip(File wavFile, int timelineStartSample) {
        this.wavFile = wavFile;
        this.timelineMsPositionValue = timelineStartSample;
        this.timelineMsPosition.set(timelineStartSample);
        this.length = this.getLength();
        this.audioStartMsValue = 0;
        this.audioStartMs.set(0);

        if (FfprobeService.getFileSampleRate(this.wavFile) != 44100) {
            FFmpegService.setSampleRate(this.wavFile);
        }
    }

    @Override
    public int compareTo(Clip o) {
        return Double.compare(this.timelineMsPosition.get(), o.timelineMsPosition.get());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Clip clip = (Clip) o;
        return timelineMsPosition.get() == clip.timelineMsPosition.get()
                && Double.compare(length, clip.length) == 0
                && Objects.equals(wavFile, clip.wavFile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(wavFile, timelineMsPosition.get(), length);
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
        return timelineMsPosition.get();
    }

    public void setTimelineMsPosition(int timelineMsPosition) {
        this.timelineMsPositionValue = timelineMsPosition;
        this.timelineMsPosition.set(timelineMsPosition);
    }

    public SimpleIntegerProperty getTimelineMsPositionProperty() {
        if (timelineMsPosition == null) {
            timelineMsPosition = new SimpleIntegerProperty(timelineMsPositionValue);
        }
        return this.timelineMsPosition;
    }

    public int getLength() {
        if (isTrimmed) return this.length;
        return FfprobeService.getFileLength(wavFile);
    }

    public void setLength(int length) {
        this.length = length;
        this.isTrimmed = true;
    }

    public int getEndPosition() {
        return this.timelineMsPositionValue + this.getLength();
    }

    public int getAudioStartMs() {
        return audioStartMs.get();
    }

    public void setAudioStartMs(int audioStartMs) {
        this.audioStartMsValue = audioStartMs;
        this.audioStartMs.set(audioStartMs);
    }

    public SimpleIntegerProperty getAudioStartMsProperty() {
        if (audioStartMs == null) {
            audioStartMs = new SimpleIntegerProperty(audioStartMsValue);
        }
        return audioStartMs;
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.length = getLength();
        this.timelineMsPosition = new SimpleIntegerProperty(timelineMsPositionValue);
        this.audioStartMs = new SimpleIntegerProperty(audioStartMsValue);
    }
}
