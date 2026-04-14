package app.musiceditorclient;

import app.musiceditorclient.infrastructure.FfmpegInstaller;
import app.musiceditorclient.models.Clip;
import app.musiceditorclient.models.Track;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class PlaybackEngineTEST {
    public static void main(String[] args) throws IOException {

        FfmpegInstaller.ensureInstalled();

        File kick = new File("/home/clifford/IdeaProjects/Music-Editor-Client/src/main/resources/app/musiceditorclient/samples/90s MPC Sample Pack/Kick/Kick 1.wav");
        File snare = new File("/home/clifford/IdeaProjects/Music-Editor-Client/src/main/resources/app/musiceditorclient/samples/90s MPC Sample Pack/Hats/Hi Hat 1.wav");

        Track track1 = new Track(
                List.of(
                    new Clip(kick, 0),
                    new Clip(kick, 500),
                    new Clip(kick, 1000),
                    new Clip(kick, 1500)
                )
        );

        Track track2 = new Track(
                List.of(
                    new Clip(snare, 0),
                    new Clip(snare, 250),
                    new Clip(snare, 500),
                    new Clip(snare, 750),
                    new Clip(snare, 1000),
                    new Clip(snare, 1250),
                    new Clip(snare, 1500),
                    new Clip(snare, 1750)
                )
        );

        track1.setLength(2500);

//        PlaybackEngine pe = new PlaybackEngine(List.of(track1,track2));

//        pe.play();

    }
}
