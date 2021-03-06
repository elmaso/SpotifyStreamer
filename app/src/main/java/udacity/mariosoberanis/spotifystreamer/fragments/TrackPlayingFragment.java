package udacity.mariosoberanis.spotifystreamer.fragments;

import android.app.DialogFragment;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import udacity.mariosoberanis.spotifystreamer.R;

/*
* Handles displaying the data related to the currently playing track.  Most of the logic
* for handling track changes, etc. is contained in TrackPlayingActivity.
*/
public class TrackPlayingFragment extends DialogFragment {

    public interface NowPlayingListener {

        void requestContentRefresh();

        void onPlayClicked();

        void onPauseClicked();

        void onNextClicked();

        void onPrevClicked();

        void seekTo(int miliSeconds);
    }

    // private final String TAG = getClass().getCanonicalName();

    private NowPlayingListener mListener;

    private TextView mTextViewArtist;
    private TextView mTextViewTrackName;
    private TextView mTextViewAlbumName;
    private ImageView mImageViewTrackImage;
    private TextView mSeekBarDurationLabel;
    private TextView mSeekBarLocationLabel;

    private SeekBar mSeekBar;

    private int mImageWidth;
    private int mImageHeight;

    ImageButton mPrevButton;
    ImageButton mPlayButton;
    ImageButton mPauseButton;
    ImageButton mNextButton;

    // Used to ignore updates when the user is trying to move the seek bar.
    private boolean suspendProgressUpdates = false;

    /*
    * The following setters should not be called until TrackPlayingFragment calls
    * requestContentRefresh().  This notifies the listener that the widgets have
    * been inflated.
    */
    public void setArtistName(String artistName) {
        if (mTextViewArtist != null)  mTextViewArtist.setText(artistName);
    }

    public void setTrackName(String trackName) {
        if (mTextViewTrackName != null) mTextViewTrackName.setText(trackName);
    }

    public void setAlbumName(String albumName) {
        if (mTextViewAlbumName != null)  mTextViewAlbumName.setText(albumName);
    }

    public void setTrackImage(String trackImage) {

        Resources resources = getActivity().getApplicationContext().getResources();

        if (mImageViewTrackImage != null)  {
            Picasso.with(getActivity().getApplicationContext())
                    .load(trackImage)
                    .placeholder(resources.getDrawable(R.drawable.image_loading, null))
                    .error(resources.getDrawable(R.drawable.image_not_available, null))
                    .resize(mImageWidth, mImageHeight)
                    .into(mImageViewTrackImage)
            ;
        }
    }

    /*
    * Takes a number of milliSeconds and formats it as M:S.
    * Seconds will always be 2 digits.
    */
    private static String formatDurationLabel(int milliSeconds)  {

        int seconds = milliSeconds / 1000;

        int mins = seconds / 60;
        int secs = seconds % 60;

        String mStr = "" + mins;
        String sStr = "" + secs;

        if (sStr.length() < 2) sStr = "0" + sStr;

        return mStr + ":" + sStr;
    }

    public void setTrackDuration(int duration)  {
        if (mSeekBar != null)  mSeekBar.setMax(duration);
        if (mSeekBarDurationLabel != null) mSeekBarDurationLabel.setText(formatDurationLabel(duration));
    }

    public void setSeekBarLocation(int location)  {

        /*
        * Do not update the seekbar programatically if suspendProgressUpdates is true.  If the
        * user is trying to move the seek bar, it's kinda frustrating if the app takes over...
        */
        if ((mSeekBar != null) && (!suspendProgressUpdates)) mSeekBar.setProgress(location);
        if (mSeekBarLocationLabel != null) mSeekBarLocationLabel.setText(formatDurationLabel(location));

    }

    public void setIsPlaying(boolean isPlaying)  {

        // Toggles the Play/Pause button
        if ((mPlayButton != null) && (mPauseButton != null)) {
            if (isPlaying) {
                mPlayButton.setVisibility(View.GONE);
                mPauseButton.setVisibility(View.VISIBLE);

            } else {
                mPlayButton.setVisibility(View.VISIBLE);
                mPauseButton.setVisibility(View.GONE);
            }
        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mImageWidth = (int) getActivity().getResources().getDimension(R.dimen.track_image_display_width);
        mImageHeight = (int) getActivity().getResources().getDimension(R.dimen.track_image_display_height);

        try {
            mListener = (NowPlayingListener) getActivity();

        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString() + " must implement" +
                    "NowPlayingFragment.NowPlayingListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.track_playing_dialog, container, false);

        mTextViewArtist = (TextView) rootView.findViewById(R.id.artist);
        mTextViewTrackName = (TextView) rootView.findViewById(R.id.track_name);
        mTextViewAlbumName = (TextView) rootView.findViewById(R.id.album_name);
        mImageViewTrackImage = (ImageView) rootView.findViewById(R.id.track_image);

        mSeekBarDurationLabel = (TextView) rootView.findViewById(R.id.seek_bar_end_label);
        mSeekBarLocationLabel = (TextView) rootView.findViewById(R.id.seek_bar_start_label);

        mSeekBar = (SeekBar) rootView.findViewById(R.id.seek_bar);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Do Nothing
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Do not allow automatic progress updates while the user is using the SeekBar
                suspendProgressUpdates = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                // Seek to the specified position, and resume automatic updates.
                mListener.seekTo(seekBar.getProgress());
                suspendProgressUpdates = false;
            }
        });

        mPrevButton = (ImageButton) rootView.findViewById(R.id.button_prev);
        mPlayButton = (ImageButton) rootView.findViewById(R.id.button_play);
        mPauseButton = (ImageButton) rootView.findViewById(R.id.button_pause);
        mNextButton = (ImageButton) rootView.findViewById(R.id.button_next);

        mPrevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onPrevClicked();
            }
        });

        mPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onPlayClicked();
            }
        });

        mPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onPauseClicked();
            }
        });

        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onNextClicked();
            }
        });

        /*
        * Notify the listener (calling Activity) that we're ready to begin receiving data.  The
        * view has been inflated, so it's ready to get the current values.
        */
        mListener.requestContentRefresh();

        return rootView;
    }
}
