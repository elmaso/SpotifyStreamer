package com.example.mariosoberanis.spotifystreamer;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

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

    private IconicAdapter artistResultListViewAdapter = null;
    private final String fallbackArtistImageUrl= "http://i.imgur.com/UinRWDbb.png";

    private final String LOG_TAG = SearchActivityFragment.class.getSimpleName();

    public SearchActivityFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.artistfragment_search, container, false);

        ArrayList<SearchResultParcelable> searchResultParcelables = null;


        EditText editText = (EditText) rootView.findViewById(R.id.search_edit_text);
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    performSearch(v.getText().toString());
                    return true;
                }
                return false;
            }
        });
        if(savedInstanceState == null || !savedInstanceState.containsKey("search_key")) {
            searchResultParcelables = new ArrayList<SearchResultParcelable>();
        }
        else {
            searchResultParcelables = savedInstanceState.getParcelableArrayList("search_key");
        }

        ListView listView = (ListView) rootView.findViewById(R.id.artist_search_results_list);
        artistResultListViewAdapter = new IconicAdapter(searchResultParcelables);
        listView.setAdapter(artistResultListViewAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent trackIntent = new Intent(getActivity(), TopTracksActivity.class);
                trackIntent.putExtra(Intent.EXTRA_TEXT,
                        artistResultListViewAdapter.getSearchResultParcelables()
                                .get(position).artistName); //extra text = artist name
                trackIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, //shortcut name = artist id
                        artistResultListViewAdapter.getSearchResultParcelables()
                                .get(position).artistId);
                startActivity(trackIntent);
            }
        });

        return rootView;

    }
    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList("search_key",
                artistResultListViewAdapter.getSearchResultParcelables());
        super.onSaveInstanceState(outState);
    }

    private void performSearch(String query) {

        SpotifyApi api = new SpotifyApi();
        SpotifyService spotify = api.getService();

        spotify.searchArtists(query, new Callback<ArtistsPager>() {
            @Override
            public void success(ArtistsPager artistsPager, Response response) {
                //List<Artist> searchResultParcelables = artistsPager.artists.items;
                final ArrayList<SearchResultParcelable> searchResultParcelables =
                        new ArrayList<SearchResultParcelable>();

                List<Artist> artists = artistsPager.artists.items;


                final int artistsReturned = artists.size();

                for (int i = 0; i < artistsReturned; i++) {
                    Artist artist = artists.get(i);

                    String artistImageUrl = "";
                    if (!artist.images.isEmpty()) {
                        artistImageUrl = artist.images.get(0).url;
                    } else {
                        artistImageUrl = fallbackArtistImageUrl;
                    }

                    searchResultParcelables.add(new SearchResultParcelable(artist.name,
                            artistImageUrl, artist.id));
                }

                artistResultListViewAdapter.swapItems(searchResultParcelables);

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (artistsReturned == 0) {

                            Toast.makeText(getActivity(),
                                    getString(R.string.toast_no_artist_match),
                                    Toast.LENGTH_SHORT).show();
                        }
                        artistResultListViewAdapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void failure(RetrofitError error) {
                Log.d("Artist failure", error.toString());
            }
        });
    }


    //This IconicAdapter Pattern is from Busy Android Coder's Guide page 272 of book version 6.7
    class IconicAdapter extends ArrayAdapter<SearchResultParcelable> {

        private ArrayList<SearchResultParcelable> searchResultParcelables;

        public IconicAdapter(ArrayList<SearchResultParcelable> searchResultParcelables) {
            /*
            Normally this 0 in the super constructor would be the id of the textView we are updating,
            but since we are using a custom Adapter, this is no longer appropriate. So we can just
            put an arbitrary value here.
             */
            super(getActivity(),0, searchResultParcelables);
            this.searchResultParcelables = searchResultParcelables;
        }

        public void swapItems(ArrayList<SearchResultParcelable> searchResultParcelables) {
            this.searchResultParcelables.clear();
            this.searchResultParcelables.addAll(searchResultParcelables);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            SearchResultParcelable result = getItem(position);


            if (convertView == null) {
                Log.d("convertView", "null");
                convertView = LayoutInflater.from(getContext()).inflate(
                        R.layout.search_result_list_item,
                        parent, false);
            }

            ImageView artistImage = (ImageView) convertView.findViewById(R.id.imageViewArtist);
            Picasso.with(getActivity()).load(result
                    .artistImageUrl).into(artistImage);

            TextView artistName = (TextView) convertView.findViewById(R.id.textViewArtistName);
            artistName.setText(result.artistName);

            return convertView;

        }

        public ArrayList<SearchResultParcelable> getSearchResultParcelables(){
            return searchResultParcelables;
        }
    }
}

