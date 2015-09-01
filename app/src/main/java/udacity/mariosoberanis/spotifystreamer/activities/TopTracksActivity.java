package udacity.mariosoberanis.spotifystreamer.activities;

import android.os.Bundle;
import android.view.Menu;

import udacity.mariosoberanis.spotifystreamer.R;
import udacity.mariosoberanis.spotifystreamer.fragments.TopTrackListFragment;

/*
 * This activity is called when a user selects an artist.
 * It users the TopTrackListFragment to display a list of
 * tracks associated with the selected artist.
 */
public class TopTracksActivity extends SpotiStreamerActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.track_list_activity);

        if (savedInstanceState == null) {
            // Create the track list fragment and add it to the activity
            // using a fragment transaction.

            Bundle arguments = new Bundle();
            arguments.putParcelable(TopTrackListFragment.BUNDLE_KEY_ARTIST_ID, getIntent().getData());

            TopTrackListFragment fragment = new TopTrackListFragment();
            fragment.setArguments(arguments);

            getFragmentManager().beginTransaction()
                    .replace(R.id.track_list_container, fragment, TRACK_LIST_FRAGMENT_ID)
                    .commit();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.track_list, menu);
        return super.onCreateOptionsMenu(menu);
    }

}