package com.dan.eo1partner;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;

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

            sharedText = getIntent().getStringExtra(Intent.EXTRA_TEXT);

            int a = sharedText.indexOf("Check it out:");
            String url = sharedText.substring(a + "Check it out:".length()).trim();
            new GetFinalURLTask().execute(url);

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
                okhttp3.Response response = client.newCall(request).execute();
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
            else {
                String imageid = finalUrl.substring(finalUrl.lastIndexOf('/', finalUrl.lastIndexOf('/') - 1) + 1, finalUrl.length() - 1);
                if (sharedText.contains("a video"))
                    sendMessage("video," + imageid);
                else
                    sendMessage("image," + imageid);
            }
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