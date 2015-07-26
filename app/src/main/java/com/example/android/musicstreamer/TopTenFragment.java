package com.example.android.musicstreamer;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

/**
 * Created by kenm on 7/8/2015.
 */
public class TopTenFragment extends Fragment implements TopTenTracksAdapter.TopTenAdapterListener{
    private final String LOG_TAG = "TopTenFragment";
    private View rootView;

    private ListView            mTopTenListView;
    private TopTenTracksAdapter mTopTenAdapter;
    private String              artistName;
    private OnItemSelectedListener listener;
    private int                 mCurrentPosition = 0;
    private String              mCountryCode;

    // data passed in from the main activity
    private final String ARTIST_POSITION      = "POSITION";
    private final String ARTIST_ID            = "ID";
    private final String ARTIST_ARTIST_NAME   = "ARTIST";
    private final String COUNTRY_CODE         = "COUNTRY_CODE";

    // used for bundling tracks
    public final String TRACK_BUNDLE_POSITION = "POSITION";
    public final String TRACK_BUNDLE_ID       = "ID";
    public final String TRACK_BUNDLE_ARTIST   = "ARTIST";
    public final String TRACK_BUNDLE_ALBUM    = "ALBUM";
    public final String TRACK_BUNDLE_TRACK    = "TRACK";
    public final String TRACK_BUNDLE_PREVIEW  = "PREVIEW";
    public final String TRACK_BUNDLE_IMAGE    = "IMAGE";

    public static Handler topTenMsgHandler;
    public static final int MSG_TOP_TEN_INCREMENT = 0x0010;
    public static final int MSG_TOP_TEN_DECREMENT = 0x0011;

    public TopTenFragment() {
    }

    public interface OnItemSelectedListener {
        public void onTopTenItemSelected(Bundle bundle);
        public void onTopTenItemReceived(Bundle[] bundles);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.topten_fragment, container, false);
        Log.v(LOG_TAG, "onCreateView");

        UpdateView();

        // Set description based on argument passed in or saved data...
        UpdateVars(getArguments(), savedInstanceState);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.v(LOG_TAG, "onActivityCreated");
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        Log.v(LOG_TAG, "onAttach");

        if (activity instanceof OnItemSelectedListener) {
            listener = (OnItemSelectedListener) activity;
        } else {
            throw new ClassCastException(activity.toString()
                    + " must implemenet TopTenFragment.OnItemSelectedListener");
        }
    }

    @Override
    public void onDetach() {
        Log.v(LOG_TAG, "onDetach");

        listener = null;

        super.onDetach();
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.v(LOG_TAG, "onStart");

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.d(LOG_TAG, "onSaveInstanceState");
        super.onSaveInstanceState(outState);
        mTopTenAdapter.save(outState);
    }

    private void SetupViewList(){
        Log.v(LOG_TAG, "SetupViewList");

        mTopTenListView = (ListView) rootView.findViewById(R.id.top_ten_listview);

        mTopTenListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mCurrentPosition = position;

                // need to setup info for player...
                // need to setup info for player...
                TopTenTracks tracks = (TopTenTracks) parent.getAdapter().getItem(position);
                Bundle bundle = CreateMediaBundle(position, tracks);
                listener.onTopTenItemSelected(bundle);
            }
        });
    }

    @Override
    public void onPause() {
        Log.v(LOG_TAG, "onPause");
        super.onPause();
    }

    @Override
    public void onResume() {
        Log.v(LOG_TAG, "onResume");
        super.onResume();
    }

    @Override
    public void onDestroy() {
        Log.v(LOG_TAG, "onDestroy");

        super.onDestroy();
    }

    public void update(Bundle bundle) {
        String id = bundle.getString(ARTIST_ID);
        artistName = bundle.getString(ARTIST_ARTIST_NAME);
        mCountryCode = bundle.getString(COUNTRY_CODE);

        mTopTenAdapter.setArtist(artistName);
        mTopTenAdapter.setCountryCode(mCountryCode);
        mTopTenAdapter.fetchNewData(id);
    }

    public void clear() {
        Log.v(LOG_TAG, "clear");
        mTopTenAdapter.clear();
    }

    private void UpdateView() {
        Log.v(LOG_TAG, "UpdateView");

        SetupViewList();

        mTopTenAdapter = new TopTenTracksAdapter(this, getActivity().getApplicationContext(),R.layout.artist_list_item, mCountryCode);
        mTopTenAdapter.setArtist(artistName);

        mTopTenListView.setAdapter(mTopTenAdapter);
    }

    private Bundle CreateMediaBundle(int position, TopTenTracks tracks) {
        Bundle bundle = new Bundle();

        bundle.putInt(TRACK_BUNDLE_POSITION, position);
        bundle.putString(TRACK_BUNDLE_ID, tracks.getSpotifyID());
        bundle.putString(TRACK_BUNDLE_ARTIST, tracks.getArtist());
        bundle.putString(TRACK_BUNDLE_ALBUM, tracks.getAlbum());
        bundle.putString(TRACK_BUNDLE_TRACK, tracks.getTrack());
        bundle.putString(TRACK_BUNDLE_PREVIEW, tracks.getPreviewUrl());
        bundle.putString(TRACK_BUNDLE_IMAGE, tracks.getImageResource());

        return bundle;
    }

    private void emptyViewList() {
        mTopTenAdapter.clear();
    }

    private void noTracksFound(){
        String temp = getActivity().getApplicationContext().getResources().getString(R.string.no_tracks_found);
        Toast.makeText(getActivity().getApplicationContext(), temp, Toast.LENGTH_SHORT).show();

        emptyViewList();
    }

    private void UpdateVars(Bundle bundle, Bundle savedInstanceState) {
        if (bundle != null) {
            String id = bundle.getString(ARTIST_ID);
            artistName = bundle.getString(ARTIST_ARTIST_NAME);

            Log.v(LOG_TAG, "UpdateVars with bundle");

            mTopTenAdapter.fetchNewData(id);
        }
        else if ((savedInstanceState != null) &&
                (savedInstanceState.containsKey("state_top_ten"))) {
            Log.v(LOG_TAG, "UpdateVars no bundle with saved data");
            mTopTenAdapter.restore(savedInstanceState);
        }
    }

    public void onTopTenItemsReady(Bundle[] bundles) {
        // pass the bundles up to the activity...
        listener.onTopTenItemReceived(bundles);
    }
}
