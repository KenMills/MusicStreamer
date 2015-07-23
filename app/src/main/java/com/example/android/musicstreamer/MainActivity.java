package com.example.android.musicstreamer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;


public class MainActivity extends ActionBarActivity
        implements ArtistFragment.OnItemSelectedListener,
                   TopTenFragment.OnItemSelectedListener,
                   MediaPlayerFragment.OnTrackUpdateListener {

    private static final String LOG_TAG = "MainActivity";
    private static final String MEDIAPLAYER_FRAGMENT = "MediaPlayerFragment";

    private static final String ARTIST_FRAGMENT_POSITION = "POSITION";
    private static final String ARTIST_FRAGMENT_ID       = "ID";
    private static final String ARTIST_FRAGMENT_ARTIST   = "ARTIST";

    private static final String BROADCAST_ACTION_PLAY    = "Play";
    private static final String BROADCAST_ACTION_PAUSE   = "Pause";
    private static final String BROADCAST_ACTION_PREV    = "Previous";
    private static final String BROADCAST_ACTION_NEXT    = "Next";

    private static final String DEFAULT_COUNTRY_CODE     = "US";
    private static final boolean DEFAULT_NOTIFICATION_SETTING = true;

    private final String COUNTRY_CODE         = "COUNTRY_CODE";
    private final String SAVED_SCREEN_STATE = "SavedScreenState";
    private final String SAVED_MEDIA_STATE = "SavedMediaState";
    private int mCurrentScreenState;
    private Bundle mMediaBundle;
    private Bundle[] mCurrentTracks = null;

    private boolean isActionButtonPressed = false;
    private boolean hasMediaPlayerBeenShown = false;
    private Menu mMenu;

    private ShareActionProvider mShareActionProvider;
    private String mCountryCode = DEFAULT_COUNTRY_CODE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.v(LOG_TAG, "onCreate");
        ReadPreferences();      // used to read system preferences (notification/country code...

        if (isDualPane()) {
            showPane(R.id.fragment_artist, null);
        }

        mCurrentScreenState = R.id.fragment_artist;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        Log.v(LOG_TAG, "onCreateOptionsMenu");

        MenuItem menuItem = menu.findItem(R.id.action_share);
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);

        mMenu = menu;

        if (hasMediaPlayerBeenShown) {
            ShowMediaPlayerActionBarIcon();
            ShowShareActionBarIcon();
        }
        else {
            HideMediaPlayerActionBarIcon();
            HideShareActionBarIcon();
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        Log.v(LOG_TAG, "onOptionsItemSelected");

        switch (id) {
            case R.id.action_settings:
                Log.v(LOG_TAG, "onOptionsItemSelected action_settings selected");
                ShowPreferences();
                return true;
            case R.id.action_mediaplayer: {
                Log.v(LOG_TAG, "onOptionsItemSelected action_mediaplayer selected");

                isActionButtonPressed = true;

                if (isDualPane()) {
                    showPane(R.id.fragment_mediaplayer, null);
                }
                else {
                    showView(R.id.fragment_mediaplayer);
                }

                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.v(LOG_TAG, "onSaveInstanceState");
        outState.putInt(SAVED_SCREEN_STATE, mCurrentScreenState);
        outState.putBoolean(SAVED_MEDIA_STATE, hasMediaPlayerBeenShown);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.v(LOG_TAG, "onRestoreInstanceState");

        mCurrentScreenState = savedInstanceState.getInt(SAVED_SCREEN_STATE);
        hasMediaPlayerBeenShown = savedInstanceState.getBoolean(SAVED_MEDIA_STATE);
        showCurrentState();
    }

    @Override
    protected void onPause() {
        Log.v(LOG_TAG, "onPause");

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Log.v(LOG_TAG, "onDestroy");

        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.v(LOG_TAG, "onResume");
        ReadPreferences();

        if (isDualPane()) {
            showPane(mCurrentScreenState, null);
        }
        else {
            showView(mCurrentScreenState);
        }
    }

    //***** ARTIST INTERFACE
    public void onArtistEntered() {
        TopTenFragment fragment = (TopTenFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_topten);
        fragment.clear();

        HideMediaPlayerActionBarIcon();
        HideShareActionBarIcon();
    }

    public void onArtistItemSelected(Bundle bundle) {
        Log.v(LOG_TAG, "onArtistItemSelected");

        int position = bundle.getInt(ARTIST_FRAGMENT_POSITION);
        String id = bundle.getString(ARTIST_FRAGMENT_ID);
        String artist = bundle.getString(ARTIST_FRAGMENT_ARTIST);

        Log.d(LOG_TAG, "onArtistItemSelected position = " + position);
        Log.d(LOG_TAG, "onArtistItemSelected id = " + id);
        Log.d(LOG_TAG, "onArtistItemSelected artist = " + artist);

        updateTopTen(bundle);

        HideMediaPlayerActionBarIcon();
        HideShareActionBarIcon();
    }
    //***** ARTIST INTERFACE

    //***** TOP TEN INTERFACE
    public void onTopTenItemSelected(Bundle bundle) {
        Log.v(LOG_TAG, "onTopTenItemSelected");

        updateMediaPlayer(bundle);

        hasMediaPlayerBeenShown = true;
        HideMediaPlayerActionBarIcon();
    }

    public void onTopTenItemReceived(Bundle[] bundles) {
        Log.v(LOG_TAG, "onTopTenItemReceived");

        mCurrentTracks = bundles;
        SendTracks();
    }

    private void updateTopTen(Bundle bundle) {
        Log.d(LOG_TAG, "updateTopTen");

        bundle.putString(COUNTRY_CODE, mCountryCode);

        if (!isDualPane()) {
            showView(R.id.fragment_topten);
        }

        TopTenFragment fragment = (TopTenFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_topten);
        fragment.update(bundle);
    }
    //***** TOP TEN INTERFACE

    //***** MEDIA PLAYER INTERFACE
    private void updateMediaPlayer(Bundle bundle) {
        Log.d(LOG_TAG, "updateMediaPlayer");

        if (!isDualPane()) {
            showView(R.id.fragment_mediaplayer);

            MediaPlayerFragment fragment = (MediaPlayerFragment) getFragmentManager().findFragmentById(R.id.fragment_mediaplayer);
            fragment.update(bundle);
        }
        else {
            showPane(R.id.fragment_mediaplayer, bundle);
        }

        mMediaBundle = bundle;
        ShowShareActionBarIcon();
        UpdateNotification();
    }

    public void onDecrementTrack() {
        Log.d(LOG_TAG, "onDecrementTrack");

        TopTenFragment fragment = (TopTenFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_topten);
        fragment.previousTrack();
    }

    public void onIncrementTrack() {
        Log.d(LOG_TAG, "onIncrementTrack");

        TopTenFragment fragment = (TopTenFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_topten);
        fragment.nextTrack();
    }

    public void onMediaStarted() {
        Log.d(LOG_TAG, "onMediaStarted");

        MediaPlayerFragment fragment = (MediaPlayerFragment)getFragmentManager().findFragmentByTag(MEDIAPLAYER_FRAGMENT);
        if (fragment != null) {
            if (isActionButtonPressed) {
                Log.d(LOG_TAG, "onMediaStarted isActionButtonPressed");

                fragment.onActionButtonPressed();
                isActionButtonPressed = false;
            }
            else if ((mMediaBundle != null)) {
                fragment.update(mMediaBundle);
            }
        }

        HideMediaPlayerActionBarIcon();
    }

    public void onMediaPaused() {
        if (!isDualPane()) {
            DismissDialog();
        }

        mCurrentScreenState = R.id.fragment_topten;

        ShowMediaPlayerActionBarIcon();
    }

    public void onServiceStarted() {
        SendTracks();
    }
    //***** MEDIA PLAYER INTERFACE

    //***** View Management
    @Override
    public void onBackPressed() {
        Log.v(LOG_TAG, "onBackPressed");

        int screenOrientation = getResources().getConfiguration().orientation;
        if (screenOrientation == Configuration.ORIENTATION_PORTRAIT) {

            if (findViewById(R.id.fragment_topten).getVisibility() == View.VISIBLE) {
                showView(R.id.fragment_artist);
            }
            else if (findViewById(R.id.fragment_mediaplayer).getVisibility() == View.VISIBLE) {
                showView(R.id.fragment_topten);
            }
            else {
                super.onBackPressed();
            }
        }
        else {
            super.onBackPressed();
        }
    }

    private void showView(int fragmentID) {

        mCurrentScreenState = fragmentID;
        switch (fragmentID) {
            case R.id.fragment_artist:{
                Log.d(LOG_TAG, "showView: fragment_artist");
                ShowMediaPlayerActionBarIcon();

                findViewById(R.id.fragment_artist).setVisibility(View.VISIBLE);
                findViewById(R.id.fragment_topten).setVisibility(View.GONE);
                findViewById(R.id.fragment_mediaplayer).setVisibility(View.GONE);
                break;
            }
            case R.id.fragment_topten:{
                Log.d(LOG_TAG, "showView: fragment_topten");
                ShowMediaPlayerActionBarIcon();

                findViewById(R.id.fragment_artist).setVisibility(View.GONE);
                findViewById(R.id.fragment_topten).setVisibility(View.VISIBLE);
                findViewById(R.id.fragment_mediaplayer).setVisibility(View.GONE);
                break;
            }
            case R.id.fragment_mediaplayer:{
                Log.d(LOG_TAG, "showView: fragment_mediaplayer");
                hasMediaPlayerBeenShown = true;
                HideMediaPlayerActionBarIcon();

                findViewById(R.id.fragment_artist).setVisibility(View.GONE);
                findViewById(R.id.fragment_topten).setVisibility(View.GONE);
                findViewById(R.id.fragment_mediaplayer).setVisibility(View.VISIBLE);
                break;
            }
        }
    }

    private void showPane(int fragmentID, Bundle bundle) {
        switch (fragmentID) {
            case R.id.fragment_artist:{
                Log.d(LOG_TAG, "showPane: fragment_artist");
                ShowMediaPlayerActionBarIcon();

                findViewById(R.id.fragment_artist).setVisibility(View.VISIBLE);
                findViewById(R.id.fragment_topten).setVisibility(View.VISIBLE);
                break;
            }
            case R.id.fragment_topten:{
                Log.d(LOG_TAG, "showPane: fragment_topten");
                ShowMediaPlayerActionBarIcon();

                findViewById(R.id.fragment_artist).setVisibility(View.VISIBLE);
                findViewById(R.id.fragment_topten).setVisibility(View.VISIBLE);
                break;
            }
            case R.id.fragment_mediaplayer:{
                Log.d(LOG_TAG, "showPane: fragment_mediaplayer");
                hasMediaPlayerBeenShown = true;
                ShowMediaPlayerActionBarIcon();

                findViewById(R.id.fragment_artist).setVisibility(View.VISIBLE);
                findViewById(R.id.fragment_topten).setVisibility(View.VISIBLE);

                // need to display the mediaplayer dialog
                MediaPlayerFragment fragment = getMediaPlayerFragment();

                if (!fragment.isVisible() && !fragment.isAdded()) {
                    Log.d(LOG_TAG, "showPane: fragment_mediaplayer SHOWING...");
                    fragment.show(getFragmentManager(), MEDIAPLAYER_FRAGMENT);
                }

                if (fragment.isVisible() && fragment.isAdded() && (bundle != null)) {
                    Log.d(LOG_TAG, "showPane: fragment_mediaplayer UPDATING...");
                    fragment.update(bundle);
                }

                break;
            }
        }

        mCurrentScreenState = fragmentID;
    }

    private boolean isTablet() {
        boolean isLarge = getResources().getBoolean(R.bool.isTablet);
        Log.d(LOG_TAG, "isLarge: " +isLarge);

        return isLarge;
    }

    private boolean isDualPane() {
        boolean ret = true;

        int screenOrientation = getResources().getConfiguration().orientation;
        if ((screenOrientation == Configuration.ORIENTATION_PORTRAIT) && (!isTablet())) {
            ret = false;
        }

        return ret;
    }

    private MediaPlayerFragment getMediaPlayerFragment() {
        MediaPlayerFragment fragment = (MediaPlayerFragment)getFragmentManager().findFragmentByTag(MEDIAPLAYER_FRAGMENT);
        if (fragment == null) {
            fragment = new MediaPlayerFragment();
        }

        return fragment;
    }

    private void showCurrentState() {
        switch (mCurrentScreenState) {
            case R.id.fragment_artist: {
                Log.d(LOG_TAG, "showCurrentState: fragment_artist");
                break;
            }
            case R.id.fragment_topten: {
                Log.d(LOG_TAG, "showCurrentState: fragment_topten");
                break;
            }
            case R.id.fragment_mediaplayer: {
                Log.d(LOG_TAG, "showCurrentState: fragment_mediaplayer");
                break;
            }
        }
    }

    private void DismissDialog() {
        MediaPlayerFragment fragment = (MediaPlayerFragment)getFragmentManager().findFragmentByTag(MEDIAPLAYER_FRAGMENT);

        if (fragment != null) {
            fragment.dismiss();
        }
    }
    //***** View Management

    private void ShowMediaPlayerActionBarIcon() {
        Log.v(LOG_TAG, "ShowMediaPlayerActionBarIcon");
        if (hasMediaPlayerBeenShown &&
                (mMenu != null) &&
                (mCurrentScreenState != R.id.fragment_mediaplayer)) {
            MenuItem item = mMenu.findItem(R.id.action_mediaplayer);
            item.setVisible(true);
        }
    }

    private void HideMediaPlayerActionBarIcon() {
        Log.v(LOG_TAG, "HideMediaPlayerActionBarIcon");
        if (mMenu != null) {
            MenuItem item = mMenu.findItem(R.id.action_mediaplayer);
            item.setVisible(false);
        }
    }

    private void ShowShareActionBarIcon() {
        Log.v(LOG_TAG, "ShowShareActionBarIcon");
        if (hasMediaPlayerBeenShown && (mMenu != null)) {
            MenuItem item = mMenu.findItem(R.id.action_share);
            item.setVisible(true);
        }
    }

    private void HideShareActionBarIcon() {
        Log.v(LOG_TAG, "HideShareActionBarIcon");
        if (mMenu != null) {
            MenuItem item = mMenu.findItem(R.id.action_share);
            item.setVisible(false);
        }
    }

    private void UpdateNotification() {
        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(createShareForecastIntent());
        }
    }

    private Intent createShareForecastIntent() {
        Intent shareIntent = null;

        if (mMediaBundle != null) {
            String playUrl = mMediaBundle.getString(MediaPlayerFragment.PREVIEW);
            shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, playUrl);
        }

        return shareIntent;
    }

    private void ShowPreferences() {
        Intent i = new Intent(this, PrefActivity.class);
        startActivity(i);
    }

    private void ReadPreferences() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        mCountryCode = sharedPref.getString(getString(R.string.countrycode_preference), DEFAULT_COUNTRY_CODE);

        Log.d(LOG_TAG, "ReadPreferences countryCode = " + mCountryCode);
        if (MediaService.mediaServiceMsgHandler != null) {
            MediaService.mediaServiceMsgHandler.sendEmptyMessage(MediaService.MSG_READ_PREF);
        }
    }

    private void SendTracks() {
        if (mCurrentTracks != null) {
            for (int i=0; i<mCurrentTracks.length; i++) {
                SendTrackToService(mCurrentTracks[i]);
            }
        }
    }

    private void SendTrackToService(Bundle bundle) {
        if (MediaService.mediaServiceMsgHandler != null) {
            Message newMessage = new Message();
            newMessage.what = MediaService.MSG_MEDIA_BUNDLE;
            newMessage.setData(bundle);
            MediaService.mediaServiceMsgHandler.sendMessage(newMessage);
        }
    }
}
