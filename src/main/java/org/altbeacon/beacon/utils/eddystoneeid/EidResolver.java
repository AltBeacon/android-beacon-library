package org.altbeacon.beacon.utils.eddystoneeid;

import android.annotation.TargetApi;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Base64;
import android.util.Log;

import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.logging.LogManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.IllegalArgumentException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.Executor;

/**
 * Utility class for resolving encrypted Eddystone-EID identifiers
 * Created by dyoung on 4/5/2016
 */
public class EidResolver {
    private static final String TAG = EidResolver.class.getSimpleName();
    private static final String GOOGLE_PROXIMITY_BEACON_API_GET_FOR_OBSERVED_URL =
          "https://proximitybeacon.googleapis.com/v1beta1/beaconinfo:getforobserved";
    private static final String GOOGLE_PROXIMITY_BEACON_API_GET_URL =
            "https://proximitybeacon.googleapis.com/v1beta1/beacons/4!";
    private static final Double MAX_SERVICE_CALL_SECONDS = 10.0;
    private String mGoogleProximityBeaconApiKey = null;
    private String mGoogleOAuthToken = null;
    private HashMap<Identifier,String> mResolvedIdentifiers;
    private HashMap<Identifier,Date> mResolutionServiceCalls;
    private Identifier mIdentifierBeingResolved = null;
    private Executor mExecutor;
    private String mResolutionAttachmentNamespacedType = null;

    /**
     * Makes a new EidResolver with a Google API key
     * @param googleProximityBeaconApiKey An API Key from the Google Proximity Beacon API.  The key must be for the account where the beacon to be resolved is registered.
     * @param resolutionAttachmentNamespacedType The key of the attachment stored in Google's Proximity Beacon API used to resolve the beacon to a fixed identifier.  Cannot be null.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static EidResolver getInstanceWithGoogleApiKey(String googleProximityBeaconApiKey, String resolutionAttachmentNamespacedType) {
        EidResolver resolver = new EidResolver();
        if (googleProximityBeaconApiKey == null) {
            throw new IllegalArgumentException("googleProximityBeaconApiKey may not be null");
        }
        if (resolutionAttachmentNamespacedType == null) {
            throw new IllegalArgumentException("resolutionAttachmentNamespacedType may not be null");
        }
        resolver.mResolutionAttachmentNamespacedType = resolutionAttachmentNamespacedType;
        resolver.mGoogleProximityBeaconApiKey = googleProximityBeaconApiKey;
        return resolver;
    }

    /**
     * Makes a new EidResolver with a Google Oauth token.  A resolver created using this method will resolve the encrypted beacon with its registered beaconName.
     * @param googleOAuthToken An OAuth token from a previous Oauth authentication. The token must be associated with a Google Account where the proximity beecon to be resolved is registered.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static EidResolver getInstanceWithGoogleOauthToken(String googleOAuthToken) {
        EidResolver resolver = new EidResolver();
        if (googleOAuthToken == null) {
            throw new IllegalArgumentException("googleOAuthToken may not be null");
        }
        resolver.mResolutionAttachmentNamespacedType = null;
        resolver.mGoogleOAuthToken = googleOAuthToken;
        return resolver;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private EidResolver() {
        mResolutionServiceCalls = new HashMap<Identifier, Date>();
        mResolvedIdentifiers = new HashMap<Identifier, String>();
        mIdentifierBeingResolved = null;
        mExecutor = AsyncTask.THREAD_POOL_EXECUTOR;
    }

    public void setExecutor(Executor executor) {
        mExecutor = executor;
    }

    /**
     Returns a resolved identifier from cache if one is already known.  If one is not known, an asynchronous call will
     be made to look it up via the Google resolver (up to once per every 10 seconds) so it may be available on the next
     call, but nil will be returned on this call.
     */
    public String getResolvedIdentifierString(Identifier identifier) {
        String resolvedId = mResolvedIdentifiers.get(identifier);
        if (resolvedId == null) {
            // Make a service call to look up this id if we have not done so in the last few secs
            Date lastCallTime = mResolutionServiceCalls.get(identifier);

            if (mIdentifierBeingResolved == null) {
                Boolean callAllowed = true;
                if (lastCallTime != null) {
                    if (System.currentTimeMillis() - lastCallTime.getTime() < MAX_SERVICE_CALL_SECONDS * 1000l) {
                        callAllowed = false;
                    }
                }
                if (callAllowed) {
                    mResolutionServiceCalls.put(identifier, new Date());
                    resolveIdentifier(identifier);
                }
            }
        }
        return resolvedId;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void resolveIdentifier(final Identifier identifier) {
        mIdentifierBeingResolved = identifier;
        byte[] data = identifier.toByteArray();
        String base64EncodedData = Base64.encodeToString(data, Base64.NO_WRAP);
        String urlString;
        if (mGoogleOAuthToken != null) {
            urlString = GOOGLE_PROXIMITY_BEACON_API_GET_URL + identifier.toString().toLowerCase().replace("0x", "");
            new AsyncCall(urlString, null, new AsyncCallback() {
                @Override
                public void onResponse(Integer statusCode, String body, Exception exception) {
                    mIdentifierBeingResolved = null;
                    if (exception == null ) {
                        if (statusCode >= 200 && statusCode < 300) {
                            String resolvedIdentifierString = parseIdentifierStringFromJsonResponse(body);
                            if (resolvedIdentifierString != null) {
                                mResolvedIdentifiers.put(identifier, resolvedIdentifierString);
                            }
                        }
                        else {
                            if (LogManager.isVerboseLoggingEnabled()) {
                                LogManager.d(TAG, "bad response code resolving eddystone-eid: %d", statusCode);
                            }
                        }
                    }
                }
            }).executeOnExecutor(mExecutor, new Void[]{});
        }
        else {
            urlString = GOOGLE_PROXIMITY_BEACON_API_GET_FOR_OBSERVED_URL + "?key=" + mGoogleProximityBeaconApiKey;
            String jsonString = "{\"observations\":[{\"advertisedId\":  {\"type\": \"EDDYSTONE_EID\",\"id\": \""+base64EncodedData+"\"}}],\"namespacedTypes\":[\""+mResolutionAttachmentNamespacedType+"\"]}";
            new AsyncCall(urlString, jsonString, new AsyncCallback() {
                @Override
                public void onResponse(Integer statusCode, String body, Exception exception) {
                    mIdentifierBeingResolved = null;
                    if (exception == null ) {
                        if (statusCode >= 200 && statusCode < 300) {
                            String resolvedIdentifierString = parseIdentifierStringFromJsonResponse(body);
                            if (resolvedIdentifierString != null) {
                                mResolvedIdentifiers.put(identifier, resolvedIdentifierString);
                            }
                        }
                        else {
                            if (LogManager.isVerboseLoggingEnabled()) {
                                LogManager.d(TAG, "bad response code resolving eddystone-eid: %d", statusCode);
                            }
                        }
                    }
                }
            }).executeOnExecutor(mExecutor, new Void[]{});
        }
   }

    private interface AsyncCallback {
        public void onResponse(Integer statusCode, String body, Exception e);
    }

    private class AsyncCall extends AsyncTask<Void, Void, Void>
    {
        private AsyncCallback mCallback;
        private String mUrlString;
        private String mJsonString;

        public AsyncCall(String urlString, String jsonString,  AsyncCallback callback) {
            mJsonString = jsonString;
            mCallback = callback;
            mUrlString = urlString;
        }

        @Override
        protected Void doInBackground(Void... data) {
            JSONObject jsonObject = null;
            int requestCount = 0;
            StringBuilder responseBuilder = new StringBuilder();
            URL url = null;
            HttpURLConnection conn = null;
            try {
                url = new URL(mUrlString);
                requestCount++;
                if (LogManager.isVerboseLoggingEnabled()) {
                    LogManager.d(TAG, "calling service at "+mUrlString);
                }
                conn = (HttpURLConnection) url.openConnection();
                conn.addRequestProperty("Accept", "application/json");
                conn.addRequestProperty("Content-Type", "application/json");
                if (mGoogleOAuthToken != null) {
                    conn.addRequestProperty("Authorization", "Bearer " + mGoogleOAuthToken);
                }

                if (mJsonString == null) {
                    conn.setRequestMethod("GET");
                }
                else {
                    conn.setRequestMethod("POST");
                    OutputStream out = conn.getOutputStream();
                    try {
                        Writer writer = new OutputStreamWriter(out, "UTF-8");
                        writer.write(mJsonString);
                        writer.close();
                    } finally {
                        if (out != null)
                            out.close();
                    }
                }

                BufferedReader in = null;
                try {
                    in = new BufferedReader(
                            new InputStreamReader(
                                    conn.getInputStream())
                    );
                    String inputLine;
                    while ((inputLine = in.readLine()) != null)
                        responseBuilder.append(inputLine);
                    in.close();
                    if (mCallback != null) {
                        mCallback.onResponse(conn.getResponseCode(), responseBuilder.toString(), null);
                    }
                    responseBuilder.toString();
                }
                finally {
                    if (in != null) {
                        in.close();
                    }
                }
            }
            catch (FileNotFoundException e1) {
                mCallback.onResponse(404, null, null);
            }
            catch (IOException e) {
                if (LogManager.isVerboseLoggingEnabled()) {
                    Log.d(TAG, "IOException making remote service call", e);
                }
                if (mCallback != null) {
                    mCallback.onResponse(null, null, e);
                }
            }
            return null;
        }
    }

    private String parseIdentifierStringFromJsonResponse(String jsonResponse) {
        try {
            JSONObject json = new JSONObject(jsonResponse);
            if (!json.isNull("error")) {
                String error = json.getString("error");
                if (LogManager.isVerboseLoggingEnabled()) {
                    LogManager.d(TAG, "Google resolver returned error response: %s", error);
                }
                return null;
            }
            else {
                JSONArray beacons = json.getJSONArray("beacons");
                if (beacons != null) {

                    JSONObject beacon = beacons.getJSONObject(0);
                    if (beacon != null) {
                        if (mResolutionAttachmentNamespacedType == null) {
                            String resolvedId =  beacon.getString("beaconName");
                            if (resolvedId.indexOf("beacons/") == 0 && resolvedId.length() > 10) {
                                return resolvedId.substring(10);
                            }
                            else {
                                if (LogManager.isVerboseLoggingEnabled()) {
                                    LogManager.d(TAG, "Cannot find beacons/ prefix on resolved name: "+resolvedId);
                                }
                            }
                        }
                        else {
                            // Get resolved identifier from an attachment with mResolutionAttachmentNamespacedType
                            if (beacon.has("attachments")) {
                                JSONArray attachments = beacon.getJSONArray("attachments");
                                for (int i = 0; i < attachments.length(); i++) {
                                    JSONObject attachment = attachments.getJSONObject(0);
                                    if (attachment.has("namespacedType") && attachment.has("data")) {
                                        if (attachment.getString("namespacedType").equals(mResolutionAttachmentNamespacedType)) {
                                            return attachment.getString("data");
                                        }
                                    }
                                }
                            }
                        }


                        if (LogManager.isVerboseLoggingEnabled()) {
                            LogManager.d(TAG, "Cannot resolve this beacon with the specified field");
                        }
                    }
                }
                if (LogManager.isVerboseLoggingEnabled()) {
                    LogManager.d(TAG, "Google does not know about this beacon");
                }
                return null;
            }
        }
        catch (JSONException e ) {
            //04-07 11:05:43.875 1711-4974/com.radiusnetworks.locate D/EidResolver: Cannot parse Google response: {  "beacons": [    {      "advertisedId": {        "type": "EDDYSTONE_EID",        "id": "fk+Yj5W30BU="      },      "beaconName": "beacons/4!7e4f988f95b7d015"    }  ]}

            if (LogManager.isVerboseLoggingEnabled()) {
                LogManager.d(TAG, "Cannot parse Google response: "+jsonResponse+" due to "+e.getMessage());
            }
        }
        return null;
    }
}
