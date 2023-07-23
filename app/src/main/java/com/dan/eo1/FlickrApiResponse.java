package com.dan.eo1;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class FlickrApiResponse {
    @SerializedName("photos")
    private FlickrPhotos photos;

    public FlickrPhotos getPhotos() {
        return photos;
    }
}

class FlickrPhotos {
    @SerializedName("photo")
    private List<FlickrPhoto> photoList;

    public List<FlickrPhoto> getPhotoList() {
        return photoList;
    }
}

class FlickrPhoto {
    @SerializedName("id")
    private String id;

    @SerializedName("secret")
    private String secret;

    @SerializedName("server")
    private String server;

    @SerializedName("farm")
    private int farm;

    @SerializedName("media")
    private String media;

    @SerializedName("url_o")
    private String url_o;


    public String getId() {
        return id;
    }

    public String getSecret() {
        return secret;
    }

    public String getServer() {
        return server;
    }

    public int getFarm() {
        return farm;
    }

    public String getMedia() {
        return media;
    }

    public String getUrlO() {
        return url_o;
    }
}