package com.example.mariosoberanis.spotifystreamer;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by mariosoberanis on 7/9/15.
 */

//Parcel pattern taken from here:
// http://stackoverflow.com/questions/12503836/how-to-save-custom-arraylist-on-android-screen-rotate
public class SearchResultParcelable implements Parcelable{
    public String artistName;
    public String artistImageUrl;
    public String artistId;

    public SearchResultParcelable(String artistName, String artistImageUrl, String artistId) {
        this.artistName = artistName;
        this.artistImageUrl = artistImageUrl;
        this.artistId = artistId;
    }

    private SearchResultParcelable(Parcel in) {
        artistName = in.readString();
        artistImageUrl = in.readString();
        artistId = in.readString();
    }

    @Override
    public String toString() {
        return "Search Result Info. Artist Name: " + artistName;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(artistName);
        dest.writeString(artistImageUrl);
        dest.writeString(artistId);
    }


    public static final Parcelable.Creator<SearchResultParcelable> CREATOR =
            new Parcelable.Creator<SearchResultParcelable>() {
                public SearchResultParcelable createFromParcel(Parcel in) {
                    return new SearchResultParcelable(in);
                }
                public SearchResultParcelable[] newArray(int size) {
                    return new SearchResultParcelable[size];
                }
            };



}
