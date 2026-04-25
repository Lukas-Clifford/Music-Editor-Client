package app.musiceditorclient.services;

import app.musiceditorclient.EditorContext;
import app.musiceditorclient.view.TrackPane;
import javafx.application.Platform;

public class PlaybackService {
    private final EditorContext context;
    public PlaybackService(EditorContext context) {
        this.context = context;
    }


    public void startPlayback() {
        if (context.playback().isPlaybackRunning()) {
            return;
        }

        context.playback().setPlaybackRunning(true);


        context.playback().setPlaybackThread(new Thread(() -> {
            try {
                context.playback().getPlaybackEngine().clearPauseRequest();
                context.playback().getPlaybackEngine().play();
            } finally {
                context.playback().setPlaybackRunning(false);
            }
        }));

        context.playback().getPlaybackThread().setDaemon(true);
        context.playback().getPlaybackThread().start();
    }

    public void pausePlayback() {
        context.playback().getPlaybackEngine().requestPause();

    }

    public void reloadPlaybackEngine() {
        context.playback().getPlaybackEngine().clearTracks();
        for (TrackPane trackPane : context.project().getTrackPanes()) {
            if (!trackPane.isMuted())
                context.playback().getPlaybackEngine().addTrack(trackPane.getTrack());
        }
        context.playback().getPlaybackEngine().reloadSongLength();
    }

    public void stopPlaybackForEdit() {
        if (context.playback().getPlaybackEngine() != null) {
            context.playback().getPlaybackEngine().requestStop();
        }

        context.playback().setPlaybackRunning(false);
        reloadPlaybackEngine();
    }


}
