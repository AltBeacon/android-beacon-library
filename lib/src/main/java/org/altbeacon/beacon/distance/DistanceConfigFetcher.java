package org.altbeacon.beacon.distance;

import org.altbeacon.beacon.logging.LogManager;

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
        URL url;
        HttpURLConnection conn = null;
        do {
            if (requestCount != 0) {
                LogManager.d(TAG, "Following redirect from %s to %s",
                        mUrlString, conn.getHeaderField("Location"));
                currentUrlString = conn.getHeaderField("Location");
            }
            requestCount++;
            mResponseCode = -1;
            url = null;
            try {
                url = new URL(currentUrlString);
            } catch (Exception e) {
                LogManager.e(TAG, "Can't construct URL from: %s", mUrlString);
                mException = e;

            }
            if (url == null) {
                LogManager.d(TAG, "URL is null.  Cannot make request");
            } else {
                try {
                    conn = (HttpURLConnection) url.openConnection();
                    conn.addRequestProperty("User-Agent", mUserAgentString);
                    mResponseCode = conn.getResponseCode();
                    LogManager.d(TAG, "response code is %s", conn.getResponseCode());
                } catch (SecurityException e1) {
                    LogManager.w(e1, TAG, "Can't reach sever.  Have you added android.permission.INTERNET to your manifest?");
                    mException = e1;
                } catch (FileNotFoundException e2) {
                    LogManager.w(e2, TAG, "No data exists at \"+urlString");
                    mException = e2;
                } catch (java.io.IOException e3) {
                    LogManager.w(e3, TAG, "Can't reach server");
                    mException = e3;
                }
            }
        }
        while (requestCount < 10 &&
                (mResponseCode == HttpURLConnection.HTTP_MOVED_TEMP
                        || mResponseCode == HttpURLConnection.HTTP_MOVED_PERM
                        || mResponseCode == HttpURLConnection.HTTP_SEE_OTHER));

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
                LogManager.w(e, TAG, "error reading beacon data");
            }
        }

    }
}
