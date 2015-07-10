package com.example.mariosoberanis.spotifystreamer;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;

import java.util.List;

import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Track;

/**
 * Created by mariosoberanis on 7/7/15.
 */
public class TracksAdapter extends RecyclerView.Adapter<TracksAdapter.ViewHolder> {

    private List<Track> tracks;
    private ImageLoader imageLoader;

    public TracksAdapter(List<Track> tracks) {
        this.tracks = tracks;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.search_result_track_item, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.setTrack(tracks.get(position));
    }

    @Override
    public int getItemCount() {
        if (tracks == null) return 0;

        return tracks.size();
    }

    public void setImageLoader(ImageLoader imageLoader) {
        this.imageLoader = imageLoader;
    }

    public void setTracks(List<Track> tracks) {
        this.tracks = tracks;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private TextView nameView;
        private TextView albumNameView;
        private NetworkImageView imageView;

        public ViewHolder(View view) {
            super(view);

            nameView = (TextView) view.findViewById(R.id.track_list_item_song_title);
            albumNameView = (TextView) view.findViewById(R.id.track_list_item_album_name);
            imageView = (NetworkImageView) view.findViewById(R.id.imageViewAlbum);
        }

        public void setTrack(Track track) {
            nameView.setText(track.name);
            albumNameView.setText(track.album.name);

            if (!track.album.images.isEmpty()) {
                Image image = track.album.images.get(1);
                imageView.setImageUrl(image.url, imageLoader);
            }
        }

    }

}
