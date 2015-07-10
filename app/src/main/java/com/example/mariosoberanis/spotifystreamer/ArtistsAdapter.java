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

import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.Image;

/**
 * Created by mariosoberanis on 7/4/15. imporamos
 */
public class ArtistsAdapter extends RecyclerView.Adapter<ArtistsAdapter.ViewHolder> {

    private List<Artist> artists;
    private ImageLoader imageLoader;

    private ClickListener clickListener;

    public ArtistsAdapter(List<Artist> artists) {
        this.artists = artists;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(
                R.layout.search_result_list_item, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Artist artist = artists.get(position);

        holder.setArtist(artist);
    }

    @Override
    public int getItemCount() {
        if (artists == null) return 0;

        return artists.size();
    }
    public void setClickListener(ClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void setImageLoader(ImageLoader imageLoader) {
        this.imageLoader = imageLoader;
    }

    public void setArtists(List<Artist> artists) {
        this.artists = artists;
    }

    public interface ClickListener {

        void onArtistItemClicked(View view, Artist artist);

    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private TextView nameView;
        private NetworkImageView imageView;

        private Artist artist;

        public ViewHolder(View view) {
            super(view);
            view.setOnClickListener(this);

            nameView = (TextView) view.findViewById(R.id.textViewArtistName);
            imageView = (NetworkImageView) view.findViewById(R.id.imageViewArtist);
        }
        @Override
        public void onClick(View view) {
            if (clickListener == null) return;

            clickListener.onArtistItemClicked(view, artist);
        }

        public void setArtist(Artist artist) {
            this.artist = artist;

            nameView.setText(artist.name);

            if (!artist.images.isEmpty() && imageLoader != null) {
                Image image = artist.images.get(1);
                imageView.setImageUrl(image.url, imageLoader);
            }
        }

    }


}
