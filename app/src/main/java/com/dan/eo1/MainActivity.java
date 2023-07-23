package com.dan.eo1;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.squareup.picasso.Picasso;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.util.Calendar;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    public int interval = 5;
    public int tempid = 0;
    private int currentPosition = 0;
    private Handler handler = new Handler();
    private ImageView imageView;
    private VideoView videoView;
    private String apikey = "";
    private String userid = "";
    private int displayOption = 0;
    private int startQuietHour = -1;
    private int endQuietHour = -1;
    private List<FlickrPhoto> flickrPhotos;
    private boolean isInQuietHours = false;
    private String customTag = "";
    private SensorManager mSensorManager;
    private Sensor mLightSensor;
    private float lastLightLevel;
    private int currentScreenBrightness;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

       // Optionally, you can also clear the disk cache
        File cacheDir = new File(getCacheDir(), "picasso-cache");
        if (cacheDir.exists() && cacheDir.isDirectory()) {
            for (File file : cacheDir.listFiles()) {
                file.delete();
            }
        }

        SharedPreferences settings = getSharedPreferences("prefs", MODE_PRIVATE);
        userid = settings.getString("userid", "");
        apikey = settings.getString("apikey", "");
        displayOption = settings.getInt("displayOption", 0);
        startQuietHour = settings.getInt("startQuietHour", -1);
        endQuietHour = settings.getInt("endQuietHour", -1);
        customTag = settings.getString("customTag", "");
        interval = settings.getInt("interval", 5);

        if (displayOption != 2) customTag = "";

        if (userid.isEmpty() || apikey.isEmpty()) {
            showSetupDialog();
        }

        imageView = findViewById(R.id.imageView);
        videoView = findViewById(R.id.videoView);

        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        SensorEventListener listener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (Math.abs(event.values[0] - lastLightLevel) >= 5.0f) {
                    adjustScreenBrightness(event.values[0]);
                }
            }
            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {
            }
        };
        Log.e("hi", "registered light sensor");
        mSensorManager.registerListener(listener, mLightSensor, SensorManager.SENSOR_DELAY_UI);
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

    boolean screenon = true;

    @SuppressLint("InvalidWakeLockTag")
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_C) {
            showSetupDialog();
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_A) {
            Toast.makeText(MainActivity.this, "sensor = " + lastLightLevel, Toast.LENGTH_SHORT).show();
        }

        if (keyCode == KeyEvent.KEYCODE_SPACE) {
            showNextImage();
        }

        if (keyCode == 132) {
            //top button pushed
            WindowManager.LayoutParams params = getWindow().getAttributes();
            if (screenon) {
                params.screenBrightness = 0;
                screenon = false;
            } else {
                params.screenBrightness = 10;
                screenon = true;
            }
            getWindow().setAttributes(params);
        }

        return super.onKeyDown(keyCode, event);
    }

    private void showSetupDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View customLayout = getLayoutInflater().inflate(R.layout.options, null);
        builder.setView(customLayout);

        final EditText userIdEditText = customLayout.findViewById(R.id.editTextUserId);
        final EditText apiKeyEditText = customLayout.findViewById(R.id.editTextApiKey);
        final Spinner startHourSpinner = customLayout.findViewById(R.id.startHourSpinner);
        final Spinner endHourSpinner = customLayout.findViewById(R.id.endHourSpinner);
        final Button btnLoadConfig = customLayout.findViewById(R.id.btnLoadConfig);
        final EditText editTextCustomTag = customLayout.findViewById(R.id.editTextCustomTag);
        final EditText editTextInterval = customLayout.findViewById(R.id.editTextInterval);

        userIdEditText.setText(userid);
        apiKeyEditText.setText(apikey);
        editTextCustomTag.setText(customTag);
        editTextInterval.setText(String.valueOf(interval));

        RadioGroup optionsRadioGroup = customLayout.findViewById(R.id.optionsRadioGroup);
        optionsRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // Check if the selected option is the last one ("Show photos tagged with...")
                if (checkedId == R.id.radioOption3) {
                    editTextCustomTag.setVisibility(View.VISIBLE);
                    editTextCustomTag.setText(customTag);
                } else {
                    editTextCustomTag.setVisibility(View.GONE);
                    customTag = ""; // Clear the customTag when not applicable
                }
            }
        });

        editTextCustomTag.setVisibility(View.GONE);
        if (displayOption == 0) ((RadioButton) customLayout.findViewById(R.id.radioOption1)).setChecked(true);
        if (displayOption == 1) ((RadioButton) customLayout.findViewById(R.id.radioOption2)).setChecked(true);
        if (displayOption == 2) {
            ((RadioButton) customLayout.findViewById(R.id.radioOption3)).setChecked(true);
            editTextCustomTag.setVisibility(View.VISIBLE);
            editTextCustomTag.setText(customTag);
        }

        // Set up the Spinners for start and end hour
        String[] hours = new String[24];
        for (int i = 0; i < 24; i++) {
            hours[i] = String.format("%02d", i);
        }
        ArrayAdapter<String> hourAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, hours);
        hourAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        startHourSpinner.setAdapter(hourAdapter);
        if (startQuietHour != -1) startHourSpinner.setSelection(startQuietHour);
        endHourSpinner.setAdapter(hourAdapter);
        if (endQuietHour != -1) endHourSpinner.setSelection(endQuietHour);

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

        builder.setTitle("Setup")
                .setCancelable(false)
                .setView(customLayout)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        userid = userIdEditText.getText().toString().trim();
                        apikey = apiKeyEditText.getText().toString().trim();
                        //displayOption = optionsSpinner.getSelectedItemPosition();
                        displayOption = getSelectedOptionIndex(optionsRadioGroup);
                        startQuietHour = Integer.parseInt(startHourSpinner.getSelectedItem().toString());
                        endQuietHour = Integer.parseInt(endHourSpinner.getSelectedItem().toString());
                        customTag = editTextCustomTag.getText().toString().trim();
                        interval = Integer.parseInt(editTextInterval.getText().toString().trim());

                        if (!userid.isEmpty() && !apikey.isEmpty()) {
                            SharedPreferences settings = getSharedPreferences("prefs", MODE_PRIVATE);
                            SharedPreferences.Editor editor = settings.edit();
                            editor.putString("userid", userid);
                            editor.putString("apikey", apikey);
                            editor.putInt("displayOption", displayOption);
                            editor.putInt("startQuietHour", startQuietHour);
                            editor.putInt("endQuietHour", endQuietHour);
                            editor.putString("customTag", customTag);
                            editor.putInt("interval", interval);
                            editor.apply();

                            Toast.makeText(MainActivity.this, "Saved!  Hit 'C' to come back here later.", Toast.LENGTH_SHORT).show();

                            loadImagesFromFlickr();
                            startSlideshow();
                        } else {
                            Toast.makeText(MainActivity.this, "Please enter User ID and API Key", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        builder.show();
    }


    private void startSlideshow() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Calendar calendar = Calendar.getInstance();
                int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
                if (currentHour >= startQuietHour && currentHour <= endQuietHour) {
                    if (!isInQuietHours) {
                        //entering quiet, turn off screen
                        WindowManager.LayoutParams params = getWindow().getAttributes();
                        params.screenBrightness = 0;
                        getWindow().setAttributes(params);
                        videoView.setVisibility(View.GONE);
                        imageView.setVisibility(View.GONE);

                        isInQuietHours = true;
                    }
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            startSlideshow();
                        }
                    }, 60000);
                } else {
                    if (isInQuietHours) {
                        //exiting quiet, turn on screen
                        WindowManager.LayoutParams params = getWindow().getAttributes();
                        params.screenBrightness = 1f;
                        getWindow().setAttributes(params);

                        isInQuietHours = false;
                    }
                    showNextImage();
                    handler.postDelayed(this, 60000 * interval);
                }
            }
        }, 60000 * interval);
    }

    private void showNextImage() {
        if (flickrPhotos != null && !flickrPhotos.isEmpty()) {
            if (currentPosition >= flickrPhotos.size()) {
                loadImagesFromFlickr(); return;
            }

            if (!flickrPhotos.get(currentPosition).getMedia().equals("video")) {
                videoView.setVisibility(View.INVISIBLE);
                imageView.setVisibility(View.VISIBLE);
                String imageUrl = flickrPhotos.get(currentPosition).getUrlO().toString().replace("_o","_k");
                Picasso.get().load(imageUrl).fit().into(imageView); //Picasso.get().load(imageUrl).fit().centerCrop().into(imageView);

            } else {
                imageView.setVisibility(View.INVISIBLE);
                videoView.setVisibility(View.VISIBLE);

                MediaController mediaController = new MediaController(this);
                mediaController.setAnchorView(videoView);
                mediaController.setVisibility(View.INVISIBLE);
                videoView.setMediaController(mediaController);

                String url = "https://www.flickr.com/photos/" + userid + "/" + flickrPhotos.get(currentPosition).getId() + "/play/720p/" + flickrPhotos.get(currentPosition).getSecret();

                //videoView.setVideoURI(Uri.parse(url));
                new DownloadVideoTask().execute(url);
//                videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
//                    @Override
//                    public void onPrepared(MediaPlayer mediaPlayer) {
//                        mediaPlayer.setLooping(true);
//                        videoView.start();
//                    }
//                });
//                videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
//                    @Override
//                    public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
//                        showNextImage();
//                        return true;
//                    }
//                });

            }
            currentPosition++;
        }
    }

    private void loadImagesFromFlickr() {
        Toast.makeText(MainActivity.this, "Load images!", Toast.LENGTH_SHORT).show();

        if (isNetworkAvailable()) {
            Toast.makeText(MainActivity.this, "IP = " + getIPAddress(), Toast.LENGTH_LONG).show();

            Intent serviceIntent = new Intent(this, MyMessageService.class);
            startService(serviceIntent);

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("https://api.flickr.com/services/rest/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(new okhttp3.OkHttpClient())
                    .build();

            FlickrApiService apiService = retrofit.create(FlickrApiService.class);
            Call<FlickrApiResponse> call;

            if (displayOption == 0)
                call = apiService.getPublicPhotos(apikey, userid, 500, "media,url_o");
            else
                call = apiService.searchPhotos(apikey, "", 500, (customTag.equals("") ? "electricobjectslives": customTag), "media,url_o");
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
            }, 10000);
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private View getLabel(String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        return textView;
    }

    private void adjustScreenBrightness(float lightValue){
        if (!isInQuietHours) {
            // Determine the desired brightness range
            float maxBrightness = 1.0f; // Maximum brightness value (0 to 1)
            float minBrightness = 0.0f; // Minimum brightness value (0 to 1)

            // Map the light sensor value (0 to 25) to the desired brightness range (0 to 1)
            float brightness = (lightValue / 25.0f) * (maxBrightness - minBrightness) + minBrightness;

            // Make sure brightness is within the valid range
            brightness = Math.min(Math.max(brightness, minBrightness), maxBrightness);

            // Apply the brightness setting to the screen
            WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
            layoutParams.screenBrightness = brightness;
            getWindow().setAttributes(layoutParams);

            lastLightLevel = lightValue;
        }
    }

    private int getSelectedOptionIndex(RadioGroup radioGroup) {
        int checkedRadioButtonId = radioGroup.getCheckedRadioButtonId();
        View checkedRadioButton = radioGroup.findViewById(checkedRadioButtonId);
        return radioGroup.indexOfChild(checkedRadioButton);
    }

    private String getIPAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (!address.isLoopbackAddress() && address.getAddress().length == 4) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return null;
    }

    private class DownloadVideoTask extends AsyncTask<String, Void, File> {

        @Override
        protected File doInBackground(String... params) {
            String videoUrl = params[0];
            try {
                // Download the video from the URL
                URL url = new URL(videoUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                File tempFile = new File(getCacheDir(), "temp" + tempid + ".mp4");
                FileOutputStream outputStream = new FileOutputStream(tempFile);

                tempid++;
                if (tempid == 5) tempid=0;

                InputStream inputStream = connection.getInputStream();
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.close();
                inputStream.close();

                return tempFile;
            } catch (IOException e) {
                Toast.makeText(MainActivity.this, "ERR> " + e.getMessage(), Toast.LENGTH_SHORT).show();
                showNextImage();
                return null;
            }
        }

        @Override
        protected void onPostExecute(File file) {
            if (file != null) {
                videoView.setVideoPath(file.getPath());
                videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mediaPlayer) {
                        mediaPlayer.setLooping(true);
                        videoView.start();
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
        }
    }



}