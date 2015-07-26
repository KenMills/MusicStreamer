package com.example.android.musicstreamer;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

/**
 * Created by kenm on 7/8/2015.
 */
public class MediaPlayerFragment extends DialogFragment {
    private String LOG_TAG = "MediaPlayerFragment";
    private String testUrl_1 = "https://p.scdn.co/mp3-preview/45b40cc30b32d50bea1572ec5433dd8372e0fddf";

    private View rootView;
    private SeekBar mSeekBar = null;
    private Button mRev = null;
    private Button mFrd = null;
    private Button mPlayPause = null;
    private ImageView mImageView = null;

    private static final int ONE_HUNDRED = 100;
    private static final int ONE_THOUSAND = 1000;
    private int position = 0;
    private String id;
    private String artistName;
    private String albumName;
    private String trackName;
    private String previewUrl = testUrl_1;
    private String imageUrl = null;
    private static final int TARGET_WIDTH = 200;
    private static final int TARGET_HEIGHT = 200;

    private final int PLAY_STATE_IDLE    = 0;
    private final int PLAY_STATE_PLAYING = 1;
    private final int PLAY_STATE_PAUSED  = 2;

    private int playerState = PLAY_STATE_IDLE;

    // used for bundling tracks
    public static final String POSITION = "POSITION";
    public static final String ID = "ID";
    public static final String ARTIST = "ARTIST";
    public static final String ALBUM = "ALBUM";
    public static final String TRACK = "TRACK";
    public static final String PREVIEW = "PREVIEW";
    public static final String IMAGE = "IMAGE";


    private static final String MEDIA_PREF = "MEDIA PREF";
    private static final String MEDIA_IS_PLAYING = "IS_PLAYING";
    private static final String MEDIA_CURRENT_POSITION = "CURRENT_POSITION";
    private static final String MEDIA_MAX_POSITION = "MAX_POSITION";

    private int mCurrentMediaPosition = 0;
    private int mMaxMediaDuration = 0;
    private boolean viewsSetup = false;

    private OnTrackUpdateListener listener;

    public MediaPlayerFragment(){
        Log.d(LOG_TAG, "MediaPlayerFragment");
    }

    public interface OnTrackUpdateListener {
        public void onMediaStarted();
        public void onMediaPaused();
        public void onMediaDismiss();
        public void onDecrementTrack();
        public void onIncrementTrack();
        public void onPauseTrack();
        public void onResumeTrack();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        Log.v(LOG_TAG, "onAttach");

        if (activity instanceof OnTrackUpdateListener) {
            listener = (OnTrackUpdateListener) activity;
        } else {
            throw new ClassCastException(activity.toString()
                    + " must implemenet MediaPlayerFragment.OnTrackUpdateListener");
        }
    }

    @Override
    public void onDetach() {
        Log.v(LOG_TAG, "onDetach");

        listener = null;

        super.onDetach();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        Log.v(LOG_TAG, "onDismiss");

        if (listener != null) {
            listener.onMediaDismiss();
        }

        super.onDismiss(dialog);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.mediaplayer_fragment, container, false);

        Log.v(LOG_TAG, "onCreateView");

        setupViews();

        if (savedInstanceState != null) {
            RestorePreferences();
        }

        return rootView;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log.v(LOG_TAG, "onCreateDialog");
        return super.onCreateDialog(savedInstanceState);
    }

    private void setupViews() {
        mSeekBar = (SeekBar) rootView.findViewById(R.id.seekBar);
        mSeekBar.setOnSeekBarChangeListener(seekBarListner);

        mRev = (Button) rootView.findViewById(R.id.button_rewind);
        mRev.setOnClickListener(previousButtonListener);

        mFrd = (Button) rootView.findViewById(R.id.button_forward);
        mFrd.setOnClickListener(forwardButtonListener);

        mPlayPause = (Button) rootView.findViewById(R.id.play_pause_button);
        mPlayPause.setOnClickListener(playPauseButtonListener);

        mImageView = (ImageView) rootView.findViewById(R.id.media_player_imageView);
        viewsSetup = true;
    }

    private Button.OnClickListener previousButtonListener = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            onRev();
        }
    };

    private Button.OnClickListener forwardButtonListener = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            onFwd();
        }
    };

    private Button.OnClickListener playPauseButtonListener = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            onPlayPause();
        }
    };

    private SeekBar.OnSeekBarChangeListener seekBarListner =
            new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    Log.v(LOG_TAG, "onStartTrackingTouch");
                    MediaService.mediaServiceMsgHandler.sendEmptyMessage(MediaService.MSG_MEDIA_PAUSE);
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    int progress = seekBar.getProgress();
                    int position = progress * mMaxMediaDuration / ONE_HUNDRED;

                    Message msg = new Message();
                    msg.what = MediaService.MSG_MEDIA_SET_POS;
                    msg.arg1 = position;
                    MediaService.mediaServiceMsgHandler.sendMessage(msg);
                    MediaService.mediaServiceMsgHandler.sendEmptyMessage(MediaService.MSG_MEDIA_RESUME);

                    Log.v(LOG_TAG, "onStopTrackingTouch position = " +position);
                }
            };


    public void update(Bundle bundle) {
        Log.v(LOG_TAG, "update");
        if (bundle != null) {
            position = bundle.getInt(POSITION);
            id = bundle.getString(ID);
            artistName = bundle.getString(ARTIST);
            albumName = bundle.getString(ALBUM);
            trackName = bundle.getString(TRACK);
            previewUrl = bundle.getString(PREVIEW);
            imageUrl = bundle.getString(IMAGE);

            SetImage();
            UpdatePlayPauseButton();
            UpdateSeekBar();
        }
    }

    public void RestorePreferences() {
        Log.v(LOG_TAG, "RestorePreferences");

        SharedPreferences pref = getActivity().getSharedPreferences(MEDIA_PREF, getActivity().MODE_PRIVATE);
        if (pref != null) {
            playerState = pref.getInt(MEDIA_IS_PLAYING, PLAY_STATE_IDLE);
            mCurrentMediaPosition = pref.getInt(MEDIA_CURRENT_POSITION, 0);
            mMaxMediaDuration = pref.getInt(MEDIA_MAX_POSITION, 0);

            Log.v(LOG_TAG, "RestorePreferences playerState = " + playerState);

            if (playerState != PLAY_STATE_IDLE) {
                SetImage();
                UpdatePlayPauseButton();
                UpdateSeekBar();

            }
        }
    }

    private void SavePreferences() {
        Log.v(LOG_TAG, "SavePreferences");

        SharedPreferences pref = getActivity().getSharedPreferences(MEDIA_PREF, getActivity().MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();

        editor.putInt(MEDIA_IS_PLAYING, playerState);
        editor.putInt(MEDIA_CURRENT_POSITION, mCurrentMediaPosition);
        editor.putInt(MEDIA_MAX_POSITION, mMaxMediaDuration);

        editor.commit();
    }

    private void SetImage() {

        if (imageUrl == null) {
            mImageView.setImageResource(R.drawable.no_image_avail);
        }
        else {
            Picasso.with(getActivity().getApplicationContext())
                    .load(imageUrl)
                    .resize(TARGET_WIDTH, TARGET_HEIGHT)
                    .centerCrop()
                    .into(mImageView);
        }

        TextView artist = (TextView) rootView.findViewById(R.id.artistTextView);
        artist.setText(artistName);

        TextView album = (TextView) rootView.findViewById(R.id.albumTextView);
        album.setText(albumName);

        TextView song = (TextView) rootView.findViewById(R.id.songTextView);
        song.setText(trackName);
    }

    private void UpdatePlayPauseButton() {
//        Log.v(LOG_TAG, "UpdatePlayPauseButton");

        if (rootView != null) {
            Button playPauseButton = (Button) rootView.findViewById(R.id.play_pause_button);
            if (playerState == PLAY_STATE_PLAYING) {
                playPauseButton.setBackgroundResource(R.drawable.ic_pause);
            }
            else {
                playPauseButton.setBackgroundResource(R.drawable.ic_play);
            }
        }
    }

    private void UpdateSeekBar() {
        float max = (float) mMaxMediaDuration / ONE_THOUSAND;
        float current = (float) mCurrentMediaPosition /ONE_THOUSAND;

        if (max == 0) {
            mSeekBar.setProgress(0);
        }
        else {
            int progress = (int) (current / max * ONE_HUNDRED);
            mSeekBar.setProgress(progress);
        }

        String currentStr = String.format("%.2f", current);
        String maxStr = String.format("%.2f", max);

        TextView currentPosition = (TextView) rootView.findViewById(R.id.currentPosition);
        TextView maxDuration = (TextView) rootView.findViewById(R.id.maxPosition);

        currentPosition.setText(currentStr);
        maxDuration.setText(maxStr);

//        Log.d(LOG_TAG, "UpdateSeekBar() mCurrentPosition = " + currentStr);
//        Log.d(LOG_TAG, "UpdateSeekBar() mMaxDuration = " + maxStr);
    }

    @Override
    public void onStart() {
        Log.v(LOG_TAG, "onStart");

        listener.onMediaStarted();

        super.onStart();
    }

    @Override
    public void onPause() {
        Log.v(LOG_TAG, "onPause");
        SavePreferences();

        listener.onMediaPaused();
        super.onPause();
    }

    @Override
    public void onResume() {
        Log.v(LOG_TAG, "onResume");

        RestorePreferences();

        super.onResume();
    }

    @Override
    public void onStop() {
        viewsSetup = false;
        super.onStop();
    }

    public void onRev() {
        Log.v(LOG_TAG, "onRev");

        mMaxMediaDuration = 0;
        mCurrentMediaPosition = 0;

        listener.onDecrementTrack();
    }

    public void onPlayPause() {

        if (playerState == PLAY_STATE_PLAYING) {
            Log.v(LOG_TAG, "onPlayPause PAUSING PLAYBACK");
            listener.onPauseTrack();
        }
        else if (playerState == PLAY_STATE_PAUSED) {
            Log.v(LOG_TAG, "onPlayPause RESUMING PLAYBACK");
            listener.onResumeTrack();
        }

        UpdatePlayPauseButton();
    }

    public void onFwd() {
        Log.v(LOG_TAG, "onFwd");

        mMaxMediaDuration = 0;
        mCurrentMediaPosition = 0;

        listener.onIncrementTrack();
    }

    public void onActionButtonPressed() {
        Log.v(LOG_TAG, "onActionButtonPressed");
    }

    public void updatePlayState(int playState) {
        Log.d(LOG_TAG, "updatePlayState");
        playerState = playState;
        UpdatePlayPauseButton();
    }

    public void updateProgress(int current, int max) {
        mCurrentMediaPosition = current;
        mMaxMediaDuration = max;

        if (viewsSetup) {
            UpdateSeekBar();
        }
    }
}
