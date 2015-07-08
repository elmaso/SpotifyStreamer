package com.example.mariosoberanis.spotifystreamer;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.util.LruCache;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.Tracks;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by mariosoberanis on 7/7/15.
 */
public class TopTracksActivityFragment extends Fragment {
    private final String LOG_TAG = TopTracksActivityFragment.class.getSimpleName();

    private TracksAdapter resultsAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_top_tracks, container, false);
        Artist artist = new Artist();
        artist.id = getActivity().getIntent()
                .getStringExtra(SearchActivityFragment.EXTRA_ARTIST_ID);
        artist.name = getActivity().getIntent()
                .getStringExtra(SearchActivityFragment.EXTRA_ARTIST_NAME);

        RequestQueue requestQueue = Volley.newRequestQueue(getActivity());
        ImageLoader imageLoader = new ImageLoader(requestQueue, new ImageLoader.ImageCache() {
            private final LruCache<String, Bitmap> cache = new LruCache<>(10);

            public void putBitmap(String url, Bitmap bitmap) {
                cache.put(url, bitmap);
            }

            public Bitmap getBitmap(String url) {
                return cache.get(url);
            }
        });

        resultsAdapter = new TracksAdapter(new ArrayList<Track>());
        resultsAdapter.setImageLoader(imageLoader);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.top_tracks_list);

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(resultsAdapter);

        getTopTracks(artist);

        return view;

        }

    private void getTopTracks(Artist artist) {
        Map<String, Object> params = new HashMap<>();
        params.put("country", "NO"); // TODO: Allow user to configure country

        Spotify.getService().getArtistTopTrack(artist.id, params, new Callback<Tracks>() {

            @Override
            public void success(final Tracks tracks, Response response) {
                getActivity().runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        List<Track> tracksList = tracks.tracks;

                        if (tracksList.isEmpty()) {
                            Toast.makeText(
                                    getActivity(),
                                    R.string.toast_no_top_tracks,
                                    Toast.LENGTH_SHORT
                            ).show();
                        } else {
                            resultsAdapter.setTracks(tracksList);
                            resultsAdapter.notifyDataSetChanged();
                        }
                    }

                });
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e(LOG_TAG, error.getLocalizedMessage());

            }

        });
    }

}

