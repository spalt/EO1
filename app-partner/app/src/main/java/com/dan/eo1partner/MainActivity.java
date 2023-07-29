package com.dan.eo1partner;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MainActivity extends AppCompatActivity {

    public TextView txtMsg;
    public String ipaddress = "";
    public boolean intenthandled = false;
    public String sharedText = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtMsg = findViewById(R.id.txtmsg);

        SharedPreferences settings = getSharedPreferences("prefs", MODE_PRIVATE);
        ipaddress = settings.getString("ipaddress", "");

        if (ipaddress.equals("")) {
            final EditText input = new EditText(MainActivity.this);
            input.setInputType(InputType.TYPE_CLASS_PHONE);

            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Enter IP Address of EO1 Device:")
                    .setView(input)
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ipaddress = input.getText().toString();

                            SharedPreferences.Editor editor = settings.edit();
                            editor.putString("ipaddress", ipaddress);
                            editor.apply();

                            handleintent();
                        }
                    });
            builder.create();
            builder.show();
        } else {
            handleintent();
        }

        if (!ipaddress.equals("")) {
            if (!intenthandled) {
                //show ui
                txtMsg.setVisibility(View.INVISIBLE);

                Button btnResume = findViewById(R.id.btnResume);
                btnResume.setVisibility(View.VISIBLE);
                btnResume.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        sendMessage("resume,");
                    }
                });

                Button btnUpdateTag = findViewById(R.id.btnUpdateTag);
                btnUpdateTag.setVisibility(View.VISIBLE);
                btnUpdateTag.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        final EditText input = new EditText(MainActivity.this);
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setTitle("Enter tag:")
                                .setView(input)
                                .setCancelable(false)
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        sendMessage("tag," + input.getText().toString());
                                    }
                                });
                        builder.create();
                        builder.show();
                    }
                });

            }
        }

    }

    private void handleintent() {
        if (Intent.ACTION_SEND.equals(getIntent().getAction()) && getIntent().getType() != null) {
            txtMsg.setText("Sending...");
            if (getIntent().getType().startsWith("image/")) {
                //image
                Uri imageUri = (Uri) getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
                if (imageUri != null) {
                    sendMessage("image," + imageUri.toString());
                }
            }
            if (getIntent().getType().startsWith("text/plain")) {
                //video or image
                sharedText = getIntent().getStringExtra(Intent.EXTRA_TEXT);

                int a = sharedText.indexOf("Check it out:");
                String url = sharedText.substring(a + "Check it out:".length()).trim();
                new GetFinalURLTask().execute(url);
            }
            intenthandled = true;
        }
    }

    private class GetFinalURLTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {
            String shortUrl = urls[0];
            String finalUrl = null;

            OkHttpClient client = new OkHttpClient();

            Request request = new Request.Builder()
                    .url(shortUrl)
                    .build();
            try {
                Response response = client.newCall(request).execute();
                String res = response.toString();
                finalUrl = res.substring(res.indexOf("url=")+("url=".length()), res.length()-1);
                response.close();
                return finalUrl;
            } catch (IOException e) {
                e.printStackTrace();
                return "";
             }
        }

        @Override
        protected void onPostExecute(String finalUrl) {
            if (finalUrl.equals(""))
                txtMsg.setText("Error");
            else
                new GetVideoURL().execute(finalUrl);
        }
    }

    private class GetVideoURL extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... urls) {
            String shortUrl = urls[0];

            String finalUrl = "";

            OkHttpClient client = new OkHttpClient();

            Request request = new Request.Builder()
                    .url(shortUrl)
                    .build();

            try {
                Response response = client.newCall(request).execute();

                ResponseBody responseBody = response.body();
                String body = responseBody.string();

                //assume video
                int a = body.indexOf("\"secret\":\"");
                int b = body.indexOf("\"", a + "\"secret\":\"".length());
                String finalurl = shortUrl + "play/720p/" + body.substring(a + "\"secret\":\"".length(), b);

                if (finalUrl.trim().equals("")) {
                    //a photo
                    a = body.indexOf("og:image\" content=\"");
                    b = body.indexOf("\"", a + "og:image\" content=\"".length());
                    finalUrl = body.substring(a + "og:image\" content=\"".length(), b);

                    sendMessage("image," + finalUrl);
                } else {
                    sendMessage("video," + finalurl);
                }

                response.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        protected void onPostExecute(String res) {
        }
    }

    protected void sendMessage(String msg) {
        CustomSendMessageTask sendMessageTask = new CustomSendMessageTask(ipaddress);
        sendMessageTask.execute(msg);
    }

    public class CustomSendMessageTask extends SendMessageTask {
        public CustomSendMessageTask(String ipAddress) {
            super(ipAddress);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                if (txtMsg.getVisibility() != View.INVISIBLE)
                    txtMsg.setText("Sent!");
                else
                    Toast.makeText(MainActivity.this, "Message sent successfully!", Toast.LENGTH_SHORT).show();
            } else {
                if (txtMsg.getVisibility() != View.INVISIBLE)
                    txtMsg.setText("Sending Failed");
                else
                    Toast.makeText(MainActivity.this, "Failed to send the message.", Toast.LENGTH_SHORT).show();

            }
        }
    }

}