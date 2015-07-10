package com.example.mariosoberanis.spotifystreamer;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by mariosoberanis on 7/9/15.
 */

//Parcel pattern taken from here:
// http://stackoverflow.com/questions/12503836/how-to-save-custom-arraylist-on-android-screen-rotate
public class TrackParcelable implements Parcelable {
    public String name;
    public String albumName;
    public String albumImageUrl;
    public String trackPreviewUrl;

    public TrackParcelable(String name, String albumName, String albumImageUrl, String trackPreviewUrl) {
        this.name = name;
        this.albumName = albumName;
        this.albumImageUrl = albumImageUrl;
        this.trackPreviewUrl = trackPreviewUrl;
    }

    private TrackParcelable(Parcel in) {
        name = in.readString();
        albumName = in.readString();
        albumImageUrl = in.readString();
        trackPreviewUrl = in.readString();
    }

    @Override
    public String toString() {
        return "Track Info. Name: " + name + ", Album Name: " + albumName;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(albumName);
        dest.writeString(albumImageUrl);
        dest.writeString(trackPreviewUrl);

    }
    public static final Parcelable.Creator<TrackParcelable> CREATOR = new Parcelable.Creator<TrackParcelable>() {
        public TrackParcelable createFromParcel(Parcel in) {
            return new TrackParcelable(in);
        }

        public TrackParcelable[] newArray(int size) {
            return new TrackParcelable[size];
        }
    };

}
