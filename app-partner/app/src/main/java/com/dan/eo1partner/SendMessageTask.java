package com.dan.eo1partner;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class SendMessageTask extends AsyncTask<String, Void, Boolean> {
    private static final int PORT = 12345;
    private String ipAddress;

    public SendMessageTask(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    @Override
    protected Boolean doInBackground(String... params) {
        String messageToSend = params[0];
        boolean success = false;
        Socket socket = null;
        OutputStream outputStream = null;

        try {
            // Create a socket connection to the given IP address and port
            socket = new Socket(ipAddress, PORT);

            // Get the output stream of the socket
            outputStream = socket.getOutputStream();

            // Convert the message to bytes and send it
            byte[] messageBytes = messageToSend.getBytes();
            outputStream.write(messageBytes);
            outputStream.flush();

            success = true;
        } catch (IOException e) {
            e.printStackTrace();
            success = false;
        } finally {
            // Close the output stream and socket
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return success;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        if (result) {
            Log.e("hi", ">"+result);
            // Message sent successfully
            // You can handle the success scenario here
        } else {
            Log.e("hi", "bad>"+result);

            // Failed to send the message
            // You can handle the failure scenario here
        }
    }
}

