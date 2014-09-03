package org.altbeacon.beacon.distance;

import android.os.Build;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
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
    private static final String CONFIG_FILE = "/model-distance-calculations.json";
    private static final String TAG = "ModelSpecificDistanceCalculator";
    private AndroidModel mDefaultModel;
    private DistanceCalculator mDistanceCalculator;
    private AndroidModel mModel;

    /**
     * Obtains the best possible <code>DistanceCalculator</code> for the Android device calling
     * the constructor
     */
    public ModelSpecificDistanceCalculator() {
        this(AndroidModel.forThisDevice());
    }
    /**
     * Obtains the best possible <code>DistanceCalculator</code> for the Android device passed
     * as an argument
     */
    public ModelSpecificDistanceCalculator(AndroidModel model) {
        loadModelMap();
        mDistanceCalculator = findCalculatorForModel(model);
    }

    /**
     * @return the Android device model used for distance calculations
     */
    public AndroidModel getModel() {
        return mModel;
    }

    @Override
    public double calculateDistance(int txPower, double rssi) {
        return mDistanceCalculator.calculateDistance(txPower, rssi);
    }

    private DistanceCalculator findCalculatorForModel(AndroidModel model) {
        Log.d(TAG, "Finding best distance calculator for "+model.getVersion()+","+
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
            Log.d(TAG, "found a match with score "+highestScore);
            Log.d(TAG, "Finding best distance calculator for "+bestMatchingModel.getVersion()+","+
                    bestMatchingModel.getBuildNumber()+","+bestMatchingModel.getModel()+"," +
                    ""+bestMatchingModel.getManufacturer());
            mModel = bestMatchingModel;
        } else {
            mModel = mDefaultModel;
            Log.d(TAG, "Cannot find match for this device.  Using default");
        }
        return mModelMap.get(mModel);
    }

    private void loadModelMap() {
        mModelMap = new HashMap<AndroidModel, DistanceCalculator>();
        try {
            JSONObject jsonObject = new JSONObject(stringFromFilePath(CONFIG_FILE));
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
        catch (Exception e) {
            throw new RuntimeException("Cannot build model distance calculations", e);
        }
    }

    private String stringFromFilePath(String path) throws IOException {
        InputStream stream = ModelSpecificDistanceCalculator.class.getResourceAsStream(path);
        if (stream == null) {
            Log.d(TAG, "Try 2");
            this.getClass().getClassLoader().getResourceAsStream(path);
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
