package org.altbeacon.beacon.distance;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import org.altbeacon.beacon.BuildConfig;
import org.json.JSONObject;

/**
 * Created by dyoung on 9/12/14.
 */
@TargetApi(Build.VERSION_CODES.CUPCAKE)
public class ModelSpecificDistanceUpdater extends AsyncTask<Void, Void, Void> {

    private static final String TAG = "ModelSpecificDistanceUpdater";
    private Exception exception = null;
    private String urlString = null;
    private String response = null;
    private Context mContext;
    private DistanceConfigFetcher mDistanceConfigFetcher;
    private CompletionHandler mCompletionHandler;

    @Override
    protected Void doInBackground(Void... params) {
        mDistanceConfigFetcher.request();
        if (mCompletionHandler != null) {
            mCompletionHandler.onComplete(mDistanceConfigFetcher.getResponseString(), mDistanceConfigFetcher.getException(), mDistanceConfigFetcher.getResponseCode());
        }
        return null;
    }

    protected void onPostExecute() {
    }

    public ModelSpecificDistanceUpdater(Context context, String urlString, CompletionHandler completionHandler) {
        mContext = context;
        mDistanceConfigFetcher = new DistanceConfigFetcher(urlString, getUserAgentString());
        mCompletionHandler = completionHandler;
    }

    private String getUserAgentString() {
        return "Android Beacon Library;"+getVersion()+";"+getPackage()+";"+getInstallId()+";"+getModel();
    }
    private String getPackage() {
        return mContext.getPackageName();
    }
    private String getModel() {
        return AndroidModel.forThisDevice().toString();
    }
    private String getInstallId() {
        return Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.ANDROID_ID);
    }
    private String getVersion() {
        return BuildConfig.VERSION_NAME;
    }

    interface CompletionHandler {
        public void onComplete(String body, Exception exception, int code);
    }

}
