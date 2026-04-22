package app.musiceditorclient.state;

import app.musiceditorclient.PlaybackEngine;
import app.musiceditorclient.view.TimelineSeekerPane;

public class PlaybackState {

    private PlaybackEngine playbackEngine;
    private Thread playbackThread;
    private volatile boolean playbackRunning = false;
    private TimelineSeekerPane timelineSeekerPane;

    public PlaybackEngine getPlaybackEngine() {
        return playbackEngine;
    }

    public void setPlaybackEngine(PlaybackEngine playbackEngine) {
        this.playbackEngine = playbackEngine;
    }

    public Thread getPlaybackThread() {
        return playbackThread;
    }

    public void setPlaybackThread(Thread playbackThread) {
        this.playbackThread = playbackThread;
    }

    public boolean isPlaybackRunning() {
        return playbackRunning;
    }

    public void setPlaybackRunning(boolean playbackRunning) {
        this.playbackRunning = playbackRunning;
    }

    public TimelineSeekerPane getTimelineSeekerPane() {
        return timelineSeekerPane;
    }

    public void setTimelineSeekerPane(TimelineSeekerPane timelineSeekerPane) {
        this.timelineSeekerPane = timelineSeekerPane;
    }
}
