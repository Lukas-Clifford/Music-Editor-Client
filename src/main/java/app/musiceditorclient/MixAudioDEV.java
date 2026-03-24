package app.musiceditorclient;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MixAudioDEV {

    record Clip(int id, int startingMillisecondsSeconds, int endingMilliseconds) {}

    record Segment(int startMs, int endMsExclusive, int clipId) {
        public int lengthMs() { return endMsExclusive - startMs; }
    }

    public static void main(String[] args) throws Exception {

        File kick = new File("/home/clifford/IdeaProjects/Music-Editor-Client/src/main/resources/app/musiceditorclient/samples/90s MPC Sample Pack/Kick/Kick 1.wav");
        File snare = new File("/home/clifford/IdeaProjects/Music-Editor-Client/src/main/resources/app/musiceditorclient/samples/90s MPC Sample Pack/Snare/Snare 1.wav");

        List<File> samples = List.of(kick, snare);

        int loopMs = 2500;
        int frameRate = 48;
        int frameSize = 6;

//        List<Clip> clips = List.of(
//                new Clip(1, kick, 0,   80),     // beat 1
//                new Clip(2, snare, 500, 580),   // beat 2
//                new Clip(1, kick, 1000,1080),   // beat 3
//                new Clip(2, snare, 1500,1580)   // beat 4
//        );

        // 2. Definir tracks (dos ejemplos simples)
        List<Clip> track1 = List.of(
                new Clip(0, 0, 140),
                new Clip(0, 500, 640),
                new Clip(0, 1000, 1140),
                new Clip(0, 1500, 1640)
        );
        List<Clip> track2 = List.of(
                new Clip(1, 250, 420),
                new Clip(1, 500, 710),
                new Clip(1, 750, 970),
                new Clip(1, 1500, 1710)
        );

        List<Segment> segments1 = buildSegments(track1, loopMs);
        List<Segment> segments2 = buildSegments(track2, loopMs);



        AudioFormat format;
        try (AudioInputStream firstAis = AudioSystem.getAudioInputStream(samples.getFirst())) {
            format = firstAis.getFormat();}

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);


        int totalFrames = loopMs * frameRate;
        byte[] pcmBufferTrack1 = new byte[totalFrames * frameSize];
        byte[] pcmBufferTrack2 = new byte[totalFrames * frameSize];

        renderTrackToPCM(pcmBufferTrack1, segments1, samples, frameSize, frameRate);
        renderTrackToPCM(pcmBufferTrack2, segments2, samples, frameSize, frameRate);

        // 4. Mezclar (suma, headroom, clip)
        byte[] mixed = new byte[totalFrames * frameSize];
        mixPCM24Stereo(pcmBufferTrack1, pcmBufferTrack2, mixed);


        try (SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info)) {
            line.open(format);
            line.start();

            System.out.println("Playing mixed buffer...");

            // Loop 4 veces el patrón mezclado
            for (int i = 0; i < 4; i++) {
                line.flush();
                line.write(mixed, 0, mixed.length);
            }

            line.drain();
            line.stop();
        }
    }

    // Renderiza un track: suma en el array los segmentos de audio en el momento correcto
    private static void renderTrackToPCM(byte[] out, List<Segment> segments, List<File> samples, int frameSize, int frameRate) throws IOException, UnsupportedAudioFileException {
        for (Segment segment : segments) {
            int frames = segment.lengthMs() * frameRate;
            int startByte = segment.startMs * frameRate * frameSize;
            if (segment.clipId == -1) {
                // silencio, out ya está a cero por defecto; no hace falta nada
                continue;
            }
            try (AudioInputStream ais = AudioSystem.getAudioInputStream(samples.get(segment.clipId))) {

                int toRead = frames * frameSize;
                byte[] buffer = new byte[toRead];
                int read = ais.read(buffer, 0, toRead);
                if (read > 0) {
                    // Suma (mezcla) buffer en 'out' (si no quieres suma, puedes solo copiar)
                    for (int b=0; b < read; b++) {
                        int sample = out[startByte + b] + buffer[b]; // overflow en byte, aceptable para MVP mono; para pro: usa float!
                        out[startByte + b] = (byte) sample;
                    }
                }
            }
        }
    }

    // Suma dos buffers PCM 24-bit estéreo, hace headroom y clipping básico
    private static void mixPCM24Stereo(byte[] in1, byte[] in2, byte[] out) {
        for (int i = 0; i < out.length; i += 3) {
            int v1 = pcm24ToIntLE(in1, i);
            int v2 = pcm24ToIntLE(in2, i);
            int mix = (v1 + v2) / 2; // headroom simple
            mix = Math.max(-8388608, Math.min(mix, 8388607)); // clip 24-bit signed
            intToPCM24LE(mix, out, i);
        }
    }
    // Convierte 3 bytes little endian a int signed 24 bits
    private static int pcm24ToIntLE(byte[] buf, int idx) {
        int b1 = buf[idx] & 0xFF;
        int b2 = buf[idx + 1] & 0xFF;
        int b3 = buf[idx + 2];
        return (b3 << 16) | (b2 << 8) | b1;
    }
    // Convierte int a 3 bytes little endian
    private static void intToPCM24LE(int val, byte[] buf, int idx) {
        buf[idx]     = (byte) (val);
        buf[idx + 1] = (byte) (val >> 8);
        buf[idx + 2] = (byte) (val >> 16);
    }

    // Igual que antes
    public static List<Segment> buildSegments(List<Clip> clips, int endMsExclusive) {
        List<Clip> sorted = clips.stream().sorted(Comparator.comparingInt(Clip::startingMillisecondsSeconds)).toList();
        List<Segment> out = new ArrayList<>();
        int cursor = 0;
        for (Clip c : sorted) {
            if (c.endingMilliseconds() <= c.startingMillisecondsSeconds()) throw new IllegalArgumentException("Invalid clip: endMs must be > startMs. Clip id=" + c.id());
            if (c.startingMillisecondsSeconds() < cursor) throw new IllegalArgumentException("Overlapping clips. Clip id=" + c.id());
            if (c.startingMillisecondsSeconds() > cursor) out.add(new Segment(cursor, c.startingMillisecondsSeconds(), -1));
            out.add(new Segment(c.startingMillisecondsSeconds(), c.endingMilliseconds(), c.id()));
            cursor = c.endingMilliseconds();
        }
        if (cursor < endMsExclusive) out.add(new Segment(cursor, endMsExclusive, -1));
        return out;
    }
}


