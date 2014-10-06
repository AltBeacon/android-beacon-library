package org.altbeacon.beacon.distance;

import android.util.Log;

import org.altbeacon.beacon.BeaconManager;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by dyoung on 12/11/13.
 *
 * @exclude
 */
public class DistanceConfigFetcher {
    private static final String TAG = "DistanceConfigFetcher";
    protected String mResponse;
    protected Exception mException;
    private int mResponseCode = -1;
    private String mUrlString;
    private String mUserAgentString;

    public DistanceConfigFetcher(String urlString, String userAgentString) {
        this.mUrlString = urlString;
        this.mUserAgentString = userAgentString;
    }


    public int getResponseCode() {
        return mResponseCode;
    }

    public String getResponseString() {
        return mResponse;
    }

    public Exception getException() {
        return mException;
    }

    public void request() {
        mResponse = null;
        String currentUrlString = mUrlString;
        int requestCount = 0;
        StringBuilder responseBuilder = new StringBuilder();
        URL url = null;
        HttpURLConnection conn = null;
        do {
            if (requestCount != 0) {
                if (BeaconManager.debug)
                    Log.d(TAG, "Following redirect from " + mUrlString + " to " + conn.getHeaderField("Location"));
                currentUrlString = conn.getHeaderField("Location");
            }
            requestCount++;
            mResponseCode = -1;
            try {
                url = new URL(currentUrlString);
            } catch (Exception e) {
                Log.e(TAG, "Can't construct URL from: " + mUrlString);
                mException = e;

            }
            if (url == null) {
                if (BeaconManager.debug) Log.d(TAG, "URL is null.  Cannot make request");
            } else {
                try {
                    conn = (HttpURLConnection) url.openConnection();
                    conn.addRequestProperty("User-Agent", mUserAgentString);
                    mResponseCode = conn.getResponseCode();
                    if (BeaconManager.debug)
                        Log.d(TAG, "response code is " + conn.getResponseCode());
                } catch (SecurityException e1) {
                    Log.w(TAG, "Can't reach sever.  Have you added android.permission.INTERNET to your manifest?", e1);
                    mException = e1;
                } catch (FileNotFoundException e2) {
                    Log.w(TAG, "No data exists at \"+urlString", e2);
                    mException = e2;
                } catch (java.io.IOException e3) {
                    Log.w(TAG, "Can't reach server", e3);
                    mException = e3;
                }
            }
        }
        while (requestCount < 10 && mResponseCode == HttpURLConnection.HTTP_MOVED_TEMP
                || mResponseCode == HttpURLConnection.HTTP_MOVED_PERM
                || mResponseCode == HttpURLConnection.HTTP_SEE_OTHER);

        if (mException == null) {
            try {
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(
                                conn.getInputStream())
                );
                String inputLine;
                while ((inputLine = in.readLine()) != null)
                    responseBuilder.append(inputLine);
                in.close();
                mResponse = responseBuilder.toString();
            } catch (Exception e) {
                mException = e;
                Log.w(TAG, "error reading beacon data", e);
            }
        }

    }
}
