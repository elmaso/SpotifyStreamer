package com.example.mariosoberanis.spotifystreamer;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;

import java.util.ArrayList;

/**
 * Created by mariosoberanis on 7/4/15.
 */
public class ArtistAdapter extends RecyclerView.Adapter<ArtistAdapter.ViewHolder> {

    private ArrayList<Artist> artists;
    private ImageLoader imageLoader;

    public ArtistAdapter(ArrayList<Artist> artists) {
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

        holder.nameView.setText(artist.getName());

        if (artist.getImageSrc() != null && imageLoader != null) {
            holder.imageView.setImageUrl(artist.getImageSrc(), imageLoader);
        }
    }

    @Override
    public int getItemCount() {
        if (artists == null) return 0;

        return artists.size();
    }

    public void setImageLoader(ImageLoader imageLoader) {
        this.imageLoader = imageLoader;
    }

    public void setArtists(ArrayList<Artist> artists) {
        this.artists = artists;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public TextView nameView;
        public NetworkImageView imageView;

        public ViewHolder(View view) {
            super(view);

            nameView = (TextView) view.findViewById(R.id.artist_search_list_item_name);
            imageView = (NetworkImageView) view.findViewById(R.id.artist_search_list_item_image);
        }
    }


}
