package com.example.mariosoberanis.spotifystreamer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by mariosoberanis on 7/1/15.
 */
public class Artist {
    private String name;
    private String imageSrc;

    public static Artist fromJSONObject(JSONObject object) throws JSONException {
        Artist artist = new Artist();

        artist.setName(object.getString("name"));

        JSONArray images = object.getJSONArray("images");
        if (images.length() != 0) {
            JSONObject imageObj = images.getJSONObject(0);
            artist.setImageSrc(imageObj.getString("url"));
        }

        return artist;
    }

    public Artist() { }

    public Artist(String name, String imageSrc) {
        this.name = name;
        this.imageSrc = imageSrc;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setImageSrc(String imageSrc) {
        this.imageSrc = imageSrc;
    }

    public String getImageSrc() {
        return imageSrc;
    }
}
