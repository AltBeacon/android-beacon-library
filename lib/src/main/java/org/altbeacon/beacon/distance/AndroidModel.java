package org.altbeacon.beacon.distance;

import android.os.Build;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.logging.LogManager;

/**
 * Represents a specific Android device model based on the available device build information
 *
 * Created by dyoung on 8/28/14.
 */
public class AndroidModel {
    private static final String TAG = "AndroidModel";
    String mVersion;
    String mBuildNumber;
    String mModel;
    String mManufacturer;


    public AndroidModel(String version, String buildNumber,
                        String model,
                        String manufacturer) {
        mVersion = version;
        mBuildNumber = buildNumber;
        mModel = model;
        mManufacturer = manufacturer;

    }
    public static AndroidModel forThisDevice() {
        return new AndroidModel(
            Build.VERSION.RELEASE,
            Build.ID,
            Build.MODEL,
            Build.MANUFACTURER);
    }

    public String getVersion() {
        return mVersion;
    }

    public void setVersion(String mVersion) {
        this.mVersion = mVersion;
    }

    public String getBuildNumber() {
        return mBuildNumber;
    }

    public String getModel() {
        return mModel;
    }


    public String getManufacturer() {
        return mManufacturer;
    }

    public void setBuildNumber(String mBuildNumber) {
        this.mBuildNumber = mBuildNumber;
    }

    public void setModel(String mModel) {
        this.mModel = mModel;
    }

    public void setManufacturer(String mManufacturer) {
        this.mManufacturer = mManufacturer;
    }

    /**
     * Calculates a qualitative match score between two different Android device models for the
     * purposes of how likely they are to have similar Bluetooth signal level responses
     * @param otherModel
     * @return match quality, higher numbers are a better match
     */
    public int matchScore(AndroidModel otherModel) {
        int score = 0;
        if (this.mManufacturer.equalsIgnoreCase(otherModel.mManufacturer)) {
            score = 1;
        }
        if (score ==1 && this.mModel.equals(otherModel.mModel)) {
            score = 2;
        }
        if (score == 2 && this.mBuildNumber.equals(otherModel.mBuildNumber)) {
            score = 3;
        }
        if (score == 3 && this.mVersion.equals(otherModel.mVersion)) {
            score = 4;
        }
        LogManager.d(TAG, "Score is %s for %s compared to %s", score, toString(), otherModel);
        return score;
    }

    @Override
    public String toString() {
        return ""+mManufacturer+";"+mModel+";"+mBuildNumber+";"+mVersion;
    }
}
