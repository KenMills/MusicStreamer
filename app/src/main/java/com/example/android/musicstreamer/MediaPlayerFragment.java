package com.example.android.musicstreamer;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
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

    private boolean isServiceRunning = false;
    private boolean playOnServiceStart = false;

    // used for bundling tracks
    public static final String POSITION = "POSITION";
    public static final String ID = "ID";
    public static final String ARTIST = "ARTIST";
    public static final String ALBUM = "ALBUM";
    public static final String TRACK = "TRACK";
    public static final String PREVIEW = "PREVIEW";
    public static final String IMAGE = "IMAGE";

    public static Handler mediaPlayerMsgHandler;
    public static final int MSG_PLAY_COMPLETE           = 0x0001;
    public static final int MSG_MEDIA_PREPARED          = 0x0002;
    public static final int MSG_MEDIA_PLAY              = 0x0003;
    public static final int MSG_MEDIA_PAUSE             = 0x0003;
    public static final int MSG_MEDIA_SET_POS           = 0x0004;
    public static final int MSG_MEDIA_GET_POS           = 0x0005;
    public static final int MSG_MEDIA_GET_MAX           = 0x0006;
    public static final int MSG_MEDIA_SERVICE_STARTED   = 0x0007;
    public static final int MSG_MEDIA_SERVICE_RUNNING   = 0x0008;
    public static final int MSG_PLAY_STARTED            = 0x0009;

    private static final String MEDIA_PREF = "MEDIA PREF";
    private static final String MEDIA_IS_PLAYING = "IS_PLAYING";
    private static final String MEDIA_CURRENT_POSITION = "CURRENT_POSITION";
    private static final String MEDIA_MAX_POSITION = "MAX_POSITION";

    private int mCurrentMediaPosition = 0;
    private int mMaxMediaDuration = 0;

    private boolean isActionBtnPressed = false;
    private boolean isRestoreNeeded = true;
    private boolean isUpdated = false;

    private OnTrackUpdateListener listener;

    public MediaPlayerFragment(){
        Log.d(LOG_TAG, "MediaPlayerFragment");
    }

    public interface OnTrackUpdateListener {
        public void onMediaStarted();
        public void onMediaPaused();
        public void onServiceStarted();
        public void onDecrementTrack();
        public void onIncrementTrack();
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.mediaplayer_fragment, container, false);

        Log.v(LOG_TAG, "onCreateView");

        setupMessageHandler();
        setupViews();

        // start the media player service and pass the preview url
        Intent serviceIntent = new Intent(getActivity(), MediaService.class);
        Bundle bundle = new Bundle();
        bundle.putString(PREVIEW, previewUrl);
        bundle.putInt(POSITION, position);
        serviceIntent.putExtras(bundle);
        getActivity().startService(serviceIntent);

        if (savedInstanceState != null) {
            RestorePreferences();
        }

        return rootView;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log.v(LOG_TAG, "onCreateDialog");
        playOnServiceStart = true;
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

    private void setupMessageHandler() {
        // setup the message handler used by the MediaPlayerActivity, MediaService, and MediaController
        mediaPlayerMsgHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
//                Log.d(LOG_TAG, "mediaPlayerMsgHandler");

                switch (msg.what) {
                    case MSG_MEDIA_SERVICE_STARTED: {
                        Log.v(LOG_TAG, "mediaPlayerMsgHandler  MSG_MEDIA_SERVICE_STARTED");

                        isServiceRunning = true;
                        if (playOnServiceStart) {
                            setPlayerState(PLAY_STATE_PLAYING);
                            SendUrlToService();
                            MediaService.mediaServiceMsgHandler.sendEmptyMessage(MediaService.MSG_MEDIA_RESET);
                        }

                        listener.onServiceStarted();
                        break;
                    }

                    case MSG_MEDIA_SERVICE_RUNNING: {
                        Log.v(LOG_TAG, "mediaPlayerMsgHandler  MSG_MEDIA_SERVICE_RUNNING");
                        Log.v(LOG_TAG, "mediaPlayerMsgHandler  isActionBtnPressed = " + isActionBtnPressed);

                        if (isRestoreNeeded) {
                            RestorePreferences();
                        }


                        // need to get the latest bundle from the service and use that as the current playing...
                        if (!isUpdated) {
                            update(msg.getData());
                        }

                        Log.v(LOG_TAG, "MSG_MEDIA_SERVICE_RUNNING position = " +position);

                        isServiceRunning = true;

                        if (playOnServiceStart && (playerState == PLAY_STATE_IDLE)) {
                            setPlayerState(PLAY_STATE_PLAYING);
                            SendUrlToService();
                            MediaService.mediaServiceMsgHandler.sendEmptyMessage(MediaService.MSG_MEDIA_RESET);
                        }
                        break;
                    }

                    case MSG_MEDIA_PLAY: {
                        Bundle msgBundle = msg.getData();

                        position = msgBundle.getInt(POSITION);
                        id = msgBundle.getString(ID);
                        albumName = msgBundle.getString(ALBUM);
                        trackName = msgBundle.getString(TRACK);
                        previewUrl = msgBundle.getString(PREVIEW);
                        imageUrl = msgBundle.getString(IMAGE);

                        // update imageView
                        SetImage();

                        // update media player
                        SendUrlToService();
                        MediaService.mediaServiceMsgHandler.sendEmptyMessage(MediaService.MSG_MEDIA_RESET);
                        break;
                    }
                    case MSG_PLAY_STARTED: {
                        Log.v(LOG_TAG, "mediaPlayerMsgHandler  MSG_PLAY_STARTED");
                        MediaService.mediaServiceMsgHandler.sendEmptyMessage(MediaService.MSG_MEDIA_GET_POS);

                        setPlayerState(PLAY_STATE_PLAYING);
                        UpdatePlayPauseButton();
                        break;
                    }
                    case MSG_PLAY_COMPLETE: {
                        Log.v(LOG_TAG, "mediaPlayerMsgHandler  MSG_PLAY_COMPLETE");

                        mCurrentMediaPosition = mMaxMediaDuration;

                        UpdatePlayPauseButton();
                        UpdateSeekBar();
                        setPlayerState(PLAY_STATE_IDLE);
                        break;
                    }
                    case MSG_MEDIA_GET_POS: {
                        mCurrentMediaPosition = msg.arg1;
                        mMaxMediaDuration = msg.arg2;

                        Log.v(LOG_TAG, "mediaPlayerMsgHandler  MSG_MEDIA_GET_POS mCurrentMediaPosition = " + mCurrentMediaPosition);

                        UpdatePlayPauseButton();
                        UpdateSeekBar();
                        break;
                    }
                    case MSG_MEDIA_GET_MAX: {
                        Log.v(LOG_TAG, "mediaPlayerMsgHandler  MSG_MEDIA_GET_MAX");
                        mMaxMediaDuration = msg.arg1;
                        UpdateSeekBar();
                        break;
                    }
                }

                return true;
            }
        });

    }

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
            isRestoreNeeded = false;
            isUpdated = true;

            if (isServiceRunning) {
                Log.v(LOG_TAG, "update isServiceRunning = " +isServiceRunning);
                setPlayerState(PLAY_STATE_PLAYING);
                SendUrlToService();
                MediaService.mediaServiceMsgHandler.sendEmptyMessage(MediaService.MSG_MEDIA_RESET);
            }
        }
    }

    private void RestorePreferences() {
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

        isUpdated = false;
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

        super.onResume();
    }

    private void SendUrlToService() {
        Message newMessage = new Message();
        Bundle bundle = new Bundle();
        bundle.putString(MediaService.PREVIEW, previewUrl);
        bundle.putInt(POSITION, position);
        newMessage.what = MediaService.MSG_MEDIA_SET_URL;
        newMessage.setData(bundle);
        MediaService.mediaServiceMsgHandler.sendMessage(newMessage);
    }

    public void onRev() {
        Log.v(LOG_TAG, "onRev");

        MediaService.mediaServiceMsgHandler.sendEmptyMessage(MediaService.MSG_MEDIA_PAUSE);
        listener.onDecrementTrack();
    }

    public void onPlayPause() {

        if (playerState == PLAY_STATE_PLAYING) {
            Log.v(LOG_TAG, "onPlayPause PAUSING PLAYBACK");
            setPlayerState(PLAY_STATE_PAUSED);
            MediaService.mediaServiceMsgHandler.sendEmptyMessage(MediaService.MSG_MEDIA_PAUSE);
        }
        else if (playerState == PLAY_STATE_PAUSED) {
            Log.v(LOG_TAG, "onPlayPause RESUMING PLAYBACK");
            setPlayerState(PLAY_STATE_PLAYING);
            MediaService.mediaServiceMsgHandler.sendEmptyMessage(MediaService.MSG_MEDIA_RESUME);
        }

        UpdatePlayPauseButton();
        MediaService.mediaServiceMsgHandler.sendEmptyMessage(MediaService.MSG_MEDIA_GET_POS);
    }

    public void onFwd() {
        Log.v(LOG_TAG, "onFwd");

        MediaService.mediaServiceMsgHandler.sendEmptyMessage(MediaService.MSG_MEDIA_PAUSE);
        listener.onIncrementTrack();
    }

    public void onActionButtonPressed() {
        Log.v(LOG_TAG, "onActionButtonPressed");
        isActionBtnPressed = true;
    }

    private void setPlayerState(int playState) {
        playerState = playState;
    }

}
