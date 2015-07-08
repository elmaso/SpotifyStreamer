package com.example.mariosoberanis.spotifystreamer;

import android.content.Context;
import android.content.Intent;
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
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;

import java.util.ArrayList;
import java.util.List;

import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;


/**
 * A placeholder fragment containing a simple view.
 */
public class SearchActivityFragment extends Fragment
        implements ArtistsAdapter.ClickListener{

    public static final String EXTRA_ARTIST_ID = "no.sindrenm.spotifystreamer.ARTIST_ID";
    public static final String EXTRA_ARTIST_NAME = "no.sindrenm.spotifystreamer.ARTIST_NAME";

    private final String LOG_TAG = SearchActivityFragment.class.getSimpleName();

    private ArtistsAdapter resultsAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.artistfragment_search, container, false);

        RequestQueue requestQueue = Volley.newRequestQueue(getActivity());
        ImageLoader imageLoader = new ImageLoader(requestQueue, new ImageLoader.ImageCache()  {
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
        resultsAdapter.setClickListener(this);

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
        Spotify.getService().searchArtists(query.toString(), new Callback<ArtistsPager>() {

            @Override
            public void success(final ArtistsPager artistsPager, Response response) {
                getActivity().runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        List<Artist> artists = artistsPager.artists.items;

                        if (artists.isEmpty()) {
                            Toast.makeText(
                                    getActivity(),
                                    R.string.toast_no_artist_match,
                                    Toast.LENGTH_SHORT
                            ).show();
                        } else {
                            resultsAdapter.setArtists(artists);
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


    @Override
    public void onArtistItemClicked(View view, Artist artist) {
        Context context = view.getContext();
        Intent intent = new Intent(context, TopTracksActivity.class);

        intent.putExtra(EXTRA_ARTIST_NAME, artist.name);
        intent.putExtra(EXTRA_ARTIST_ID, artist.id);

        context.startActivity(intent);
    }
}


