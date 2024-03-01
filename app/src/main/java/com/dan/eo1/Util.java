package com.dan.eo1;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.RadioGroup;

import java.io.File;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class Util {

    public static String getIPAddress() {
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

    public static void cacheCleanup(File cacheDir) {
        long totalSpace = cacheDir.getTotalSpace();
        long freeSpace = cacheDir.getFreeSpace();
        long threshold = (long) (0.3 * totalSpace);

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                //Toast.makeText(MainActivity.this, "Free space: " + (((double) fs2 / totalSpace) * 100) + "%", Toast.LENGTH_SHORT).show();
            }
        });

        while (freeSpace < threshold) {
            File[] files = cacheDir.listFiles();
            File oldestFile = null;
            long oldestFileTime = Long.MAX_VALUE;
            for (File file : files) {
                if (file.lastModified() < oldestFileTime) {
                    oldestFile = file;
                    oldestFileTime = file.lastModified();
                }
            }
            if (oldestFile != null) {
                long oldlen = oldestFile.length();
                if (oldestFile.delete()) {
                    freeSpace += oldlen;
                } else {
                    break;
                }
            } else {
                break;
            }
        }
    }

    public static int getSelectedOptionIndex(RadioGroup radioGroup) {
        int checkedRadioButtonId = radioGroup.getCheckedRadioButtonId();
        View checkedRadioButton = radioGroup.findViewById(checkedRadioButtonId);
        return radioGroup.indexOfChild(checkedRadioButton);
    }


    public static boolean isNetworkAvailable(Context t) {
        ConnectivityManager connectivityManager = (ConnectivityManager) t.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

}
