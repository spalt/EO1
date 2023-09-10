package com.dan.eo1partner;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
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
    public boolean autobrightness = true;
    public float brightnesslevel = 0.5f;
    public int interval = 5;
    private int startQuietHour = -1;
    private int endQuietHour = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtMsg = findViewById(R.id.txtmsg);

        SharedPreferences settings = getSharedPreferences("prefs", MODE_PRIVATE);
        ipaddress = settings.getString("ipaddress", "");
        autobrightness = settings.getBoolean("autobrightness", true);
        brightnesslevel = settings.getFloat("brightnesslevel", 0.5f);
        interval = settings.getInt("interval", 5);
        startQuietHour = settings.getInt("startQuietHour", -1);
        endQuietHour = settings.getInt("endQuietHour", -1);

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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        View customLayout = getLayoutInflater().inflate(R.layout.options, null);
        final SeekBar sbBrightness = customLayout.findViewById(R.id.sbBrightness);
        final CheckBox cbAutoBrightness = customLayout.findViewById(R.id.cbBrightnessAuto);
        final EditText editTextInterval = customLayout.findViewById(R.id.editTextInterval);
        final Spinner startHourSpinner = customLayout.findViewById(R.id.startHourSpinner);
        final Spinner endHourSpinner = customLayout.findViewById(R.id.endHourSpinner);

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

        cbAutoBrightness.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b)
                    sbBrightness.setVisibility(View.GONE);
                else
                    sbBrightness.setVisibility(View.VISIBLE);
                autobrightness = b;
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Options")
                .setView(customLayout)
                .setCancelable(true)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        interval = Integer.parseInt(editTextInterval.getText().toString().trim());
                        brightnesslevel = ((float) sbBrightness.getProgress()) / 10;
                        startQuietHour = Integer.parseInt(startHourSpinner.getSelectedItem().toString());
                        endQuietHour = Integer.parseInt(endHourSpinner.getSelectedItem().toString());

                        if (cbAutoBrightness.isChecked()) brightnesslevel = -1;

                        sendMessage("options," + brightnesslevel + "," + interval + "," + startQuietHour + "," + endQuietHour);

                        SharedPreferences settings = getSharedPreferences("prefs", MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putBoolean("autobrightness", autobrightness);
                        editor.putFloat("brightnesslevel", brightnesslevel);
                        editor.putInt("startQuietHour", startQuietHour);
                        editor.putInt("endQuietHour", endQuietHour);
                        editor.putInt("interval", interval);
                        editor.apply();
                    }
                });

        builder.create();
        builder.show();

        if (autobrightness) {
            cbAutoBrightness.setChecked(true);
            sbBrightness.setVisibility(View.GONE);
        } else {
            cbAutoBrightness.setChecked(false);
            sbBrightness.setVisibility(View.VISIBLE);
        }

        sbBrightness.setProgress((int) (brightnesslevel * 10));

        editTextInterval.setText(String.valueOf(interval));

        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
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