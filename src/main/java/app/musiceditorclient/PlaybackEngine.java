package app.musiceditorclient;

import app.musiceditorclient.models.Clip;
import app.musiceditorclient.models.Track;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlaybackEngine {

    private final float FRAME_RATE = 44.1f;
    private final int SAMPLE_RATE = 44100;
    private final int FRAME_SIZE = 4;
    private final float NORMALISED_FRAME_RATE = FRAME_RATE * FRAME_SIZE;
    private final float FRAMES_PER_MS = (float) SAMPLE_RATE / 1000f;

    private final AudioFormat format = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            SAMPLE_RATE,
            16,
            2,
            4,
            FRAME_RATE * 1000f,
            false
    );

    private final DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

    private final List<Track> tracks = new ArrayList<>();
    private int songLength = 0;

    public SimpleFloatProperty seeker = new SimpleFloatProperty(0f);
    public SimpleFloatProperty songLengthProperty = new SimpleFloatProperty(1f);

    private volatile boolean stopRequested = false;
    private volatile boolean pauseRequested = false;
    private volatile float pausedFrame = 0f;

    public PlaybackEngine() {}

    public PlaybackEngine(List<Track> tracks) {
        this.tracks.addAll(tracks);
        songLength = Collections.max(tracks).getLength();
        songLengthProperty.set(songLength * NORMALISED_FRAME_RATE);
    }

    public void reloadSongLength() {
        if (!tracks.isEmpty()) {
            songLength = Collections.max(tracks).getLength();
            songLengthProperty.set(songLength);
        }
    }

    public void play() {
        if (tracks.isEmpty()) {
            System.out.println("No tracks to play");
            return;
        }

        reloadSongLength();

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

            seeker.set(Math.max(pausedFrame, 0f));

            while (!stopRequested) {
                int writtenFrames = floatToInt(pausedFrame);
                int lastWrittenFrames = floatToInt(pausedFrame);

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

                    pausedFrame = writtenFrames;

                    // update every X cycles
                    if (writtenFrames - lastWrittenFrames >= bufferFrames*4) {
                        seeker.set(pausedFrame);
                        lastWrittenFrames = writtenFrames;
                    }


                }

                if (pauseRequested) {
                    line.stop();

                    while (pauseRequested && !stopRequested) {
                        Thread.sleep(20);
                    }

                    if (stopRequested) break;

                    line.start();
                } else if (!stopRequested) {
                    line.drain();
                    seeker.set(0);
                    pausedFrame = 0f;
                    line.stop();
                    line.flush();
                    line.start();
                }
                line.flush();

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


    public void clearTracks() {
        tracks.clear();
        songLength = 0;
        seeker.set(0f);
        songLengthProperty.set(1f);
        pausedFrame = 0f;
    }

    public void setTracks(List<Track> tracks) {
        clearTracks();
        this.tracks.addAll(tracks);
        if (!this.tracks.isEmpty()) {
            songLength = Collections.max(this.tracks).getLength();
            songLengthProperty.set(songLength * NORMALISED_FRAME_RATE);
        }
    }

    public void addTrack(Track track) {
        this.tracks.add(track);
        songLength = Math.max(1, Collections.max(this.tracks).getLength());
        songLengthProperty.set(songLength * NORMALISED_FRAME_RATE);
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

    public float getPausedFrame() {
        return pausedFrame;
    }

    public void setPausedFrame(float pausedFrame) {
        this.pausedFrame = Math.max(0, pausedFrame);
        this.seeker.set(this.pausedFrame);
    }

    public boolean isPaused() {
        return pauseRequested;
    }

    public boolean isStopRequested() {
        return stopRequested;
    }

    private byte[] getMixedTracks() {
        byte[] mixed = new byte[floatToInt(songLength * NORMALISED_FRAME_RATE)];

        for (Track track : tracks) {
            mixPCM16Stereo(mixed, getTrackInPCM(track), mixed);
        }

        return mixed;
    }

    private byte[] getTrackInPCM(Track track) {
        byte[] bufferTrack = new byte[floatToInt(songLength * NORMALISED_FRAME_RATE)];

        for (Clip clip : track.getClips()) {
            int clipBytes = safeEvenByteCount(floatToInt(clip.getLength() * NORMALISED_FRAME_RATE));
            int startByte = safeEvenByteCount(floatToInt(clip.getTimelineMsPosition() * NORMALISED_FRAME_RATE));
            int audioStartByte = safeEvenByteCount(floatToInt(clip.getAudioStartMs() * NORMALISED_FRAME_RATE));

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

    private static void mixPCM16Stereo(byte[] in1, byte[] in2, byte[] out) {
        int limit = out.length - (out.length % 4);
        for (int i = 0; i < limit; i += 4) {
            int l1 = pcm16ToIntLE(in1, i);
            int l2 = pcm16ToIntLE(in2, i);
            int mixL = (l1 + l2) / 2;
            mixL = Math.max(Short.MIN_VALUE, Math.min(mixL, Short.MAX_VALUE));
            intToPCM16LE(mixL, out, i);

            int r1 = pcm16ToIntLE(in1, i + 2);
            int r2 = pcm16ToIntLE(in2, i + 2);
            int mixR = (r1 + r2) / 2;
            mixR = Math.max(Short.MIN_VALUE, Math.min(mixR, Short.MAX_VALUE));
            intToPCM16LE(mixR, out, i + 2);
        }
    }

    private static int pcm16ToIntLE(byte[] buf, int idx) {
        if (idx < 0 || idx + 1 >= buf.length) {
            return 0;
        }
        int lo = buf[idx] & 0xFF;
        int hi = buf[idx + 1];
        return (hi << 8) | lo;
    }

    private static void intToPCM16LE(int val, byte[] buf, int idx) {
        if (idx < 0 || idx + 1 >= buf.length) {
            return;
        }
        buf[idx] = (byte) (val);
        buf[idx + 1] = (byte) (val >> 8);
    }

    public void exportToWav(Path outputFile) {
        if (tracks.isEmpty()) {
            return;
        }

        songLength = Collections.max(tracks).getLength();
        songLengthProperty.set(songLength * NORMALISED_FRAME_RATE);
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

    private int floatToInt(float value) {
        return Math.max(0, Math.round(value));
    }

    private int safeEvenByteCount(int bytes) {
        return bytes - (bytes % 2);
    }
}
