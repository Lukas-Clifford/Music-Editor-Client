package app.musiceditorclient;

import app.musiceditorclient.models.Clip;
import app.musiceditorclient.models.Track;

import javax.sound.sampled.*;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class PlaybackEngine {

    private final int FRAME_RATE = 48;
    private final int SAMPLE_RATE = 48;
    private final int FRAME_SIZE = 6;
    private final int NORMALISED_FRAME_RATE = FRAME_RATE * FRAME_SIZE;

    private final AudioFormat format = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED, // encoding
            SAMPLE_RATE*1000,                // sampleRate (Hz)
            24,                              // sampleSizeInBits
            2,                               // channels (2 = stereo)
            6,                               // frameSize (bytes por frame)
            FRAME_RATE*1000,                 // frameRate
            false                            // bigEndian (false = littleEndian)
    );

    private final DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);


    private List<Track> tracks;
    private int songLength = 0;

    public PlaybackEngine(List<Track> tracks) {
        this.tracks = tracks;

        songLength = Collections.max(tracks).getLength();

    }

    public void play() {
        byte[] mixed = getMixedTracks();

        try (SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info)) {
            line.open(format);
            line.start();

            line.flush();
            line.write(mixed, 0, mixed.length);

            line.drain();
            line.stop();
        } catch (LineUnavailableException e) {
            throw new RuntimeException(e);
        }
    }

    public void playLoop(int iterations) {
        byte[] mixed = getMixedTracks();

        try (SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info)) {
            line.open(format);
            line.start();

            for (int i = 0; i < iterations; i++) {
                line.flush();
                line.write(mixed, 0, mixed.length);
            }

            line.drain();
            line.stop();
        } catch (LineUnavailableException e) {
            throw new RuntimeException(e);
        }
    }

    public void playLoop() {
        byte[] mixed = getMixedTracks();

        try (SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info)) {
            line.open(format);
            line.start();
            try{
                while (true) {
                    line.flush();
                    line.write(mixed, 0, mixed.length);
                }
            } finally {

                line.drain();
                line.stop();

            }

        } catch (LineUnavailableException e) {
            throw new RuntimeException(e);
        }
    }


    private byte[] getMixedTracks() {
        byte[] mixed = new byte[ songLength * NORMALISED_FRAME_RATE ];

        for (Track track:tracks) mixPCM24Stereo(mixed, getTrackInPCM(track), mixed);

        return mixed;
    }

    private byte[] getTrackInPCM(Track track){
        byte[] bufferTrack = new byte[songLength * NORMALISED_FRAME_RATE];

        for ( Clip clip: track.getClips() ) {
            int frames = clip.getLength() * NORMALISED_FRAME_RATE;
            int startByte = clip.getTimelineMsPosition() * NORMALISED_FRAME_RATE;

            try (AudioInputStream ais = AudioSystem.getAudioInputStream(clip.getWavFile())) {

                byte[] buffer = new byte[frames];
                int read = ais.read(buffer, 0, frames);

                if (read > 0) {
                    for (int b = 0; b < read; b++) {
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
        for (int i = 0; i < out.length; i += 3) {
            int v1 = pcm24ToIntLE(in1, i);
            int v2 = pcm24ToIntLE(in2, i);
            int mix = (v1 + v2) / 2;
            mix = Math.max(-8388608, Math.min(mix, 8388607));
            intToPCM24LE(mix, out, i);
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


}
