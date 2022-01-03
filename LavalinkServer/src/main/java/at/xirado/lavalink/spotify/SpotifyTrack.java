package at.xirado.lavalink.spotify;

import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.track.*;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.michaelthelin.spotify.model_objects.specification.ArtistSimplified;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.model_objects.specification.TrackSimplified;

public class SpotifyTrack extends DelegatedAudioTrack {

    private static final Logger log = LoggerFactory.getLogger(SpotifyTrack.class);

    private final Spotify spotify;
    private String isrc = null;

    public SpotifyTrack(String title, String identifier, ArtistSimplified[] artists, Integer trackDuration, Spotify spotify) {
        this(new AudioTrackInfo(title, artists[0].getName(), trackDuration.longValue(), identifier, false, "https://open.spotify.com/track/" + identifier), spotify);
    }

    public SpotifyTrack(AudioTrackInfo trackInfo, Spotify spotify) {
        super(trackInfo);
        this.spotify = spotify;
    }

    public SpotifyTrack setIsrc(String isrc)
    {
        this.isrc = isrc;
        return this;
    }

    public String getISRC()
    {
        return isrc;
    }

    public boolean hasISRC()
    {
        return isrc != null;
    }

    public static SpotifyTrack of(TrackSimplified track, Spotify spotify) {
        return new SpotifyTrack(track.getName(), track.getId() != null ? track.getId() : track.getUri(), track.getArtists(), track.getDurationMs(), spotify);
    }

    public static SpotifyTrack of(Track track, Spotify spotify) {
        return new SpotifyTrack(track.getName(), track.getId() != null ? track.getId() : track.getUri(), track.getArtists(), track.getDurationMs(), spotify);
    }

    @Override
    public void process(LocalAudioTrackExecutor executor) throws Exception
    {
        AudioItem delegate = processDelegate();

        if (delegate == null)
            throw new RuntimeException("No matching youtube track found");
        if (delegate instanceof AudioPlaylist)
            delegate = ((AudioPlaylist) delegate).getTracks().get(0);
        if (delegate instanceof InternalAudioTrack) {
            processDelegate((InternalAudioTrack) delegate, executor);
            return;
        }
        throw new RuntimeException("No matching youtube track found");
    }

    @Override
    public AudioSourceManager getSourceManager() {
        return this.spotify;
    }

    private boolean hasResult(AudioItem item)
    {
        return item instanceof AudioTrack || item instanceof AudioPlaylist;
    }

    private AudioItem processDelegate()
    {
        YoutubeAudioSourceManager asm = spotify.manager.source(YoutubeAudioSourceManager.class);
        AudioItem audioItem = null;
        if (hasISRC())
            audioItem = asm.loadItem(spotify.manager, new AudioReference("ytsearch:\"" + getISRC() + "\"", null));
        if (!hasResult(audioItem))
            audioItem = asm.loadItem(spotify.manager, new AudioReference("ytsearch:" + trackInfo.title + " " + trackInfo.author, null));
        if (!hasResult(audioItem))
            return null;
        return audioItem;
    }
}
