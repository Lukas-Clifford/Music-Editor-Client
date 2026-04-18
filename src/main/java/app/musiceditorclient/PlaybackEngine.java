package app.musiceditorclient;

import app.musiceditorclient.models.Clip;
import app.musiceditorclient.models.Track;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlaybackEngine {

    private final int FRAME_RATE = 48;
    private final int SAMPLE_RATE = 48000;
    private final int FRAME_SIZE = 6;
    private final int NORMALISED_FRAME_RATE = FRAME_RATE * FRAME_SIZE;

    private final AudioFormat format = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED, // encoding
            SAMPLE_RATE,                // sampleRate (Hz)
            24,                              // sampleSizeInBits
            2,                               // channels (2 = stereo)
            6,                               // frameSize (bytes por frame)
            FRAME_RATE*1000,                 // frameRate
            false                            // bigEndian (false = littleEndian)
    );

    private final DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

    private final List<Track> tracks = new ArrayList<>();
    private int songLength = 0;

    public SimpleIntegerProperty seeker = new SimpleIntegerProperty(0);

    private volatile boolean stopRequested = false;
    private volatile boolean pauseRequested = false;

    public PlaybackEngine() {}

    public PlaybackEngine(List<Track> tracks) {
        this.tracks.addAll(tracks);
        songLength = Collections.max(tracks).getLength();
    }

    public void clearTracks() {
        tracks.clear();
        songLength = 0;
        seeker.set(0);
    }

    public void setTracks(List<Track> tracks) {
        clearTracks();
        this.tracks.addAll(tracks);
        if (!this.tracks.isEmpty()) {
            songLength = Collections.max(this.tracks).getLength();
        }
    }

    public void addTrack(Track track) {
        this.tracks.add(track);
    }

    public void requestStop() {
        stopRequested = true;
        pauseRequested = false;
    }

    public void clearStopRequest() {
        stopRequested = false;
    }

    public void requestPause() {
        pauseRequested = true;
    }

    public void clearPauseRequest() {
        pauseRequested = false;
    }

    public boolean isPaused() {
        return pauseRequested;
    }

    public boolean isStopRequested() {
        return stopRequested;
    }

    public void play() {
        if (tracks.isEmpty()) {
            System.out.println("No tracks to play");
            return;
        }

        songLength = Collections.max(tracks).getLength();
        if (songLength == 0) {
            System.out.println("Song length == 0");
            return;
        }

        byte[] mixed = getMixedTracks();

        SourceDataLine line = null;
        try {
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();

            int totalFrames = mixed.length / FRAME_SIZE;
            int bufferFrames = 1024;
            byte[] buffer = new byte[bufferFrames * FRAME_SIZE];

            while (!stopRequested) {
                int writtenFrames = 0;
                seeker.set(0);
                long lastUiPushNanos = 0L;
                long baseFrame = -1L;

                while (writtenFrames < totalFrames && !stopRequested && !pauseRequested) {
                    int chunkFrames = Math.min(bufferFrames, totalFrames - writtenFrames);

                    System.arraycopy(
                            mixed,
                            writtenFrames * FRAME_SIZE,
                            buffer,
                            0,
                            chunkFrames * FRAME_SIZE
                    );

                    line.write(buffer, 0, chunkFrames * FRAME_SIZE);
                    writtenFrames += chunkFrames;

                    long now = System.nanoTime();
                    if (now - lastUiPushNanos >= 10_000_000L || writtenFrames >= totalFrames) {
                        long currentFrame = line.getLongFramePosition();
                        if (baseFrame < 0L) {
                            baseFrame = currentFrame;
                        }
                        int ms = (int) Math.max(0L, (currentFrame - baseFrame) / FRAME_RATE);
                        seeker.set(ms);
                        lastUiPushNanos = now;
                    }
                }

                if (pauseRequested) {
                    line.stop();
                    while (pauseRequested && !stopRequested) {
                        Thread.sleep(20);
                    }
                    if (stopRequested) {
                        break;
                    }
                    line.start();
                } else if (!stopRequested) {
                    line.drain();
                    seeker.set(songLength);
                    line.stop();
                    line.flush();
                    line.start();
                }
            }

        } catch (LineUnavailableException | InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if (line != null) {
                line.stop();
                line.close();
            }
            stopRequested = false;
        }
    }

    private byte[] getMixedTracks() {
        byte[] mixed = new byte[ songLength * NORMALISED_FRAME_RATE ];

        System.out.println(tracks);
        for (Track track:tracks) mixPCM24Stereo(mixed, getTrackInPCM(track), mixed);

        return mixed;
    }

    private byte[] getTrackInPCM(Track track){
        byte[] bufferTrack = new byte[songLength * NORMALISED_FRAME_RATE];

        for (Clip clip : track.getClips()) {
            int clipBytes = clip.getLength() * NORMALISED_FRAME_RATE;
            int startByte = clip.getTimelineMsPosition() * NORMALISED_FRAME_RATE;
            int audioStartByte = clip.getAudioStartMs() * NORMALISED_FRAME_RATE;

            try (AudioInputStream ais = AudioSystem.getAudioInputStream(clip.getWavFile())) {
                long skipped = 0;
                while (skipped < audioStartByte) {
                    long step = ais.skip(audioStartByte - skipped);
                    if (step <= 0) {
                        break;
                    }
                    skipped += step;
                }

                byte[] buffer = new byte[clipBytes];
                int read = ais.read(buffer, 0, clipBytes);

                if (read > 0) {
                    int maxWritable = Math.min(read, bufferTrack.length - startByte);
                    if (maxWritable <= 0) {
                        continue;
                    }

                    for (int b = 0; b < maxWritable; b++) {
                        int sample = bufferTrack[startByte + b] + buffer[b];
                        bufferTrack[startByte + b] = (byte) sample;
                    }
                }

            } catch (UnsupportedAudioFileException | IOException e) {
                throw new RuntimeException(e);
            }
        }

        return bufferTrack;
    }

    private static void mixPCM24Stereo(byte[] in1, byte[] in2, byte[] out) {
        // 24-bit stereo: 6 bytes por frame (L=3, R=3).
        for (int i = 0; i < out.length; i += 6) {
            int l1 = pcm24ToIntLE(in1, i);
            int l2 = pcm24ToIntLE(in2, i);
            int mixL = (l1 + l2) / 2;
            mixL = Math.max(-8388608, Math.min(mixL, 8388607));
            intToPCM24LE(mixL, out, i);

            int r1 = pcm24ToIntLE(in1, i + 3);
            int r2 = pcm24ToIntLE(in2, i + 3);
            int mixR = (r1 + r2) / 2;
            mixR = Math.max(-8388608, Math.min(mixR, 8388607));
            intToPCM24LE(mixR, out, i + 3);
        }
    }

    private static int pcm24ToIntLE(byte[] buf, int idx) {
        int b1 = buf[idx] & 0xFF;
        int b2 = buf[idx + 1] & 0xFF;
        int b3 = buf[idx + 2];
        return (b3 << 16) | (b2 << 8) | b1;
    }

    private static void intToPCM24LE(int val, byte[] buf, int idx) {
        buf[idx]     = (byte) (val);
        buf[idx + 1] = (byte) (val >> 8);
        buf[idx + 2] = (byte) (val >> 16);
    }

    public void exportToWav(Path outputFile) {
        if (tracks.isEmpty()) {
            return;
        }

        songLength = Collections.max(tracks).getLength();
        if (songLength == 0) {
            System.out.println("Song length == 0");
            return;
        }

        byte[] mixed = getMixedTracks();

        try (ByteArrayInputStream bais = new ByteArrayInputStream(mixed);
             AudioInputStream audioInputStream = new AudioInputStream(
                     bais,
                     format,
                     mixed.length / FRAME_SIZE
             )) {
            AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, outputFile.toFile());
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }
}
