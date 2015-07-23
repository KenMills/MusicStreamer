package com.example.android.musicstreamer;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by kenm on 6/19/2015.
 */
public class TopTenTracks implements Parcelable {
    private String spotifyID;
    private String artist;
    private String album;
    private String track;
    private String imageResource;
    private String previewUrl;

    public TopTenTracks(String spotifyID, String artist, String album, String track, String imageResource, String previewUrl) {
        this.spotifyID = spotifyID;
        this.artist = artist;
        this.album = album;
        this.track = track;
        this.imageResource = imageResource;
        this.previewUrl = previewUrl;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getSpotifyID() {
        return spotifyID;
    }
    public void setSpotifyID(String spotifyID) {
        this.spotifyID = spotifyID;
    }

    public String getImageResource() {
        return imageResource;
    }
    public void setImageResource(String imageResource) {
        this.imageResource = imageResource;
    }

    public String getAlbum() {
        return album;
    }
    public void setAlbum(String album) {
        this.album = album;
    }

    public String getTrack() {
        return track;
    }
    public void setTrack(String track) {
        this.track = track;
    }

    public String getPreviewUrl() {
        return previewUrl;
    }

    public void setPreviewUrl(String previewUrl) {
        this.previewUrl = previewUrl;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(spotifyID);
        dest.writeString(artist);
        dest.writeString(album);
        dest.writeString(track);
        dest.writeString(imageResource);
        dest.writeString(previewUrl);
    }

    public TopTenTracks(Parcel in) {
        this.spotifyID = in.readString();
        this.artist = in.readString();
        this.album = in.readString();
        this.track = in.readString();
        this.imageResource = in.readString();
        this.previewUrl = in.readString();
    }

    public static final Creator<TopTenTracks> CREATOR = new Creator<TopTenTracks>() {
        public TopTenTracks createFromParcel(Parcel in) {
            return new TopTenTracks(in);
        }

        public TopTenTracks[] newArray(int size) {
            return new TopTenTracks[size];
        }
    };
}
