package com.example.mariosoberanis.spotifystreamer;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.util.LruCache;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;

import java.util.ArrayList;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;


/**
 * A placeholder fragment containing a simple view.
 */
public class SearchActivityFragment extends Fragment {

    private final String LOG_TAG = SearchActivityFragment.class.getSimpleName();

    private RequestQueue requestQueue;

    private ArtistsAdapter resultsAdapter;

    private ImageLoader imageLoader;

    private SpotifyService spotifyService;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.artistfragment_search, container, false);

        requestQueue = Volley.newRequestQueue(getActivity());
        imageLoader = new ImageLoader(requestQueue, new ImageLoader.ImageCache() {
            private final LruCache<String, Bitmap> cache = new LruCache<>(10);

            public void putBitmap(String url, Bitmap bitmap) {
                cache.put(url, bitmap);
            }

            public Bitmap getBitmap(String url) {
                return cache.get(url);
            }
        });

        resultsAdapter = new ArtistsAdapter(new ArrayList<Artist>());
        resultsAdapter.setImageLoader(imageLoader);

            resultsAdapter = new ArtistsAdapter(new ArrayList<Artist>());
            resultsAdapter.setImageLoader(imageLoader);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.artist_search_results_list);

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(resultsAdapter);

        EditText editText = (EditText) view.findViewById(R.id.search_edit_text);
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                // Guard against sending multiple requests when triggered by a down key action
                if (event.getAction() == KeyEvent.ACTION_DOWN) return false;

                searchArtists(v.getText());
                return true;
            }

        });

        return view;

        }


    private void searchArtists(CharSequence query) {
        getSpotifyService().searchArtists(query.toString(), new Callback<ArtistsPager>() {

            @Override
            public void success(final ArtistsPager artistsPager, Response response) {
                getActivity().runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        resultsAdapter.setArtists(artistsPager.artists.items);
                    }

                });
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e(LOG_TAG, error.getLocalizedMessage());
            }

        });
    }


    private SpotifyService getSpotifyService() {
        if (spotifyService != null) return spotifyService;

        SpotifyApi api = new SpotifyApi();
        spotifyService = api.getService();
        return spotifyService;
    }
}


