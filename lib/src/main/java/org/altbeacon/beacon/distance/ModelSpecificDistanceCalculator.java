package org.altbeacon.beacon.distance;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;

import org.altbeacon.beacon.logging.LogManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Obtains a <code>DistanceCalculator</code> appropriate for a specific Android model.  Each model
 * may have a different Bluetooth chipset, radio and antenna and sees a different signal level
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
    private final ReentrantLock mLock = new ReentrantLock();

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
        mDistanceCalculator = findCalculatorForModelWithLock(model);
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
            LogManager.w(TAG, "distance calculator has not been set");
            return -1.0;
        }
        return mDistanceCalculator.calculateDistance(txPower, rssi);
    }

    DistanceCalculator findCalculatorForModelWithLock(AndroidModel model) {
        mLock.lock();
        try {
            return findCalculatorForModel(model);
        } finally {
            mLock.unlock();
        }
    }

    private DistanceCalculator findCalculatorForModel(AndroidModel model) {
        LogManager.d(TAG, "Finding best distance calculator for %s, %s, %s, %s",
                model.getVersion(), model.getBuildNumber(), model.getModel(),
                model.getManufacturer());

        if (mModelMap == null) {
            LogManager.d(TAG, "Cannot get distance calculator because modelMap was never initialized");
            return null;
        }

        int highestScore = 0;
        AndroidModel bestMatchingModel = null;
        for (AndroidModel candidateModel : mModelMap.keySet()) {
            if (candidateModel.matchScore(model) > highestScore) {
                highestScore = candidateModel.matchScore(model);
                bestMatchingModel = candidateModel;
            }
        }
        if (bestMatchingModel != null) {
            LogManager.d(TAG, "found a match with score %s", highestScore);
            LogManager.d(TAG, "Finding best distance calculator for %s, %s, %s, %s",
                    bestMatchingModel.getVersion(), bestMatchingModel.getBuildNumber(),
                    bestMatchingModel.getModel(), bestMatchingModel.getManufacturer());
            mModel = bestMatchingModel;
        } else {
            mModel = mDefaultModel;
            LogManager.w(TAG, "Cannot find match for this device.  Using default");
        }
        return mModelMap.get(mModel);
    }

    private void loadModelMap() {
        boolean mapLoaded = false;
        if (mRemoteUpdateUrlString != null) {
            mapLoaded = loadModelMapFromFile();
            // We only want to try to download an update from the server the first time the app is
            // run.  If we successfully download an update it gets saved to a file, so if the file
            // is present that means should not download again.
            if (!mapLoaded) {
                requestModelMapFromWeb();
            }
        }
        if (!mapLoaded) {
            loadDefaultModelMap();
        }
        mDistanceCalculator = findCalculatorForModelWithLock(mRequestedModel);
    }

    private boolean loadModelMapFromFile() {
        File file = new File(mContext.getFilesDir(), CONFIG_FILE);
        FileInputStream inputStream = null;
        BufferedReader reader = null;
        StringBuilder sb = new StringBuilder();
        try {
            inputStream = new FileInputStream(file);
            reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        catch (FileNotFoundException fnfe){
            //This occurs on the first time the app is run, no error message necessary.
            return false;
        }
        catch (IOException e) {
            LogManager.e(e, TAG, "Cannot open distance model file %s", file);
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
            buildModelMapWithLock(sb.toString());
            return true;
        } catch (JSONException e) {
            LogManager.e(
                    e,
                    TAG,
                    "Cannot update distance models from online database at %s with JSON: %s",
                    mRemoteUpdateUrlString, sb.toString()
            );
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
            LogManager.w(e, TAG, "Cannot write updated distance model to local storage");
            return false;
        }
        finally {
            try {
                if (outputStream != null) outputStream.close();
            }
            catch (Exception e) {}
        }
        LogManager.i(TAG, "Successfully saved new distance model file");
        return true;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void requestModelMapFromWeb() {

        if (mContext.checkCallingOrSelfPermission("android.permission.INTERNET") != PackageManager.PERMISSION_GRANTED) {
            LogManager.w(TAG, "App has no android.permission.INTERNET permission.  Cannot check for distance model updates");
            return;
        }

        new ModelSpecificDistanceUpdater(mContext, mRemoteUpdateUrlString,
                new ModelSpecificDistanceUpdater.CompletionHandler() {
            @Override
            public void onComplete(String body, Exception ex, int code) {
                if (ex != null) {
                    LogManager.w(TAG, "Cannot updated distance models from online database at %s",
                            ex, mRemoteUpdateUrlString);
                }
                else if (code != 200) {
                    LogManager.w(TAG, "Cannot updated distance models from online database at %s "
                            + "due to HTTP status code %s", mRemoteUpdateUrlString, code);
                }
                else {
                    LogManager.d(TAG,
                            "Successfully downloaded distance models from online database");
                    try {
                        buildModelMapWithLock(body);
                        if (saveJson(body)) {
                            loadModelMapFromFile();
                            mDistanceCalculator = findCalculatorForModelWithLock(mRequestedModel);
                            LogManager.i(TAG, "Successfully updated distance model with latest from online database");
                        }
                    } catch (JSONException e) {
                        LogManager.w(e, TAG, "Cannot parse json from downloaded distance model");
                    }
                }
            }
        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    void buildModelMapWithLock(String jsonString) throws JSONException {
        mLock.lock();
        try {
            buildModelMap(jsonString);
        } finally {
            mLock.unlock();
        }
    }

    private void buildModelMap(String jsonString) throws JSONException {
        HashMap<AndroidModel, DistanceCalculator> map = new HashMap<AndroidModel, DistanceCalculator>();
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
            map.put(androidModel, distanceCalculator);
            if (defaultFlag) {
                mDefaultModel = androidModel;
            }
        }
        mModelMap = map;
    }

    private void loadDefaultModelMap() {
        try {
            buildModelMap(stringFromFilePath(CONFIG_FILE));
        }
        catch (Exception e) {
            mModelMap = new HashMap<AndroidModel, DistanceCalculator>();
            LogManager.e(e, TAG, "Cannot build model distance calculations");
        }
    }

    private String stringFromFilePath(String path) throws IOException {
        InputStream stream = null;
        BufferedReader bufferedReader = null;
        StringBuilder inputStringBuilder = new StringBuilder();
        try {
            stream = ModelSpecificDistanceCalculator.class.getResourceAsStream("/" + path);
            if (stream == null) {
                stream = this.getClass().getClassLoader().getResourceAsStream("/" + path);
            }

            if (stream == null) {
                throw new RuntimeException("Cannot load resource at " + path);
            }
            bufferedReader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
            String line = bufferedReader.readLine();
            while(line != null){
                inputStringBuilder.append(line);inputStringBuilder.append('\n');
                line = bufferedReader.readLine();
            }
        }
        finally {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (stream != null) {
                stream.close();
            }
        }
        return inputStringBuilder.toString();
    }

}
