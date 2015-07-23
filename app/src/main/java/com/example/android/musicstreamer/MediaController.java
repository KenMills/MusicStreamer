package com.example.android.musicstreamer;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;

/**
 * Created by kenm on 6/29/2015.
 */
public class MediaController {
    private final int PLAY_STATE_IDLE    = 0;
    private final int PLAY_STATE_PLAYING = 1;
    private final int PLAY_STATE_PAUSED  = 2;

    private int playState = PLAY_STATE_IDLE;

    private String mUrl;// = "https://p.scdn.co/mp3-preview/64684256e5ec0b4148f5f41ddc087ee636ebdb0a";
    MediaPlayer mMediaPlayer = null;
    Context mAppContext = null;

    private static final int UPDATE_DELAY = 200;
    private final Handler handler = new Handler();

    public MediaController(Context context) {
        this.mAppContext = context;
        init();
    }

    public void setUrl(String mUrl) {
        Log.d("MediaController", "setUrl mUrl = " + mUrl);
        this.mUrl = mUrl;
    }

    public int getPlayState() {
        return playState;
    }

    public int getDuration() {
        return mMediaPlayer.getDuration();
    }

    public int getCurentPosition() {
        return mMediaPlayer.getCurrentPosition();
    }

    public void setCurentPosition(int curentPosition) {
        mMediaPlayer.seekTo(curentPosition);
    }

    public void init() {
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setScreenOnWhilePlaying(true);

        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                playState = PLAY_STATE_IDLE;
                MediaService.mediaServiceMsgHandler.sendEmptyMessage(MediaService.MSG_MEDIA_PREPPED);
                sendMax();
            }
        });

        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if (mp.getCurrentPosition() > 0) {
                    playState = PLAY_STATE_IDLE;
                    MediaService.mediaServiceMsgHandler.sendEmptyMessage(MediaService.MSG_PLAY_COMPLETE);
                }
            }
        });
    }

    public void Prep() {
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        try {
            Log.d("MediaController", "Prep mUrl = " + mUrl);

            mMediaPlayer.setDataSource(mUrl);
            mMediaPlayer.prepareAsync();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void destroy() {
        mMediaPlayer.release();
        mMediaPlayer = null;
    }

    public void play() {
        if (playState != PLAY_STATE_PLAYING) {
            playState = PLAY_STATE_PLAYING;
            mMediaPlayer.start();
            checkPlayProgress();

            MediaPlayerFragment.mediaPlayerMsgHandler.sendEmptyMessage(MediaPlayerFragment.MSG_PLAY_STARTED);
        }
    }

    public void pause() {
        if (playState == PLAY_STATE_PLAYING) {
            mMediaPlayer.pause();
            playState = PLAY_STATE_PAUSED;
        }
    }

    public void reset() {
        mMediaPlayer.reset();
        Prep();
    }

    private void sendMax() {
        Message newMsg = new Message();
        newMsg.what = MediaPlayerFragment.MSG_MEDIA_GET_MAX;
        newMsg.arg1 = mMediaPlayer.getDuration();
        MediaPlayerFragment.mediaPlayerMsgHandler.sendMessage(newMsg);
    }

    private void sendPosition() {
        Message newMsg = new Message();
        newMsg.what = MediaPlayerFragment.MSG_MEDIA_GET_POS;
        newMsg.arg1 = mMediaPlayer.getCurrentPosition();
        newMsg.arg2 = mMediaPlayer.getDuration();
        MediaPlayerFragment.mediaPlayerMsgHandler.sendMessage(newMsg);
    }

    private void checkPlayProgress() {
//        Log.d("MediaController", "checkPlayProgress isPlaying = " + isPlaying());

        if (playState == PLAY_STATE_PLAYING) {
            sendPosition();

            Runnable notification = new Runnable() {
                public void run() {
                    checkPlayProgress();
                }
            };

            // keep updating while playing
            handler.postDelayed(notification, UPDATE_DELAY);
        }
    }
}
