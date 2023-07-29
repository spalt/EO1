package com.dan.eo1;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class MyMessageService extends Service {
    private ServerSocket serverSocket;
    private boolean isRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        // Setup your server socket here
        try {
            int port = 12345; // Replace with the desired port number
            Log.e("hi", "oncreate socket");

            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isRunning) {
            isRunning = true;
            startSocketListener();
        }
        return START_STICKY;
    }

    private void startSocketListener() {
        // Create and start the background thread for listening to incoming messages
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (isRunning) {
                        Log.e("hi", ">"+isRunning);
                        Socket clientSocket = serverSocket.accept();
                        handleIncomingMessage(clientSocket);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void handleIncomingMessage(Socket clientSocket) {
        try {
            BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String message = input.readLine().toString();

            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MyMessageService.this, message, Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent("MSG_RECEIVED");
                    String type = "";
                    if (message.startsWith("image,") || message.startsWith("video,")) {
                        type = message.substring(0, ("image,".length()) - 1);

                        intent.putExtra("url", message.replace("image,", "").replace("video,", ""));
                        intent.putExtra("type", type);
                    }

                    if (message.startsWith("resume")) {
                        intent.putExtra("type", "resume");
                    }

                    if (message.startsWith("tag")) {
                        intent.putExtra("type", "tag");
                        intent.putExtra("tag", message.replace("tag,",""));
                    }

                    LocalBroadcastManager.getInstance(MyMessageService.this).sendBroadcast(intent);
                }
            });
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(MyMessageService.this, e.getMessage(), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(MyMessageService.this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Clean up resources and stop the service
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        isRunning = false;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't bind this service, so return null
        return null;
    }
}
