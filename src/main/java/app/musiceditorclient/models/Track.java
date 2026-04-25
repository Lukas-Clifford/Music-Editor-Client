package app.musiceditorclient.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Track implements Comparable<Track>, Serializable {

    private List<Clip> clips;
    private int length = 0;

    public Track(List<Clip> clips) {
        this.clips = new ArrayList<>(clips.stream().sorted().toList());
        this.length = (clips.isEmpty()) ? 0:clips.getLast().getEndPosition();
    }

    public Track(){
        clips = new ArrayList<>();
    }


    public List<Clip> getClips() {
        return clips;
    }

    public void setClips(List<Clip> clips) {
        this.clips = clips;
    }

    public int getLength() {
        this.clips = new ArrayList<>(clips.stream().sorted().toList());
        return (clips.isEmpty()) ? 0:clips.getLast().getEndPosition();
    }

    public void setLength(int length) {
        this.length = length;
    }

    public void addClip(Clip clip) {
        this.clips.add(clip);
        this.length = getLength();
    }

    public void removeClip(Clip clip){
        this.clips.remove(clip);
    }

    @Override
    public int compareTo(Track o) {
        return Integer.compare(this.getLength(), o.getLength());
    }

    @Override
    public String toString() {
        return clips.toString();
    }
}
