package com.example.android.musicstreamer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * Created by kenm on 7/23/2015.
 */
public class ServiceController {
    private String LOG_TAG = "ServiceController";
    private Activity mActivity;

    public static Handler serviceControllerMsgHandler;
    public static final int MSG_PLAY_COMPLETE           = 0x0001;
    public static final int MSG_MEDIA_PREPARED          = 0x0002;
    public static final int MSG_MEDIA_PLAY              = 0x0003;
    public static final int MSG_MEDIA_PAUSE             = 0x0004;
    public static final int MSG_MEDIA_SET_POS           = 0x0005;
    public static final int MSG_MEDIA_GET_POS           = 0x0006;
    public static final int MSG_MEDIA_GET_MAX           = 0x0007;
    public static final int MSG_MEDIA_SERVICE_STARTED   = 0x0008;
    public static final int MSG_MEDIA_SERVICE_RUNNING   = 0x0009;
    public static final int MSG_PLAY_STARTED            = 0x000a;
    public static final int MSG_TRACK_INFO              = 0x000b;
    public static final int MSG_PLAY_STATE              = 0x000c;

    private final int PLAY_STATE_IDLE    = 0;
    private final int PLAY_STATE_PLAYING = 1;
    private final int PLAY_STATE_PAUSED  = 2;

    private int playerState = PLAY_STATE_IDLE;
    private int currentPosition = 0;

    private boolean playOnServiceStart = false;
    private OnServiceListener listener;
    private Bundle mMediaBundle;

    public interface OnServiceListener {
        public void onServiceStarted();
        public void onServiceRunning(Bundle bundle);
        public void onTrackUpdated(Bundle bundle);
        public void onPlayStateChanged(int playState);
        public void onProgressChanged(int current, int max);
    }

    public ServiceController(Activity activity) {
        mActivity = activity;
        listener = (OnServiceListener) activity;
    }

    public void setupService() {
        Log.v(LOG_TAG, "setupService");

        setupMessageHandler();

        // start the media player service and pass the preview url
        Intent serviceIntent = new Intent(mActivity, MediaService.class);
        mActivity.startService(serviceIntent);
    }

    private void setupMessageHandler() {
        serviceControllerMsgHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
//                Log.d(LOG_TAG, "setupMessageHandler");

                switch (msg.what) {
                    case MSG_MEDIA_SERVICE_STARTED: {
                        Log.v(LOG_TAG, "setupMessageHandler  MSG_MEDIA_SERVICE_STARTED");

                        if (playOnServiceStart) {
                            MediaService.mediaServiceMsgHandler.sendEmptyMessage(MediaService.MSG_MEDIA_RESET);
                        }

                        listener.onServiceStarted();
                        break;
                    }

                    case MSG_MEDIA_SERVICE_RUNNING: {
                        Log.v(LOG_TAG, "setupMessageHandler  MSG_MEDIA_SERVICE_RUNNING");

                        currentPosition = msg.arg1;
                        Log.v(LOG_TAG, "MSG_MEDIA_SERVICE_RUNNING position = " + currentPosition);

                        listener.onServiceRunning(msg.getData());
                        mMediaBundle = msg.getData();

                        if (playOnServiceStart && (playerState == PLAY_STATE_IDLE)) {
                            setPlayerState(PLAY_STATE_PLAYING);
                            MediaService.mediaServiceMsgHandler.sendEmptyMessage(MediaService.MSG_MEDIA_RESET);
                        }
                        break;
                    }

                    case MSG_MEDIA_PLAY: {
                        MediaService.mediaServiceMsgHandler.sendEmptyMessage(MediaService.MSG_MEDIA_RESET);
                        break;
                    }
                    case MSG_PLAY_STARTED: {
                        Log.v(LOG_TAG, "setupMessageHandler  MSG_PLAY_STARTED");
                        MediaService.mediaServiceMsgHandler.sendEmptyMessage(MediaService.MSG_MEDIA_GET_POS);

                        setPlayerState(PLAY_STATE_PLAYING);
                        break;
                    }
                    case MSG_PLAY_COMPLETE: {
                        Log.v(LOG_TAG, "setupMessageHandler  MSG_PLAY_COMPLETE");
                        setPlayerState(PLAY_STATE_IDLE);
                        break;
                    }
                    case MSG_MEDIA_GET_POS: {
                        int current = msg.arg1;
                        int max = msg.arg2;
                        listener.onProgressChanged(current, max);
                        break;
                    }
                    case MSG_MEDIA_GET_MAX: {
                        Log.v(LOG_TAG, "setupMessageHandler  MSG_MEDIA_GET_MAX");
                        break;
                    }
                    case MSG_TRACK_INFO: {
                        Log.v(LOG_TAG, "setupMessageHandler  MSG_TRACK_INFO");
                        listener.onTrackUpdated(msg.getData());
                        mMediaBundle = msg.getData();
                        break;
                    }
                    case MSG_PLAY_STATE: {
                        int playState = msg.arg1;
                        Log.v(LOG_TAG, "setupMessageHandler  MSG_PLAY_STATE playState = " +playState);

                        listener.onPlayStateChanged(playState);
                        break;
                    }
                }

                return true;
            }
        });

    }

    private void setPlayerState(int playState) {
        playerState = playState;
    }

    public void IncrementTrack() {
        MediaService.mediaServiceMsgHandler.sendEmptyMessage(MediaService.MSG_INC_TRACK);
    }

    public void DecrementTrack() {
        MediaService.mediaServiceMsgHandler.sendEmptyMessage(MediaService.MSG_DEC_TRACK);
    }

    public void PauseTrack() {
        MediaService.mediaServiceMsgHandler.sendEmptyMessage(MediaService.MSG_MEDIA_PAUSE);
    }

    public void ResumeTrack() {
        MediaService.mediaServiceMsgHandler.sendEmptyMessage(MediaService.MSG_MEDIA_RESUME);
    }
    public void ReadPreferences() {
        Log.d(LOG_TAG, "ReadPreferences");
        if (MediaService.mediaServiceMsgHandler != null) {
            MediaService.mediaServiceMsgHandler.sendEmptyMessage(MediaService.MSG_READ_PREF);
        }
    }

    public void SendTrackToService(Bundle bundle) {
        if (MediaService.mediaServiceMsgHandler != null) {
            Message newMessage = new Message();
            newMessage.what = MediaService.MSG_MEDIA_BUNDLE;
            newMessage.setData(bundle);
            MediaService.mediaServiceMsgHandler.sendMessage(newMessage);
        }
    }

    public void PlayTrack(int position) {
        if (MediaService.mediaServiceMsgHandler != null) {
            Message newMessage = new Message();
            newMessage.what = MediaService.MSG_MEDIA_PLAY;
            newMessage.arg1 = position;
            MediaService.mediaServiceMsgHandler.sendMessage(newMessage);
        }

    }

    public void RequestCurrentTrack() {
        Log.v(LOG_TAG, "RequestCurrentTrack");
        if (MediaService.mediaServiceMsgHandler != null) {
            MediaService.mediaServiceMsgHandler.sendEmptyMessage(MediaService.MSG_GET_CURR_TRACK);
        }
    }
    public Bundle GetCurrentMediaBundle() {
        Log.v(LOG_TAG, "GetCurrentMediaBundle");
        return mMediaBundle;
    }

    public void StartNotifications() {
        if (MediaService.mediaServiceMsgHandler != null) {
            MediaService.mediaServiceMsgHandler.sendEmptyMessage(MediaService.MSG_START_NOTIFY);
        }
    }

    public void StopNotifications() {
        if (MediaService.mediaServiceMsgHandler != null) {
            MediaService.mediaServiceMsgHandler.sendEmptyMessage(MediaService.MSG_STOP_NOTIFY);
        }
    }
}
