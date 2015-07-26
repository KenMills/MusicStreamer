package com.example.android.musicstreamer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.squareup.picasso.Picasso;

/**
 * Created by kenm on 7/17/2015.
 */
public class MediaNotification {
    private static final String LOG_TAG = "MediaNotification";
    private Context mContext;

    // used for bundling tracks
    public static final String POSITION = "POSITION";
    public static final String ID = "ID";
    public static final String ARTIST = "ARTIST";
    public static final String ALBUM = "ALBUM";
    public static final String TRACK = "TRACK";
    public static final String PREVIEW = "PREVIEW";
    public static final String IMAGE = "IMAGE";

    private final int PLAY_STATE_IDLE    = 0;
    private final int PLAY_STATE_PLAYING = 1;
    private final int PLAY_STATE_PAUSED  = 2;

    private final int NOTIFICATION_ID  = 0;

    private String mArtistName;
    private String mAlbumName;
    private String mTrackName;
    private String mImageUrl;
    private int mPlayState;

    public MediaNotification(Context context) {
        this.mContext = context;
    }

    public void ShowNotification(int playState, Bundle bundle) {
        Log.v(LOG_TAG, "ShowNotification");

        mPlayState = playState;
        if (bundle != null) {
            mArtistName = bundle.getString(ARTIST);
            mAlbumName = bundle.getString(ALBUM);
            mTrackName = bundle.getString(TRACK);
            mImageUrl = bundle.getString(IMAGE);

            if (mImageUrl != null) {
                GetImageTask imageTask = new GetImageTask();
                imageTask.execute(mImageUrl);
            }
            else {
                Notification.Action playPauseAction;
                if ((playState == PLAY_STATE_PAUSED) || (playState == PLAY_STATE_IDLE)) {
                    playPauseAction = generateAction(android.R.drawable.ic_media_play, "", "Play");
                }
                else {
                    playPauseAction = generateAction(android.R.drawable.ic_media_pause, "", "Pause");
                }

                PendingIntent pi = PendingIntent.getBroadcast(mContext, 0, new Intent("ExitNotification"), PendingIntent.FLAG_UPDATE_CURRENT);

                Notification notification = new Notification.Builder(mContext)
                        .setTicker(mArtistName)
                        .setContentTitle(mArtistName)
                        .setContentText(mAlbumName + ":" + mTrackName)
                        .setContentIntent(pi)
                        .setAutoCancel(false)
                        .setOngoing(true)
                        .setSmallIcon(R.drawable.ic_play_circle)
                        .addAction(generateAction(android.R.drawable.ic_media_previous, "", "Previous"))
                        .addAction(playPauseAction)
                        .addAction(generateAction(android.R.drawable.ic_media_next, "", "Next"))
                        .build();

                NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(mContext.NOTIFICATION_SERVICE);
                notificationManager.notify(NOTIFICATION_ID, notification);
            }
        }
        else {
            Log.v(LOG_TAG, "ShowNotification empty bundle...");
        }

    }

    public void CancelNotification() {
        NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(mContext.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
    }

    private Notification.Action generateAction( int icon, String title, String intentAction ) {
        Intent intent = new Intent(intentAction);
        PendingIntent pi = PendingIntent.getBroadcast(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        return new Notification.Action.Builder( icon, title, pi ).build();
    }

    public class GetImageTask extends AsyncTask<String, Void, Bitmap> {
        private Exception savedException = null;

        @Override
        protected Bitmap doInBackground(String... params) {
            if (params.length == 0) {
                return null;
            }

            Bitmap bitmap = null;
            String imageUrl = params[0];
            savedException = null;
            try {
                bitmap = Picasso.with(mContext).load(imageUrl).get();
            } catch (Exception e) {
                Log.d(LOG_TAG, "GetImageTask()", e);
                savedException = e;
            }

            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);

            Notification.Action playPauseAction;
            if ((mPlayState == PLAY_STATE_PAUSED) || (mPlayState == PLAY_STATE_IDLE)) {
                playPauseAction = generateAction(android.R.drawable.ic_media_play, "", "Play");
            }
            else {
                playPauseAction = generateAction(android.R.drawable.ic_media_pause, "", "Pause");
            }

            PendingIntent pi = PendingIntent.getBroadcast(mContext, 0, new Intent("ExitNotification"), PendingIntent.FLAG_UPDATE_CURRENT);

            Notification notification = new Notification.Builder(mContext)
                    .setTicker(mArtistName)
                    .setContentTitle(mArtistName)
                    .setContentText(mAlbumName + ":" + mTrackName)
                    .setContentIntent(pi)
                    .setAutoCancel(false)
                    .setOngoing(true)
                    .setSmallIcon(R.drawable.ic_play_circle)
                    .setLargeIcon(bitmap)
                    .addAction(generateAction(android.R.drawable.ic_media_previous, "", "Previous"))
                    .addAction(playPauseAction)
                    .addAction(generateAction(android.R.drawable.ic_media_next, "", "Next"))
                    .build();

            NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(mContext.NOTIFICATION_SERVICE);
            notificationManager.notify(0, notification);

        }
    }

}
