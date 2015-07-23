package com.example.android.musicstreamer;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.ArtistsPager;

/**
 * Created by kenm on 6/18/2015.
 */
public class ArtistAdapter extends ArrayAdapter{
    public static final String ARTIST_LIST = "state_artist";
    private static final String TAG = ArtistAdapter.class.getSimpleName();

    private final int TARGET_WIDTH = 200;
    private final int TARGET_HEIGHT = 200;

    ArrayList<Artist> list = new ArrayList<>();
    private Context mContext;

    public ArtistAdapter(Context context, int resource) {
        super(context, resource);
        // TODO Auto-generated constructor stub
        mContext = context;
    }

    public void add(Artist object) {
        // TODO Auto-generated method stub
        list.add(object);
        super.add(object);
    }

    public void save(Bundle outState) {
        outState.putParcelableArrayList(ARTIST_LIST, list);
    }

    public void restore(Bundle inState) {
        list = inState.getParcelableArrayList(ARTIST_LIST);
    }

    public void clear(){
        list.clear();
        super.clear();
    }

    static class RowHolder {
        ImageView IMG;
        TextView NAME;
    }

    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return this.list.size();
    }

    @Override
    public Object getItem(int position) {
        // TODO Auto-generated method stub
        return this.list.get(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // TODO Auto-generated method stub
        View row;
        row = convertView;
        RowHolder holder;

        if (convertView == null)
        {
            LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            row = inflater.inflate(R.layout.artist_list_item, parent, false);

            holder = new RowHolder();
            holder.IMG = (ImageView) row.findViewById(R.id.list_item_artist_icon);
            holder.NAME = (TextView) row.findViewById(R.id.list_item_artist_name_textview);

            row.setTag(holder);
        }
        else
        {
            holder = (RowHolder) row.getTag();
        }

        Artist artist = (Artist) getItem(position);
        holder.NAME.setText(artist.getName());

        if (artist.getImageUrl() == null) {
            holder.IMG.setImageResource(R.drawable.no_image_avail);
        }
        else {
            Picasso.with(mContext.getApplicationContext())
                    .load(artist.getImageUrl())
                    .resize(TARGET_WIDTH, TARGET_HEIGHT)
                    .centerCrop()
                    .into(holder.IMG);
        }

        return row;
    }

    public void fetchNewData(String artist) {
        SpotifyAritstDataTask spotifyArtistTask = new SpotifyAritstDataTask();
        spotifyArtistTask.execute(artist);
    }

    public class SpotifyAritstDataTask extends AsyncTask<String, Void, ArtistsPager> {
        private Exception savedException = null;

        @Override
        protected ArtistsPager doInBackground(String... params) {
            if (params.length == 0) {
                return null;
            }

            ArtistsPager artistResults = null;
            savedException = null;
            try {
                SpotifyApi api = new SpotifyApi();
                SpotifyService spotify = api.getService();
                artistResults = spotify.searchArtists(params[0]);
            } catch (Exception e) {
                Log.d(TAG, "SpotifyAritstDataTask()", e);
                savedException = e;
            }

            return artistResults;
        }

        @Override
        protected void onPostExecute(ArtistsPager artistResults) {
            super.onPostExecute(artistResults);

            if ((artistResults != null) && (artistResults.artists.items.size() > 0)) {
                clear();

                for(int i = 0; i < artistResults.artists.items.size(); i++) {
                    String url = null;
                    if (artistResults.artists.items.get(i).images.size() != 0) {
                        url = artistResults.artists.items.get(i).images.get(0).url;
                    }

                    Artist artist = new Artist(artistResults.artists.items.get(i).name,
                            artistResults.artists.items.get(i).id,
                            url);

                    add(artist);
                }
            }
            else {
                String temp;
                if (savedException != null) {
                    temp = mContext.getString(R.string.problem_detected) +
                            savedException.getCause() +
                            "\n" +
                            savedException.getMessage();
                }
                else {
                    temp = mContext.getString(R.string.artist_not_found);
                }

                Toast.makeText(mContext.getApplicationContext(), temp, Toast.LENGTH_LONG).show();

                clear();
            }
        }
    }
}
