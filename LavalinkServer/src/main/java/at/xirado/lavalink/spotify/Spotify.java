package at.xirado.lavalink.spotify;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.DataFormatTools;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import lavalink.server.config.SpotifyConfig;
import org.apache.hc.core5.http.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.PlaylistTrack;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.requests.authorization.client_credentials.ClientCredentialsRequest;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class Spotify implements AudioSourceManager
{
    public static final Pattern SPOTIFY_URL_PATTERN = Pattern.compile("(https?://)?(www\\.)?open\\.spotify\\.com/(user/[a-zA-Z0-9-_]+/)?(?<type>track|album|playlist)/(?<identifier>[a-zA-Z0-9-_]+)");
    private static final Logger log = LoggerFactory.getLogger(Spotify.class);

    public SpotifyApi spotify;
    private ClientCredentialsRequest clientCredentialsRequest;
    public AudioPlayerManager manager;

    public Spotify(SpotifyConfig config, AudioPlayerManager manager)
    {
        log.info("Starting Spotify...");

        if (config.getClientId() == null || config.getClientId().isEmpty())
        {
            log.error("No spotify client id found in configuration!");
            return;
        }

        if (config.getClientSecret() == null || config.getClientSecret().isEmpty())
        {
            log.error("No spotify client secret found in configuration!");
            return;
        }

        this.spotify = new SpotifyApi.Builder().setClientId(config.getClientId()).setClientSecret(config.getClientSecret()).build();
        this.clientCredentialsRequest = this.spotify.clientCredentials().build();
        this.manager = manager;

        var thread = new Thread(() -> {
            try {
                while (true) {
                    try {
                        var clientCredentials = this.clientCredentialsRequest.execute();
                        this.spotify.setAccessToken(clientCredentials.getAccessToken());
                        Thread.sleep((clientCredentials.getExpiresIn() * 1000)-5000);
                    } catch (IOException | SpotifyWebApiException | ParseException e) {
                        log.error("Failed to update the spotify access token. Retrying in 1 minute ", e);
                        Thread.sleep(60 * 1000);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to update the spotify access token", e);
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public String getSourceName()
    {
        return "spotify";
    }

    @Override
    public AudioItem loadItem(AudioPlayerManager manager, AudioReference reference) {
        if (this.spotify == null) {
            return null;
        }
        var matcher = SPOTIFY_URL_PATTERN.matcher(reference.identifier);
        if (!matcher.find()) {
            return null;
        }

        var id = matcher.group("identifier");
        try {
            switch (matcher.group("type")) {
                case "album":
                    return this.getAlbum(id);

                case "track":
                    return this.getTrack(id);

                case "playlist":
                    return this.getPlaylist(id);
            }
        } catch (IOException | ParseException | SpotifyWebApiException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public SpotifyPlaylist getAlbum(String id) throws IOException, ParseException, SpotifyWebApiException {
        var album = this.spotify.getAlbum(id).build().execute();

        var tracks = new ArrayList<AudioTrack>();
        for (var item : album.getTracks().getItems()) {
            tracks.add(SpotifyTrack.of(item, this));
        }

        return new SpotifyPlaylist(album.getName(), tracks, 0);
    }

    public SpotifyTrack getTrack(String id) throws IOException, ParseException, SpotifyWebApiException {
        var track = this.spotify.getTrack(id).build().execute();
        return SpotifyTrack.of(track, this).setIsrc(track.getExternalIds().getExternalIds().get("isrc"));
    }

    public SpotifyPlaylist getPlaylist(String id) throws IOException, ParseException, SpotifyWebApiException {
        List<PlaylistTrack> playlistTracks = new ArrayList<>();
        var playlist = this.spotify.getPlaylist(id).build().execute();
        int maxPages = 10;
        for (int i = 0; i < 100 * maxPages; i+= 100)
        {
            Paging<PlaylistTrack> result = this.spotify.getPlaylistsItems(id).setQueryParameter("offset", i).build().execute();
            if (result.getItems().length == 0) break;
            playlistTracks.addAll(Arrays.asList(result.getItems()));
        }

        var tracks = new ArrayList<AudioTrack>();
        for (var item : playlistTracks)
        {
            if (!(item.getTrack() instanceof Track))
                continue;
            tracks.add(SpotifyTrack.of((Track) item.getTrack(), this).setIsrc(((Track) item.getTrack()).getExternalIds().getExternalIds().get("isrc")));
        }

        return new SpotifyPlaylist(playlist.getName(), tracks, 0);
    }

    @Override
    public boolean isTrackEncodable(AudioTrack track)
    {
        return true;
    }

    @Override
    public void encodeTrack(AudioTrack track, DataOutput output) throws IOException
    {
        SpotifyTrack spotifyTrack = (SpotifyTrack) track;
        DataFormatTools.writeNullableText(output, spotifyTrack.getISRC());
    }

    @Override
    public AudioTrack decodeTrack(AudioTrackInfo trackInfo, DataInput input) throws IOException
    {
        String isrc = DataFormatTools.readNullableText(input);
        return new SpotifyTrack(trackInfo, this).setIsrc(isrc);
    }

    @Override
    public void shutdown()
    {

    }
}
