package udacity.mariosoberanis.spotifystreamer.activities;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import udacity.mariosoberanis.spotifystreamer.fragments.TrackPlayingFragment;
import udacity.mariosoberanis.spotifystreamer.PlayList;
import udacity.mariosoberanis.spotifystreamer.PlayListItem;
import udacity.mariosoberanis.spotifystreamer.R;
import udacity.mariosoberanis.spotifystreamer.db.SpotiContract;
import udacity.mariosoberanis.spotifystreamer.db.SpotiProvider;
import udacity.mariosoberanis.spotifystreamer.services.SpotiMediaService;

public class TrackPlayingActivity extends Activity
        implements TrackPlayingFragment.NowPlayingListener {

    public final String TAG = getClass().getCanonicalName();

    public static final String TRACK_PLAYING_FRAGMENT_ID = "Track_Playing_Fragment";

    // Key used to store the current position in the playlist
    public static final String CURRENT_PLAYLIST_POSITION = "current_playlist_position";

    private static final int SEEK_BAR_UPDATE_INTERVAL = 1000; // in milliseconds.

    // Ids of the most recent track and artist.
    private String mTrackSpotifyId;
    private String mArtistSpotifyId;

    // Holds the tracks associated with the current artists.
    PlayList mPlayList;

    /*
    * It may happen that the user clicks play or pause before we're connected to the
    * SpotiMediaService.  In this case, we'll use these to flag what should happen
    * when the service is connected.
    */
    private boolean mPlayOnServiceConnect = false;
    private boolean mPauseOnServiceConnect = false;

    private TrackPlayingFragment mTrackPlayingFragment;

    // The service we'll use to play the music.
    SpotiMediaService mSpotiService;
    boolean isSpotiServiceBound = false;

    // Handles updates to the progress bar.
    private Handler mHandler = new Handler();

    //Handles new song selected stoping the current one
    private boolean mResetOnStartup;


    //Handles our connection to the SpotiMediaService.

    private ServiceConnection mSpotiServiceConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder service) {

            if (mPlayList.size() == 0) return;

            SpotiMediaService.SpotiMediaServiceBinder binder =
                    (SpotiMediaService.SpotiMediaServiceBinder) service;

            mSpotiService = binder.getService();
            isSpotiServiceBound = true;

            // Let de Service a new song si coming
            mSpotiService.setContinueOnCompletion(true);

            // Begin playing the first track
            if (mResetOnStartup) {
                queueNextSong();
                onPlayClicked();

            } else {

                /*
                * In case Play or Pause was clicked before the service was established.
                */
                if (mPlayOnServiceConnect) {
                    mPlayOnServiceConnect = false;
                    onPlayClicked();

                } else if (mPauseOnServiceConnect) {
                    mPauseOnServiceConnect = false;
                    onPauseClicked();
                }

                // Helps get the play/pause button to the right state.
                setIsPlaying(mSpotiService.isPlaying());

            }

            /*
            * Create a process to update the seek bar location every second.
            * Also keeps the play/pause button status up to date.  The play/pause
            * status can be off if the user clicks it too quickly.  This will straighten
            * it out every second.
            */
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mSpotiService != null) {
                        mTrackPlayingFragment.setTrackDuration(mSpotiService.getDuration());
                        mTrackPlayingFragment.setSeekBarLocation(mSpotiService.getLocation());
                        mTrackPlayingFragment.setIsPlaying(mSpotiService.isPlaying());
                    }

                    if (isSpotiServiceBound) mHandler.postDelayed(this, SEEK_BAR_UPDATE_INTERVAL);
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isSpotiServiceBound = false;
        }
    };

    /*
    * When we receive notice that the track has finished playing, move on
    * to the next track.
    */
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction())  {
                case SpotiMediaService.TRACK_STOP_BROADCAST_FILTER:  {
                    onNextClicked();
                    break;
                }
                default:
                    throw new IllegalArgumentException("Unexpected broadcast message received: " +
                            intent.getAction());
            }
        }
    };

    @Override
    public void onSaveInstanceState(Bundle outStatus) {
        super.onSaveInstanceState(outStatus);
        outStatus.putInt(CURRENT_PLAYLIST_POSITION, mPlayList.getPosition());
        outStatus.putBoolean(SpotiStreamerActivity.KEY_IS_PLAYING, mSpotiService.isPlaying());
    }

    /*
    * Handles intents coming in from notifications.
    */
    @Override
    public void onNewIntent(Intent i) {
        super.onNewIntent(i);

        switch (i.getAction()) {
            case SpotiStreamerActivity.ACTION_PREVIOUS:
                onPrevClicked();
                break;
            case SpotiStreamerActivity.ACTION_NEXT:
                onNextClicked();
                break;
            case SpotiStreamerActivity.ACTION_PAUSE:
                onPauseClicked();
                break;
            case SpotiStreamerActivity.ACTION_PLAY:
                onPlayClicked();
                break;
            default: // Do Nothing
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.track_playing);

        int savedPlayListPosition = -1;

        if (savedInstanceState == null) {
            getFragmentManager()
                    .beginTransaction()
                    .replace(R.id.track_playing_container,
                            new TrackPlayingFragment(), TRACK_PLAYING_FRAGMENT_ID)
                    .commit();

            /*
            * savedInstanceState is null, so this is our first time starting up.  We'll want
            * to reset and begin playing the first track.
            */
            mResetOnStartup = true;

        } else {

            /*
            * Get the playing / paused state from the bundle until the StreamerService
            * is connected.  At that point, we can use the service to determine if we're
            * currently playing.
            */
            savedPlayListPosition =
                    savedInstanceState.getInt(CURRENT_PLAYLIST_POSITION);

            /*
            * Not our first time starting up, so just continue playing the current track.
            */
            mResetOnStartup = false;
        }

        Intent callingIntent = getIntent();

        String action = callingIntent.getStringExtra(SpotiStreamerActivity.ACTION);

        if (SpotiStreamerActivity.ACTION_PREVIOUS.equals(action)) {
            onPrevClicked();
            return;
        }

        if (SpotiStreamerActivity.ACTION_PAUSE.equals(action)) {
            onPauseClicked();
            return;
        }

        if (SpotiStreamerActivity.ACTION_NEXT.equals(action)) {
            onNextClicked();
            return;
        }

        mTrackSpotifyId = callingIntent.getStringExtra(
                SpotiStreamerActivity.KEY_TRACK_SPOTIFY_ID);

        mArtistSpotifyId = callingIntent.getStringExtra(
                SpotiStreamerActivity.KEY_ARTIST_SPOTIFY_ID);


        // Are we returning from a notification?  Notification should set to false;
        mResetOnStartup &=
                callingIntent.getBooleanExtra(SpotiStreamerActivity.KEY_RESET_ON_STARTUP, true);


        // Register to receive track completed broadcast notifications
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mBroadcastReceiver,
                new IntentFilter(SpotiMediaService.TRACK_STOP_BROADCAST_FILTER));

        /*
        * Uses mTrackSpotifyId and mArtistSpotifyId to load track data into the PlayList.
        */
        loadTrackData();

        if (savedPlayListPosition >= 0)  {
            mPlayList.setPosition(savedPlayListPosition);
        }

        // Start the SpotiMedia service.
        Intent startMediaServiceIntent = new Intent(this, SpotiMediaService.class);
        startService(startMediaServiceIntent);
        bindService(startMediaServiceIntent, mSpotiServiceConnection,
                Context.BIND_AUTO_CREATE);

    }

    private boolean isPlaying() {
        return (mSpotiService == null) ? false : mSpotiService.isPlaying();
    }

    /*
    * Loads data from the content provider into a PlayList object.  Sets the play list position
    * to match the track given in mTrackSpotifyId.
    */
    private void loadTrackData() {

        /*
        * Load list of tracks for the given artist.
        * Data is pulled from the Content Provider, and stored in String[]s.
        */
        Cursor trackListCursor = null;

        try {
            trackListCursor = getApplicationContext().getContentResolver().query(
                    SpotiContract.GET_TRACKS_CONTENT_URI.buildUpon()
                            .appendEncodedPath(mArtistSpotifyId).build(),
                    null,
                    null,
                    null,
                    null);

            trackListCursor.moveToFirst();

            mPlayList = new PlayList(trackListCursor.getCount());

            int i = 0;
            while (!trackListCursor.isAfterLast()) {

                PlayListItem item = new PlayListItem();

                item.setArtistName(trackListCursor.getString(SpotiProvider.TRACKS_BY_ARTIST_ARTIST_NAME));
                item.setArtistId(trackListCursor.getString(SpotiProvider.TRACKS_BY_ARTIST_ARTIST_SPOTIFY_ID));
                item.setTrackUri(trackListCursor.getString(SpotiProvider.TRACKS_BY_ARTIST_PREVIEW_URL));
                item.setTrackId(trackListCursor.getString(SpotiProvider.TRACKS_BY_ARTIST_TRACK_SPOTIFY_ID));
                item.setTrackName(trackListCursor.getString(SpotiProvider.TRACKS_BY_ARTIST_TRACK_NAME));
                item.setAlbumName(trackListCursor.getString(SpotiProvider.TRACKS_BY_ARTIST_ALBUM_NAME));
                item.setTrackImage(trackListCursor.getString(SpotiProvider.TRACKS_BY_ARTIST_TRACK_IMAGE));
                item.setDuration(trackListCursor.getInt(SpotiProvider.TRACKS_BY_ARTIST_DURATION));
                item.setExplicit(trackListCursor.getInt(SpotiProvider.TRACKS_BY_ARTIST_EXPLICIT) == 1);

                if (mTrackSpotifyId.equals(item.getTrackId())) {
                    mPlayList.setPosition(i);
                }

                mPlayList.setItemAt(i++, item);
                trackListCursor.moveToNext();
            }

        } catch (Exception e) {

            /*
            * Set mNumberOfTracks to 0 to indicate an error.
            * This is probably the result of a orphaned notification.  That is, the app closed, the
            * SpotiMediaSevice exited, but the last song that was playing never finished,
            * therefore the notification never got cancelled.
            */
            mPlayList = new PlayList(0);

            Toast.makeText(getApplicationContext(), R.string.error_restoring_state, Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(this, SearchActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();

        } finally {

            if (trackListCursor != null) {
                trackListCursor.close();
            }
        }
    }

    /*
    * When the
    * TrackPlayingFragment completes loading, it will call this method.  At that time
    * we'll push the artist and track data down.
    */
    public void requestContentRefresh() {
        refreshContent();
    }

    /*
    * Update TrackPlayingFragment with the current artist and track info.
    */
    private void refreshContent() {

        /*
        * If there was a error loading track data, don't try to push anything down
        * to mNowPlayingFragment.
        */
        if (mPlayList.size() == 0) return;

        if (mTrackPlayingFragment == null) {
            mTrackPlayingFragment = (TrackPlayingFragment) getFragmentManager()
                    .findFragmentByTag(TRACK_PLAYING_FRAGMENT_ID);
        }

        PlayListItem item = mPlayList.getCurrentItem();

        mTrackPlayingFragment.setArtistName(item.getArtistName());
        mTrackPlayingFragment.setTrackName(item.getTrackName());
        mTrackPlayingFragment.setAlbumName(item.getAlbumName());
        mTrackPlayingFragment.setTrackImage(item.getTrackImage());
        mTrackPlayingFragment.setIsPlaying(isPlaying());
    }

    /*
    * Sets the status of the Play/Pause button.  This is temporary until the connection
    * to the SpotiMediaService is re-established.
    */
    private void setIsPlaying(boolean isPlaying) {
        mTrackPlayingFragment.setIsPlaying(isPlaying);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy() called");

        try {
            if (isSpotiServiceBound) {

                /*
                * Notify the service that there are no more tracks coming.  This allows
                * it to cancel notifications (rather than leaving them around to be
                * replaced).
                */
                mSpotiService.setContinueOnCompletion(false);
                unbindService(mSpotiServiceConnection);
            }
        } catch (Exception e) {
            // Ignore exception.
        }

        // Stop receiving track finished broadcasts.
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);

        super.onDestroy();
    }

    @Override
    public void onStop() {
        Log.d(TAG, "onStop() called");
        super.onStop();
    }

    /*
    * Prepares the MediaService to play the next song.
    */
    private void queueNextSong() {
        if (isSpotiServiceBound && (mPlayList.size() > 0)) {
            if (!mSpotiService.reset(mPlayList.getCurrentItem())) {
                Toast.makeText(this, R.string.media_error_playing, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onPlayClicked() {

        if (mPlayList.size() == 0) return;

        if (isSpotiServiceBound) {

            if (mSpotiService.play()) {

            } else {
                Toast.makeText(this, R.string.media_error_general, Toast.LENGTH_SHORT).show();
            }

        } else {

            /*
            * Play was clicked,
            */
            mPlayOnServiceConnect = true;

            Intent startMediaServiceIntent = new Intent(this, SpotiMediaService.class);
            startService(startMediaServiceIntent);
            bindService(startMediaServiceIntent, mSpotiServiceConnection,
                    Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    public void onPauseClicked() {

        if (mPlayList.size() == 0) return;

        if (isSpotiServiceBound) {

            if (!mSpotiService.pause()) {
                Toast.makeText(this, R.string.media_error_general, Toast.LENGTH_SHORT).show();
            }

        } else {

            /*
            * Pause was clicked,
            */
            mPauseOnServiceConnect = true;

            Intent startMediaServiceIntent = new Intent(this, SpotiMediaService.class);
            startService(startMediaServiceIntent);
            bindService(startMediaServiceIntent, mSpotiServiceConnection,
                    Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    public void onNextClicked() {

        if (mPlayList.size() == 0) return;

        mPlayList.nextTrack();


        refreshContent();

        // Start the track.
        queueNextSong();
        onPlayClicked();
    }

    @Override
    public void onPrevClicked() {

        if (mPlayList.size() == 0) return;

        mPlayList.previousTrack();


        refreshContent();

        // Play the track.
        queueNextSong();
        onPlayClicked();
    }

    @Override
    public void seekTo(int miliSeconds) {

        if (mPlayList.size() == 0) return;

        if (isSpotiServiceBound) {
            if (!mSpotiService.seekTo(miliSeconds)) {
                Toast.makeText(this, R.string.media_error_general, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
