package com.example.mariosoberanis.spotifystreamer;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by mariosoberanis on 7/9/15.
 */
public class StreamerActivityFragment extends Fragment {

    public StreamerActivityFragment(){

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_streamer, container, false);
    }
}
