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

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;


/**
 * A placeholder fragment containing a simple view.
 */
public class ArtistSearchFragment extends Fragment {

    private final String LOG_TAG = ArtistSearchFragment.class.getSimpleName();

    private RequestQueue requestQueue;

    private ArtistAdapter resultsAdapter;

    private ImageLoader imageLoader;


        @Override
        public View onCreateView (LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState){

            View view = inflater.inflate(R.layout.artistfragment_main, container, false);

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

            resultsAdapter = new ArtistAdapter(new ArrayList<Artist>());

            LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
            RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.artist_search_results_list);

            recyclerView.setLayoutManager(layoutManager);
            recyclerView.setAdapter(resultsAdapter);

            EditText editText = (EditText) view.findViewById(R.id.search_edit_text);
            editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {

                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    searchArtists(v.getText());
                    return true;
                }

            });

            return view;

        }

    public void searchArtists(CharSequence query) {
        String BASE_URL = "https://api.spotify.com/v1/search";

        String url = null;
        try {
            url = BASE_URL + "?type=artist&q=" + URLEncoder.encode(query.toString(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        JsonObjectRequest jsonRequest = new JsonObjectRequest(
                Request.Method.GET, url, null,

                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject object) {
                        ArrayList<Artist> artists = new ArrayList<>();

                        try {
                            JSONObject artistsWrapper = object.getJSONObject("artists");
                            JSONArray artistsArray = artistsWrapper.getJSONArray("items");

                            for (int i = 0; i < artistsArray.length(); i++) {
                                JSONObject artistObject = artistsArray.getJSONObject(i);
                                artists.add(Artist.fromJSONObject(artistObject));
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        resultsAdapter.setImageLoader(imageLoader);
                        resultsAdapter.setArtists(artists);
                    }

                },

                new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        Log.e(LOG_TAG, volleyError.getLocalizedMessage());
                    }

                }
        );

        requestQueue.add(jsonRequest);
    }
}


