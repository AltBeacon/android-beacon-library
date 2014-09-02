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
 * Created by dyoung on 8/28/14.
 */
public class ModelSpecificDistanceCalculator implements DistanceCalculator {
    Map<AndroidModel,DistanceCalculator> mModelMap;
    private static final String CONFIG_FILE = "/model-distance-calculations.json";
    private static final String TAG = "ModelSpecificDistanceCalculator";
    private AndroidModel mDefaultModel;
    private DistanceCalculator mDistanceCalculator;
    private AndroidModel mModel;

    public ModelSpecificDistanceCalculator() {
        this(AndroidModel.forThisDevice());
    }
    public ModelSpecificDistanceCalculator(AndroidModel model) {
        loadModelMap();
        mDistanceCalculator = findCalculatorForModel(model);
    }
    public AndroidModel getModel() {
        return mModel;
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

    @Override
    public double calculateDistance(int txPower, double rssi) {
        return mDistanceCalculator.calculateDistance(txPower, rssi);
    }
}
