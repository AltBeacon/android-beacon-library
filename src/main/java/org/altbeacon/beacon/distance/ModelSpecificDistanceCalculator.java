package org.altbeacon.beacon.distance;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import org.altbeacon.beacon.BeaconManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Obtains a <code>DistanceCalculator</code> appropriate for a specific Android model.  Each model
 * may have a different bluetooth chipset, radio and antenna and sees a different signal level
 * at the same distance, therefore requiring a different equation coefficients for each model.
 *
 * This class uses a configuration table to look for a matching Android device model for which
 * coefficients are known.  If an exact match cannot be found, this class will attempt to find the
 * closest match possible based on the assumption that an unknown model made by Samsung, for example
 * might have a different signal response as a known device model also made by Samsung.
 *
 * If no match can be found at all, the device model marked as the default will be used for the
 * calculation.
 *
 * The configuration table is stored in model-distance-calculations.json
 *
 * For information on how to get new Android device models added to this table, please
 * see <a href='http://altbeacon.github.io/android-beacon-library/distance-calculations.html'
 * Optimizing Distance Calculations</a>
 *
 * Created by dyoung on 8/28/14.
 */
public class ModelSpecificDistanceCalculator implements DistanceCalculator {
    Map<AndroidModel,DistanceCalculator> mModelMap;
    private static final String CONFIG_FILE = "model-distance-calculations.json";
    private static final String TAG = "ModelSpecificDistanceCalculator";
    private AndroidModel mDefaultModel;
    private DistanceCalculator mDistanceCalculator;
    private AndroidModel mModel;
    private AndroidModel mRequestedModel;
    private String mRemoteUpdateUrlString = null;
    private Context mContext;

    /**
     * Obtains the best possible <code>DistanceCalculator</code> for the Android device calling
     * the constructor
     */
    public ModelSpecificDistanceCalculator(Context context, String remoteUpdateUrlString) {
        this(context, remoteUpdateUrlString, AndroidModel.forThisDevice());
    }
    /**
     * Obtains the best possible <code>DistanceCalculator</code> for the Android device passed
     * as an argument
     */
    public ModelSpecificDistanceCalculator(Context context, String remoteUpdateUrlString, AndroidModel model) {
        mRequestedModel = model;
        mRemoteUpdateUrlString = remoteUpdateUrlString;
        mContext = context;
        loadModelMap();
        mDistanceCalculator = findCalculatorForModel(model);
    }

    /**
     * @return the Android device model used for distance calculations
     */
    public AndroidModel getModel() {
        return mModel;
    }
    /**
     * @return the Android device model requested to be used for distance calculations
     */
    public AndroidModel getRequestedModel() {
        return mRequestedModel;
    }

    @Override
    public double calculateDistance(int txPower, double rssi) {
        if (mDistanceCalculator == null) {
            Log.w(TAG, "distance calculator has not been set");
            return -1.0;
        }
        return mDistanceCalculator.calculateDistance(txPower, rssi);
    }

    private DistanceCalculator findCalculatorForModel(AndroidModel model) {
        BeaconManager.logDebug(TAG, "Finding best distance calculator for "+model.getVersion()+","+
                model.getBuildNumber()+","+model.getModel()+"," +
                ""+model.getManufacturer());

        int highestScore = 0;
        AndroidModel bestMatchingModel = null;
        for (AndroidModel candidateModel : mModelMap.keySet()) {
            if (candidateModel.matchScore(model) > highestScore) {
                highestScore = candidateModel.matchScore(model);
                bestMatchingModel = candidateModel;
            }
        }
        if (bestMatchingModel != null) {
            BeaconManager.logDebug(TAG, "found a match with score "+highestScore);
            BeaconManager.logDebug(TAG, "Finding best distance calculator for "+bestMatchingModel.getVersion()+","+
                    bestMatchingModel.getBuildNumber()+","+bestMatchingModel.getModel()+"," +
                    ""+bestMatchingModel.getManufacturer());
            mModel = bestMatchingModel;
        } else {
            mModel = mDefaultModel;
            Log.w(TAG, "Cannot find match for this device.  Using default");
        }
        return mModelMap.get(mModel);
    }

    private void loadModelMap() {
        boolean mapLoaded = false;
        if (mRemoteUpdateUrlString != null) {
            mapLoaded= loadModelMapFromFile();
            // We only want to try to download an update from the server the first time the app is
            // run.  If we successfully download an update it gets saved to a file, so if the file
            // is present that means should not download again
            if (!mapLoaded) {
                requestModelMapFromWeb();
            }
        }
        if (!mapLoaded) {
            loadDefaultModelMap();
        }
        mDistanceCalculator = findCalculatorForModel(mRequestedModel);
    }

    private boolean loadModelMapFromFile() {
        File file = new File(mContext.getFilesDir(), CONFIG_FILE);
        FileInputStream inputStream = null;
        BufferedReader reader = null;
        StringBuilder sb = new StringBuilder();
        try {
            inputStream = new FileInputStream(file);
            reader = new BufferedReader(new InputStreamReader(inputStream));
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        catch (IOException e) {
            Log.w(TAG, "Cannot open distance model file "+file);
            return false;
        }
        finally {
            if (reader != null) {
                try { reader.close(); } catch (Exception e2) {}
            }
            if (inputStream != null) {
                try { inputStream.close(); } catch (Exception e2) {}
            }
        }
        try {
            buildModelMap(sb.toString());
            return true;
        } catch (JSONException e) {
            Log.w(TAG, "Cannot update distance models from online database at "+mRemoteUpdateUrlString+
                    " with JSON of "+sb.toString()+" due to exception ", e);
            return false;
        }
    }

    private boolean saveJson(String jsonString) {

        FileOutputStream outputStream = null;

        try {
            outputStream = mContext.openFileOutput(CONFIG_FILE, Context.MODE_PRIVATE);
            outputStream.write(jsonString.getBytes());
            outputStream.close();
        } catch (Exception e) {
            Log.w(TAG, "Cannot write updated distance model to local storage", e);
            return false;
        }
        finally {
            try {
                if (outputStream != null) outputStream.close();
            }
            catch (Exception e) {}
        }
        Log.i(TAG, "Successfully saved new distance model file");
        return true;
    }

    @TargetApi(Build.VERSION_CODES.CUPCAKE)
    private void requestModelMapFromWeb() {

        if (mContext.checkCallingOrSelfPermission("android.permission.INTERNET") != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "App has no android.permission.INTERNET permission.  Cannot check for distance model updates");
            return;
        }

        new ModelSpecificDistanceUpdater(mContext, mRemoteUpdateUrlString,
                new ModelSpecificDistanceUpdater.CompletionHandler() {
            @Override
            public void onComplete(String body, Exception ex, int code) {
                if (ex != null) {
                    Log.w(TAG, "Cannot updated distance models from online database at "+mRemoteUpdateUrlString+
                            " due to exception: "+ex);
                }
                else if (code != 200) {
                    Log.w(TAG, "Cannot updated distance models from online database at "+mRemoteUpdateUrlString+
                            " due to HTTP status code "+code);

                }
                else {
                    BeaconManager.logDebug(TAG,
                            "Successfully downloaded distance models from online database");
                    try {
                        buildModelMap(body);
                        if (saveJson(body)) {
                            loadModelMapFromFile();
                            mDistanceCalculator = findCalculatorForModel(mRequestedModel);
                            Log.i(TAG, "Successfully updated distance model with latest from online database");
                        }
                    } catch (JSONException e) {
                        Log.w(TAG, "Cannot parse json from downloaded distance model",e);
                    }
                }
            }
        }).execute(null, null, null);
    }

    private void buildModelMap(String jsonString) throws JSONException {
        mModelMap = new HashMap<AndroidModel, DistanceCalculator>();
        JSONObject jsonObject = new JSONObject(jsonString);
        JSONArray array = jsonObject.getJSONArray("models");
        for (int i = 0; i < array.length(); i++) {
            JSONObject modelObject = array.getJSONObject(i);
            boolean defaultFlag = false;
            if (modelObject.has("default")) {
                defaultFlag = modelObject.getBoolean("default");
            }
            Double coefficient1 = modelObject.getDouble("coefficient1");
            Double coefficient2 = modelObject.getDouble("coefficient2");
            Double coefficient3 = modelObject.getDouble("coefficient3");
            String version = modelObject.getString("version");
            String buildNumber = modelObject.getString("build_number");
            String model = modelObject.getString("model");
            String manufacturer = modelObject.getString("manufacturer");

            CurveFittedDistanceCalculator distanceCalculator =
                    new CurveFittedDistanceCalculator(coefficient1,coefficient2,coefficient3);

            AndroidModel androidModel = new AndroidModel(version, buildNumber, model, manufacturer);
            mModelMap.put(androidModel, distanceCalculator);
            if (defaultFlag) {
                mDefaultModel = androidModel;
            }
        }
    }
    private void loadDefaultModelMap() {
        mModelMap = new HashMap<AndroidModel, DistanceCalculator>();
        try {
            buildModelMap(stringFromFilePath(CONFIG_FILE));
        }
        catch (Exception e) {
            throw new RuntimeException("Cannot build model distance calculations", e);
        }
    }

    private String stringFromFilePath(String path) throws IOException {
        InputStream stream = ModelSpecificDistanceCalculator.class.getResourceAsStream("/"+path);
        if (stream == null) {
            this.getClass().getClassLoader().getResourceAsStream("/"+path);
        }

        if (stream == null) {
            throw new RuntimeException("Cannot load resource at "+path);
        }
        StringBuilder inputStringBuilder = new StringBuilder();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
        String line = bufferedReader.readLine();
        while(line != null){
            inputStringBuilder.append(line);inputStringBuilder.append('\n');
            line = bufferedReader.readLine();
        }
        return inputStringBuilder.toString();
    }

}
