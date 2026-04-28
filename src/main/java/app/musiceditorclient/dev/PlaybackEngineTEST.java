package app.musiceditorclient.dev;

import app.musiceditorclient.PlaybackEngine;
import app.musiceditorclient.infrastructure.FfmpegInstaller;
import app.musiceditorclient.models.Clip;
import app.musiceditorclient.models.Track;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class PlaybackEngineTEST {
    public static void main(String[] args) throws IOException {

        FfmpegInstaller.ensureInstalled();

        File hat = new File("/home/clifford/Música/90s MPC Sample Pack/Hats/Hi Hat 1.wav");

        Track track = new Track(
                List.of(
                    new Clip(hat, 0),
                    new Clip(hat, 250),
                    new Clip(hat, 500),
                    new Clip(hat, 750),
                    new Clip(hat, 1000),
                    new Clip(hat, 1250),
                    new Clip(hat, 1500),
                    new Clip(hat, 1750)
                )
        );

        PlaybackEngine pe = new PlaybackEngine(List.of(track));
        pe.play();

    }
}
