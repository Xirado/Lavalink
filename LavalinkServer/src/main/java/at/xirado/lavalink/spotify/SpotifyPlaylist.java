package at.xirado.lavalink.spotify;

import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.util.List;

public class SpotifyPlaylist implements AudioPlaylist {

    private final String name;
    private final List<AudioTrack> tracks;
    private final int selectedTrack;

    public SpotifyPlaylist(String name, List<AudioTrack> tracks, int selectedTrack) {
        this.name = name;
        this.tracks = tracks;
        this.selectedTrack = selectedTrack;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public List<AudioTrack> getTracks() {
        return this.tracks;
    }

    @Override
    public AudioTrack getSelectedTrack() {
        if (this.selectedTrack == -1) {
            return null;
        }
        return this.tracks.get(this.selectedTrack);
    }

    @Override
    public boolean isSearchResult() {
        return false;
    }

}
