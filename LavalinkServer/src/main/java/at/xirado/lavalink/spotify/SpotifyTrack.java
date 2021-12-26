package at.xirado.lavalink.spotify;

import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.track.*;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;
import lavalink.server.config.SpotifyConfig;
import se.michaelthelin.spotify.model_objects.specification.ArtistSimplified;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.model_objects.specification.TrackSimplified;

public class SpotifyTrack extends DelegatedAudioTrack {

    private final Spotify spotify;

    public SpotifyTrack(String title, String identifier, ArtistSimplified[] artists, Integer trackDuration, Spotify spotify) {
        this(new AudioTrackInfo(title, artists[0].getName(), trackDuration.longValue(), identifier, false, "https://open.spotify.com/track/" + identifier), spotify);
    }

    public SpotifyTrack(AudioTrackInfo trackInfo, Spotify spotify) {
        super(trackInfo);
        this.spotify = spotify;
    }

    public static SpotifyTrack of(TrackSimplified track, Spotify spotify) {
        return new SpotifyTrack(track.getName(), track.getId(), track.getArtists(), track.getDurationMs(), spotify);
    }

    public static SpotifyTrack of(Track track, Spotify spotify) {
        return new SpotifyTrack(track.getName(), track.getId(), track.getArtists(), track.getDurationMs(), spotify);
    }

    @Override
    public void process(LocalAudioTrackExecutor executor) throws Exception {
        if (this.spotify.manager == null)
        {
            System.out.println("MANAGER IS NULL");
            return;
        }

        if (this.spotify.manager.source(YoutubeAudioSourceManager.class) == null)
        {
            System.out.println("YOUTUBE AUDIO SOURCE MANAGER NULL");
            return;
        }

        var track = this.spotify.manager.source(YoutubeAudioSourceManager.class).loadItem(this.spotify.manager, new AudioReference("ytsearch:" + trackInfo.title + " " + trackInfo.author, null));
        if (track == null) {
            throw new RuntimeException("No matching youtube track found");
        }
        if (track instanceof AudioPlaylist) {
            track = ((AudioPlaylist) track).getTracks().get(0);
        }
        if (track instanceof InternalAudioTrack) {
            processDelegate((InternalAudioTrack) track, executor);
            return;
        }
        throw new RuntimeException("No matching youtube track found");
    }

    @Override
    public AudioSourceManager getSourceManager() {
        return this.spotify;
    }

}
