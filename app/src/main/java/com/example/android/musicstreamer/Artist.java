package com.example.android.musicstreamer;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by kenm on 6/18/2015.
 */
public class Artist implements Parcelable {
    private String name;
    private String spotifyID;
    private String imageUrl;

    public Artist(String name, String spotifyID, String imageUrl) {
        this.name = name;
        this.spotifyID = spotifyID;
        this.imageUrl = imageUrl;
    }

    public Artist(Parcel in) {
        this.name = in.readString();
        this.spotifyID = in.readString();
        this.imageUrl = in.readString();
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getSpotifyID() {
        return spotifyID;
    }
    public void setSpotifyID(String spotifyID) {
        this.spotifyID = spotifyID;
    }

    public String getImageUrl() {
        return imageUrl;
    }
    public void setImageResource(String imageResource) {
        this.imageUrl = imageResource;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(spotifyID);
        dest.writeString(imageUrl);
    }

    public static final Creator<Artist> CREATOR = new Creator<Artist>() {
        public Artist createFromParcel(Parcel in) {
            return new Artist(in);
        }

        public Artist[] newArray(int size) {
            return new Artist[size];
        }
    };

}