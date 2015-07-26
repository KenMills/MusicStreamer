package com.example.android.musicstreamer;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;


/**
 * Created by kenm on 6/24/2015.
 */
public class MediaService extends Service {
    private MediaController mMediaController = null;
    public static final String SERVICE_TAG          = "MediaService";

    public static Handler mediaServiceMsgHandler;
    public static final int MSG_MEDIA_PLAY          = 0x0011;
    public static final int MSG_MEDIA_PAUSE         = 0x0012;
    public static final int MSG_MEDIA_SET_URL       = 0x0013;
    public static final int MSG_MEDIA_SET_POS       = 0x0014;
    public static final int MSG_MEDIA_GET_POS       = 0x0015;
    public static final int MSG_MEDIA_GET_MAX       = 0x0016;
    public static final int MSG_MEDIA_PLAY_STARTED  = 0x0017;
    public static final int MSG_MEDIA_PREPPED       = 0x0018;
    public static final int MSG_MEDIA_RESUME        = 0x0019;
    public static final int MSG_PLAY_COMPLETE       = 0x001a;
    public static final int MSG_INC_TRACK           = 0x001c;
    public static final int MSG_DEC_TRACK           = 0x001d;

    public static final int MSG_MEDIA_RESET         = 0x0020;
    public static final int MSG_START_NOTIFY        = 0x0021;
    public static final int MSG_STOP_NOTIFY         = 0x0022;

    public static final int MSG_MEDIA_BUNDLE        = 0x0030;
    public static final int MSG_READ_PREF           = 0x0031;
    public static final int MSG_GET_CURR_TRACK      = 0x0032;

    private static final String BROADCAST_ACTION_PLAY    = "Play";
    private static final String BROADCAST_ACTION_PAUSE   = "Pause";
    private static final String BROADCAST_ACTION_PREV    = "Previous";
    private static final String BROADCAST_ACTION_NEXT    = "Next";

    private static final boolean DEFAULT_NOTIFICATION_SETTING = false;
    private boolean mAllowNotifications = DEFAULT_NOTIFICATION_SETTING;
    private boolean mNotificationsStarted = false;

    public static final String PREVIEW = "PREVIEW";
    public static final String POSITION = "POSITION";

    private boolean serviceStarted = false;
    private MediaNotification mediaNotification = null;
    private int mCurrentTrack = 0;

    // used for bundling tracks
    private static int MAX_TRACKS = 10;
    private Bundle mBundles[];
    public final String TRACK_BUNDLE_POSITION = "POSITION";
    public final String TRACK_BUNDLE_ID       = "ID";
    public final String TRACK_BUNDLE_ARTIST   = "ARTIST";
    public final String TRACK_BUNDLE_ALBUM    = "ALBUM";
    public final String TRACK_BUNDLE_TRACK    = "TRACK";
    public final String TRACK_BUNDLE_PREVIEW  = "PREVIEW";
    public final String TRACK_BUNDLE_IMAGE    = "IMAGE";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(SERVICE_TAG, "onCreate");

        mMediaController = new MediaController(this);

        setupBundles();
        setupReceiver();
        ReadPreferences();

        mediaServiceMsgHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_MEDIA_PREPPED: {
                        Log.d(SERVICE_TAG, "mediaServiceMsgHandler  MSG_MEDIA_PLAY");
                        mMediaController.play();
                        mNotificationsStarted = true;
                        UpdateNotification();
                        break;
                    }
                    case MSG_MEDIA_RESUME: {
                        Log.d(SERVICE_TAG, "mediaServiceMsgHandler  MSG_MEDIA_RESUME");
                        mMediaController.play();
                        UpdateNotification();
                        break;
                    }
                    case MSG_MEDIA_PLAY: {
                        Log.d(SERVICE_TAG, "mediaServiceMsgHandler  MSG_MEDIA_PLAY position = " +msg.arg1);

                        if (mMediaController.getPlayState() == mMediaController.PLAY_STATE_PLAYING) {
                            mMediaController.pause();
                        }

                        mNotificationsStarted = true;
                        mCurrentTrack = msg.arg1;

                        if (mBundles[mCurrentTrack] != null) {
                            String url = mBundles[mCurrentTrack].getString(TRACK_BUNDLE_PREVIEW);
                            mMediaController.setUrl(url);
                            mMediaController.reset();

                            UpdateNotification();
                            sendTrackInfo();
                        }
                        break;
                    }
                    case MSG_PLAY_COMPLETE: {
                        Log.d(SERVICE_TAG, "mediaServiceMsgHandler  MSG_PLAY_COMPLETE");
                        UpdateNotification();
                        ServiceController.serviceControllerMsgHandler.sendEmptyMessage(ServiceController.MSG_PLAY_COMPLETE);
                        break;
                    }
                    case MSG_MEDIA_RESET: {
                        Log.d(SERVICE_TAG, "mediaServiceMsgHandler  MSG_MEDIA_RESET");
                        mMediaController.reset();
                        UpdateNotification();
                        break;
                    }
                    case MSG_MEDIA_PAUSE: {
                        Log.d(SERVICE_TAG, "mediaServiceMsgHandler  MSG_MEDIA_PAUSE");
                        mMediaController.pause();
                        UpdateNotification();
                        break;
                    }
                    case MSG_MEDIA_SET_URL: {
                        Log.d(SERVICE_TAG, "mediaServiceMsgHandler  MSG_MEDIA_SET_URL");
                        Bundle msgBundle = msg.getData();
                        String url = msgBundle.getString(PREVIEW);
                        mCurrentTrack = msgBundle.getInt(POSITION);
                        mMediaController.setUrl(url);

                        Log.d(SERVICE_TAG, "mediaServiceMsgHandler  url = " + url);
                    }
                    break;
                    case MSG_MEDIA_SET_POS: {
                        int position = msg.arg1;
                        mMediaController.setCurentPosition(position);

                        Log.d(SERVICE_TAG, "mediaServiceMsgHandler  MSG_MEDIA_SET_POS = "+position);
                    }
                    break;
                    case MSG_MEDIA_GET_POS: {
                        Log.d(SERVICE_TAG, "mediaServiceMsgHandler  MSG_MEDIA_GET_POS");

                        Message newMsg = new Message();
                        newMsg.what = ServiceController.MSG_MEDIA_GET_POS;
                        newMsg.arg1 = mMediaController.getCurentPosition();
                        newMsg.arg2 = mMediaController.getDuration();
                        if (ServiceController.serviceControllerMsgHandler != null) {
                            ServiceController.serviceControllerMsgHandler.sendMessage(newMsg);
                        }
                    }

                    break;
                    case MSG_MEDIA_GET_MAX: {
                        Log.d(SERVICE_TAG, "mediaServiceMsgHandler  MSG_MEDIA_GET_MAX");
                        int max = mMediaController.getDuration();

                        Message newMsg = new Message();
                        newMsg.what = ServiceController.MSG_MEDIA_GET_MAX;
                        newMsg.arg1 = max;
                        if (ServiceController.serviceControllerMsgHandler != null) {
                            ServiceController.serviceControllerMsgHandler.sendMessage(newMsg);
                        }
                    }
                    break;
                    case MSG_MEDIA_BUNDLE: {
                        Log.d(SERVICE_TAG, "mediaServiceMsgHandler  MSG_MEDIA_BUNDLE");
                        ExtractBundle(msg.getData());
                    }
                    break;
                    case MSG_READ_PREF: {
                        ReadPreferences();
                    }
                    break;
                    case MSG_INC_TRACK: {
                        Log.d(SERVICE_TAG, "mediaServiceMsgHandler  MSG_INC_TRACK");
                        onIncrementTrack();
                        break;
                    }
                    case MSG_DEC_TRACK: {
                        Log.d(SERVICE_TAG, "mediaServiceMsgHandler  MSG_DEC_TRACK");
                        onDecrementTrack();
                        break;
                    }
                    case MSG_GET_CURR_TRACK: {
                        Log.d(SERVICE_TAG, "mediaServiceMsgHandler  MSG_GET_CURR_TRACK");
                        sendTrackInfo();
                        break;
                    }
                    case MSG_START_NOTIFY: {
                        mNotificationsStarted = true;
                        break;
                    }
                    case MSG_STOP_NOTIFY: {
                        ClearNotification();
                        mNotificationsStarted = false;
                        break;
                    }
                }

                return true;
            }
        });
    }

    private void setupBundles() {
        mBundles = new Bundle[MAX_TRACKS];
        for (int i=0; i<MAX_TRACKS; i++) {
            mBundles[i] = new Bundle();
        }
    }

    private void ExtractBundle(Bundle bundle) {
        int position = bundle.getInt(TRACK_BUNDLE_POSITION);

        if ((bundle != null) && (position >= 0) && (position < MAX_TRACKS)) {
            mBundles[position].putInt(TRACK_BUNDLE_POSITION, position);
            mBundles[position].putString(TRACK_BUNDLE_ID, bundle.getString(TRACK_BUNDLE_ID));
            mBundles[position].putString(TRACK_BUNDLE_ARTIST, bundle.getString(TRACK_BUNDLE_ARTIST));
            mBundles[position].putString(TRACK_BUNDLE_ALBUM, bundle.getString(TRACK_BUNDLE_ALBUM));
            mBundles[position].putString(TRACK_BUNDLE_TRACK, bundle.getString(TRACK_BUNDLE_TRACK));
            mBundles[position].putString(TRACK_BUNDLE_PREVIEW, bundle.getString(TRACK_BUNDLE_PREVIEW));
            mBundles[position].putString(TRACK_BUNDLE_IMAGE, bundle.getString(TRACK_BUNDLE_IMAGE));
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(SERVICE_TAG, "onStartCommand first time = " + !serviceStarted);

        if (serviceStarted) {
            if (ServiceController.serviceControllerMsgHandler != null) {
                Message newMsg = new Message();
                newMsg.what = ServiceController.MSG_MEDIA_SERVICE_RUNNING;
                newMsg.setData(mBundles[mCurrentTrack]);

                ServiceController.serviceControllerMsgHandler.sendMessage(newMsg);
            }
        }
        else {
            SendMediaPlayerFragmentMessage(ServiceController.MSG_MEDIA_SERVICE_STARTED);
            serviceStarted = true;
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        mMediaController.destroy();
        Log.v(SERVICE_TAG, "onDestroy");

        this.unregisterReceiver(this.receiver);
        ClearNotification();

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.v(SERVICE_TAG, "onBind");
        return null;
    }

    private void SendMediaPlayerFragmentMessage(int msg) {
        if (ServiceController.serviceControllerMsgHandler != null) {
            ServiceController.serviceControllerMsgHandler.sendEmptyMessage(msg);
        }
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(SERVICE_TAG, "onReceive");
            if (intent != null) {
                String action = intent.getAction();
                if (action != null) {
                    Log.d(SERVICE_TAG, "onReceive action = " + action);

                    if (action == "Play") {
                        Log.v(SERVICE_TAG, "onReceive PLAY");
                        mMediaController.play();
                        UpdateNotification();
                    } else if (action == "Pause") {
                        Log.v(SERVICE_TAG, "onReceive PAUSE");
                        mMediaController.pause();
                        UpdateNotification();
                    } else if (action == "Previous") {
                        Log.v(SERVICE_TAG, "onReceive PREVIOUS");
                        onDecrementTrack();
                    } else if (action == "Next") {
                        Log.v(SERVICE_TAG, "onReceive NEXT");
                        onIncrementTrack();
                    }
                }
            }
        }
    };

    private void onIncrementTrack() {
        if (++mCurrentTrack >= MAX_TRACKS) {
            mCurrentTrack = 0;
        }

        UpdateNotification();
        String url = mBundles[mCurrentTrack].getString(TRACK_BUNDLE_PREVIEW);
        mMediaController.setUrl(url);
        mMediaController.reset();
        sendTrackInfo();
    }

    private void onDecrementTrack() {
        if (--mCurrentTrack < 0) {
            mCurrentTrack = MAX_TRACKS-1;
        }

        UpdateNotification();
        String url = mBundles[mCurrentTrack].getString(TRACK_BUNDLE_PREVIEW);
        mMediaController.setUrl(url);
        mMediaController.reset();
        sendTrackInfo();
    }

    private void setupReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BROADCAST_ACTION_PLAY);
        filter.addAction(BROADCAST_ACTION_PAUSE);
        filter.addAction(BROADCAST_ACTION_PREV);
        filter.addAction(BROADCAST_ACTION_NEXT);

        this.registerReceiver(this.receiver, filter);
    }

    private void UpdateNotification() {
        Log.d(SERVICE_TAG, "UpdateNotification mCurrentTrack = " + mCurrentTrack);

        if (mNotificationsStarted) {
            if (mAllowNotifications) {
                if ((mCurrentTrack < MAX_TRACKS) && (mBundles[mCurrentTrack] != null)) {
                    if (mediaNotification ==  null) {
                        mediaNotification = new MediaNotification(this);
                    }

                    mediaNotification.ShowNotification(mMediaController.getPlayState(), mBundles[mCurrentTrack]);
                }
            }
            else {
                ClearNotification();
            }
        }

        sendShortMsg(ServiceController.MSG_PLAY_STATE, mMediaController.getPlayState());
    }

    private void ClearNotification() {
        Log.d(SERVICE_TAG, "ClearNotification");

        if (mediaNotification != null) {
            mediaNotification.CancelNotification();
        }
    }

    private void ReadPreferences() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        mAllowNotifications = sharedPref.getBoolean(getString(R.string.notification_preference), DEFAULT_NOTIFICATION_SETTING);

        Log.d(SERVICE_TAG, "ReadPreferences notificationCheck = " +mAllowNotifications);

        UpdateNotification();
    }

    private void sendTrackInfo() {
        Message newMessage = new Message();
        newMessage.what = ServiceController.MSG_TRACK_INFO;
        newMessage.setData(mBundles[mCurrentTrack]);

        if (ServiceController.serviceControllerMsgHandler != null) {
            ServiceController.serviceControllerMsgHandler.sendMessage(newMessage);
        }
    }

    private void sendShortMsg(int msgID, int value) {
        Message newMessage = new Message();
        newMessage.what = msgID;
        newMessage.arg1 = value;

        if (ServiceController.serviceControllerMsgHandler != null) {
            ServiceController.serviceControllerMsgHandler.sendMessage(newMessage);
        }
    }
}
