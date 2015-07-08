package com.example.mariosoberanis.spotifystreamer;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;

/**
 * Created by mariosoberanis on 7/7/15.
 */
public class Spotify {
    private static SpotifyService service;

    public static SpotifyService getService() {
        if (service != null) return service;

        SpotifyApi api = new SpotifyApi();
        service = api.getService();
        return service;
    }

    private Spotify() {}
}
