package com.dan.eo1;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface FlickrApiService {

    @GET("?method=flickr.photos.search&format=json&nojsoncallback=1")
    Call<FlickrApiResponse> searchPhotos(
            @Query("api_key") String apiKey,
            @Query("text") String searchText,
            @Query("per_page") int perPage
    );

    @GET("?method=flickr.people.getPublicPhotos&format=json&nojsoncallback=1")
    Call<FlickrApiResponse> getPublicPhotos(
            @Query("api_key") String apiKey,
            @Query("user_id") String userId,
            @Query("per_page") int perPage,
            @Query("extras") String extras
    );

}