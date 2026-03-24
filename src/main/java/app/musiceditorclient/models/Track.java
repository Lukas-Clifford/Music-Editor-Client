package app.musiceditorclient.models;

import java.util.ArrayList;
import java.util.List;

public class Track implements Comparable<Track> {

    private List<Clip> clips;
    private int length = 0;

    public Track(List<Clip> clips) {
        this.clips = clips.stream().sorted().toList();
        this.length = clips.getLast().getEndPosition();
    }



    public List<Clip> getClips() {
        return clips;
    }

    public void setClips(List<Clip> clips) {
        this.clips = clips;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }


    @Override
    public int compareTo(Track o) {
        return Integer.compare(this.length, o.length);
    }
}
