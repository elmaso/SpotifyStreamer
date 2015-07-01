package com.example.mariosoberanis.spotifystreamer;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;


/**
 * A placeholder fragment containing a simple view.
 */
public class ArtistSearchFragment extends Fragment {



    ArrayAdapter<String> resultsAdapter;

    public ArtistSearchFragment() {

        }


        @Override
        public View onCreateView (LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState){

            View view = inflater.inflate(R.layout.artistfragment_main, container, false);

            resultsAdapter = new ArrayAdapter<>(getActivity(), R.layout.search_result_list_item,
                    R.id.artist_search_list_item_name, getDummyArtists());

            ListView listView = (ListView) view.findViewById(R.id.artist_search_results_list);
            listView.setAdapter(resultsAdapter);

            return view;

        }
        private String[] getDummyArtists (){
            return new String[]{
                    "Muse",
                    "Train",
                    "OMD",
                    "Mettalica",
                    "Iggy Azalea",
                    "Cricri",
            };
        }
    }


