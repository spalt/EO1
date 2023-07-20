package com.dan.eo1;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.squareup.picasso.Picasso;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private static final int SLIDESHOW_DELAY = 60000 * 5;
    private int currentPosition = 0;
    private Handler handler = new Handler();
    private ImageView imageView;
    private VideoView videoView;
    private String apikey = "";
    private String userid = "";
    private List<FlickrPhoto> flickrPhotos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences settings = getSharedPreferences("prefs", MODE_PRIVATE);
        userid = settings.getString("userid", "");
        apikey = settings.getString("apikey", "");

        if (userid.isEmpty() || apikey.isEmpty()) {
            showSetupDialog();
        }

        imageView = findViewById(R.id.imageView);
        videoView = findViewById(R.id.videoView);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!userid.isEmpty() && !apikey.isEmpty()) {
            loadImagesFromFlickr();
            startSlideshow();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_C) {
            showSetupDialog();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void showSetupDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Setup")
                .setMessage("Please enter your Flickr User ID and API Key")
                .setCancelable(false);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        final EditText userIdEditText = new EditText(this);
        if (!userid.equals("")) userIdEditText.setText(userid);
        userIdEditText.setHint("User ID");
        layout.addView(userIdEditText);

        final EditText apiKeyEditText = new EditText(this);
        if (!apikey.equals("")) apiKeyEditText.setText(apikey);
        apiKeyEditText.setHint("API Key");
        layout.addView(apiKeyEditText);

        final Button btnLoadConfig = new Button(this);
        btnLoadConfig.setText("Load settings from config.txt");
        layout.addView(btnLoadConfig);

        btnLoadConfig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                File downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
                File file = new File(downloadsDir, "config.txt");
                if (!file.exists()) {
                    Toast.makeText(MainActivity.this, "Can't find config.txt", Toast.LENGTH_SHORT).show();
                } else {
                    StringBuilder sb = new StringBuilder();
                    try {
                        BufferedReader br = new BufferedReader(new FileReader(file));
                        String line;
                        while ((line = br.readLine()) != null) {
                            sb.append(line).append('\n');
                        }
                        br.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    userIdEditText.setText(sb.toString().split("\n")[0]);
                    apiKeyEditText.setText(sb.toString().split("\n")[1]);
                }
            }
        });

        builder.setView(layout);

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                userid = userIdEditText.getText().toString().trim();
                apikey = apiKeyEditText.getText().toString().trim();

                if (!userid.isEmpty() && !apikey.isEmpty()) {
                    SharedPreferences settings = getSharedPreferences("prefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString("userid", userid);
                    editor.putString("apikey", apikey);
                    editor.apply();

                    Toast.makeText(MainActivity.this, "User ID and API Key saved", Toast.LENGTH_SHORT).show();

                    loadImagesFromFlickr();
                    startSlideshow();

                } else {
                    Toast.makeText(MainActivity.this, "Please enter User ID and API Key", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.show();
    }

    private void startSlideshow() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                showNextImage();
                handler.postDelayed(this, SLIDESHOW_DELAY);
            }
        }, SLIDESHOW_DELAY);
    }

    private void showNextImage() {
        if (flickrPhotos != null && !flickrPhotos.isEmpty()) {
            if (currentPosition >= flickrPhotos.size()) {
                currentPosition = 0;
            }

            if (!flickrPhotos.get(currentPosition).getMedia().equals("video")) {
                videoView.setVisibility(View.INVISIBLE);
                imageView.setVisibility(View.VISIBLE);
                String imageUrl = buildImageUrl(flickrPhotos.get(currentPosition));
                Picasso.get().load(imageUrl).fit().into(imageView); //Picasso.get().load(imageUrl).fit().centerCrop().into(imageView);

            } else {
                imageView.setVisibility(View.INVISIBLE);
                videoView.setVisibility(View.VISIBLE);
                Uri uri = Uri.parse("https://www.flickr.com/photos/" + userid + "/" + flickrPhotos.get(currentPosition).getId() + "/play/720p/" + flickrPhotos.get(currentPosition).getSecret());
                MediaController mediaController = new MediaController(this);
                mediaController.setAnchorView(videoView);
                mediaController.setVisibility(View.INVISIBLE);
                videoView.setMediaController(mediaController);
                videoView.setVideoURI(uri);
                videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mediaPlayer) {
                        mediaPlayer.setLooping(true);
                        videoView.start();
                        videoView.setVisibility(View.VISIBLE);
                    }
                });
                videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                    @Override
                    public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
                        showNextImage();
                        return true;
                    }
                });

            }
            currentPosition++;
        }
    }

    private void loadImagesFromFlickr() {
        Toast.makeText(MainActivity.this, "Load images", Toast.LENGTH_SHORT).show();

        if (isNetworkAvailable()) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("https://api.flickr.com/services/rest/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(new okhttp3.OkHttpClient())
                    .build();

            FlickrApiService apiService = retrofit.create(FlickrApiService.class);
            //Call<FlickrApiResponse> call = apiService.searchPhotos(apiKey, "flowers", 50);
            Call<FlickrApiResponse> call = apiService.getPublicPhotos(apikey, userid, 500, "media");
            call.enqueue(new Callback<FlickrApiResponse>() {
                @Override
                public void onResponse(Call<FlickrApiResponse> call, Response<FlickrApiResponse> response) {
                    if (response.isSuccessful()) {
                        FlickrApiResponse apiResponse = response.body();
                        if (apiResponse != null) {
                            flickrPhotos = apiResponse.getPhotos().getPhotoList();
                            Collections.shuffle(flickrPhotos);
                            showNextImage();
                        }
                    }
                }

                @Override
                public void onFailure(Call<FlickrApiResponse> call, Throwable t) {
                    Toast.makeText(MainActivity.this, "Flickr failure", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            new android.os.Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    loadImagesFromFlickr(); // Retry loading images after delay
                }
            }, 5000);
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private String buildImageUrl(FlickrPhoto photo) {
        return "https://farm" + photo.getFarm() +
                ".staticflickr.com/" + photo.getServer() +
                "/" + photo.getId() + "_" + photo.getSecret() + ".jpg";
    }

}