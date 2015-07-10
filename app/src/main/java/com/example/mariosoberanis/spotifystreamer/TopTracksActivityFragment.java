package com.example.mariosoberanis.spotifystreamer;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.Tracks;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by mariosoberanis on 7/7/15.
 */
public class TopTracksActivityFragment extends Fragment {
    private IconicAdapter trackResultListViewAdapter = null;
    private String artistId = null;

    private final String LOG_TAG = TopTracksActivityFragment.class.getSimpleName();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        ArrayList<TrackParcelable> trackParcelables = null;

        View rootView = inflater.inflate(R.layout.fragment_top_tracks, container, false);

        Intent intent = getActivity().getIntent();
        if(intent != null && intent.hasExtra(Intent.EXTRA_SHORTCUT_NAME)) {
            artistId = intent.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
        }

        if(savedInstanceState == null || !savedInstanceState.containsKey("tracks_key")) {
            trackParcelables = new ArrayList<TrackParcelable>();
            performSearch(artistId);
        }
        else {
            trackParcelables = savedInstanceState.getParcelableArrayList("tracks_key");
        }

        ListView listView = (ListView) rootView.findViewById(R.id.listViewOfTopTracks);
        trackResultListViewAdapter = new IconicAdapter(trackParcelables);
        listView.setAdapter(trackResultListViewAdapter);

        return rootView;
    }



    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList("tracks_key",
                trackResultListViewAdapter.getTrackParcelables());
        super.onSaveInstanceState(outState);
    }

    private ArrayList<String> getTrackNamesFromParcelables(ArrayList<TrackParcelable>
                                                                   trackParcelables){

        ArrayList<String> trackNames = new ArrayList<>();
        for(TrackParcelable element : trackParcelables){
            trackNames.add(element.name);
        }
        return trackNames;
    }

    private void performSearch(String artistId) {
        SpotifyApi api = new SpotifyApi();
        SpotifyService spotify = api.getService();

        Map<String, Object> options = new HashMap<>();
        options.put("country", "US");
        spotify.getArtistTopTrack(artistId, options, new Callback<Tracks>() {
            @Override
            public void success(Tracks tracks, Response response) {
                final ArrayList<TrackParcelable> trackParcelables =
                        new ArrayList<TrackParcelable>();
                for (Track track : tracks.tracks) {
                    trackParcelables.add(new TrackParcelable(track.name,track.album.name,
                            track.album.images.get(0).url,track.preview_url));
                }
                trackResultListViewAdapter.swapItems(trackParcelables);
                Log.d(LOG_TAG,trackParcelables.toString());

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(trackParcelables.size()==0){
                            Toast.makeText(getActivity(),
                                    getString(R.string.toast_no_artist_match),
                                    Toast.LENGTH_SHORT).show();
                        }
                        trackResultListViewAdapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void failure(RetrofitError error) {

            }
        });

    }
    //This IconicAdapter Pattern is from Busy Android Coder's Guide page 272 of book version 6.7
    class IconicAdapter extends ArrayAdapter<TrackParcelable> {

        private ArrayList<TrackParcelable> trackParcelables;

        public IconicAdapter(ArrayList<TrackParcelable> trackParcelables) {
            /*
            Normally this 0 in the super constructor would be the id of the textView we are updating,
            but since we are using a custom Adapter, this is no longer appropriate. So we can just
            put an arbitrary value here.
             */
            super(getActivity(), 0, trackParcelables);
            this.trackParcelables = trackParcelables;
        }

        public void swapItems(ArrayList<TrackParcelable> trackParcelables) {
            this.trackParcelables.clear();
            this.trackParcelables.addAll(trackParcelables);

        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            TrackParcelable track = getItem(position);

            //View Holder Pattern
            if (convertView == null) {
                Log.d("convertView", "null");
                convertView = LayoutInflater.from(getContext()).inflate(
                        R.layout.search_result_track_item,
                        parent, false);
            }

            ImageView albumImage = (ImageView) convertView.findViewById(R.id.imageViewAlbum);
            Picasso.with(getActivity()).load(track.albumImageUrl).into(albumImage);

            TextView trackName = (TextView) convertView.findViewById(R.id.textViewTrackTitle);
            trackName.setText(track.name);

            TextView trackAlbum = (TextView) convertView.findViewById(R.id.textViewTrackAlbum);
            trackName.setText(track.albumName);

            return convertView;

        }

        public ArrayList<TrackParcelable> getTrackParcelables(){
            return trackParcelables;
        }

    }

}

