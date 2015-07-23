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
import java.util.HashMap;
import java.util.Map;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Tracks;

/**
 * Created by kenm on 6/19/2015.
 */
public class TopTenTracksAdapter extends ArrayAdapter {
    public static final int TARGET_WIDTH = 200;
    public static final int TARGET_HEIGHT = 200;
    private static final int MAX_TRACKS = 10;
    public static final String TOP_TEN_LIST = "state_top_ten";
    private static final String TAG = TopTenTracksAdapter.class.getSimpleName();
    private String artist;
    private String mCountryCode;

    public Bundle[] mBundles = null;
    public final String TRACK_BUNDLE_POSITION = "POSITION";
    public final String TRACK_BUNDLE_ID       = "ID";
    public final String TRACK_BUNDLE_ARTIST   = "ARTIST";
    public final String TRACK_BUNDLE_ALBUM    = "ALBUM";
    public final String TRACK_BUNDLE_TRACK    = "TRACK";
    public final String TRACK_BUNDLE_PREVIEW  = "PREVIEW";
    public final String TRACK_BUNDLE_IMAGE    = "IMAGE";

    ArrayList<TopTenTracks> list = new ArrayList<>();
    private Context mContext;
    private TopTenAdapterListener listener;

    public interface TopTenAdapterListener {
        public void onTopTenItemsReady(Bundle[] bundles);
    }

    public TopTenTracksAdapter(TopTenAdapterListener ttaListner, Context context, int resource, String countryCode) {
        super(context, resource);
        // TODO Auto-generated constructor stub
        mContext = context;
        mCountryCode = countryCode;
        listener = ttaListner;
    }

    public void add(TopTenTracks object) {
        // TODO Auto-generated method stub
        list.add(object);
        super.add(object);
    }

    public void setCountryCode(String mCountryCode) {
        this.mCountryCode = mCountryCode;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public void save(Bundle outState) {
        outState.putParcelableArrayList(TOP_TEN_LIST, list);
    }

    public void restore(Bundle inState) {
        list = inState.getParcelableArrayList(TOP_TEN_LIST);
    }

    public void clear(){
        list.clear();
        super.clear();
    }

    static class RowHolder {
        ImageView IMG;
        TextView ALBUM;
        TextView TRACK;
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
            row = inflater.inflate(R.layout.top_ten_item, parent, false);

            holder = new RowHolder();
            holder.IMG = (ImageView) row.findViewById(R.id.top_ten_artist_image);
            holder.ALBUM = (TextView) row.findViewById(R.id.top_ten_album_textview);
            holder.TRACK = (TextView) row.findViewById(R.id.top_ten_track_textview);

            row.setTag(holder);
        }
        else
        {
            holder = (RowHolder) row.getTag();
        }

        TopTenTracks topTen = (TopTenTracks) getItem(position);
        holder.ALBUM.setText(topTen.getAlbum());
        holder.TRACK.setText(topTen.getTrack());

        if (topTen.getImageResource() == null) {
            holder.IMG.setImageResource(R.drawable.no_image_avail);
        }
        else {
            Picasso.with(mContext.getApplicationContext())
                    .load(topTen.getImageResource())
                    .resize(TARGET_WIDTH, TARGET_HEIGHT)
                    .centerCrop()
                    .into(holder.IMG);
        }

        return row;
    }

    public void fetchNewData(String artistID) {
        SpotifyTopTracksDataTask spotifyTopTracksTask = new SpotifyTopTracksDataTask();
        spotifyTopTracksTask.execute(artistID);
    }

    public class SpotifyTopTracksDataTask extends AsyncTask<String, Void, Tracks> {
        private Exception savedException = null;

        @Override
        protected Tracks doInBackground(String... params) {
            if (params.length == 0) {
                return null;
            }

            Tracks tracks = null;
            savedException = null;
            try {
                SpotifyApi api = new SpotifyApi();
                SpotifyService spotify = api.getService();

                Map<String, Object> options = new HashMap<>();
                options.put("country", mCountryCode);
                tracks = spotify.getArtistTopTrack(params[0], options);
            } catch (Exception e) {
                Log.d(TAG, "SpotifyTopTracksDataTask()", e);
                savedException = e;
            }

            return tracks;
        }

        @Override
        protected void onPostExecute(Tracks topTracks) {
            super.onPostExecute(topTracks);

            if ((topTracks != null) && (topTracks.tracks.size() > 0)) {
                clear();

                int max = topTracks.tracks.size() > MAX_TRACKS ? MAX_TRACKS:topTracks.tracks.size();
                mBundles = new Bundle[max];
                for(int i = 0; i < max; i++) {
                    Bundle bundle = new Bundle();

                    String id = topTracks.tracks.get(i).id;
                    String album = topTracks.tracks.get(i).album.name;
                    String track = topTracks.tracks.get(i).name;
                    String previewUrl = topTracks.tracks.get(i).preview_url;

                    String url = null;
                    if (topTracks.tracks.get(i).album.images.size() != 0) {
                        url = topTracks.tracks.get(i).album.images.get(0).url;
                    }

                    TopTenTracks topTenTracks = new TopTenTracks(id,
                            artist,
                            album,
                            track,
                            url,
                            previewUrl);

                    add(topTenTracks);

                    bundle.putInt(TRACK_BUNDLE_POSITION, i);
                    bundle.putString(TRACK_BUNDLE_ID, id);
                    bundle.putString(TRACK_BUNDLE_ARTIST, artist);
                    bundle.putString(TRACK_BUNDLE_ALBUM, album);
                    bundle.putString(TRACK_BUNDLE_TRACK, track);
                    bundle.putString(TRACK_BUNDLE_PREVIEW, previewUrl);
                    bundle.putString(TRACK_BUNDLE_IMAGE, url);
                    mBundles[i] = bundle;
                }

                // need to let the fragment know mBundles is ready...
                listener.onTopTenItemsReady(mBundles);
            }
            else {
                String temp;
                if (savedException != null) {
                    temp = mContext.getString(R.string.problem_detected) +
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
