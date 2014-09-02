package org.altbeacon.beacon.distance;

import android.os.Build;
import android.util.Log;

/**
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

    public int matchScore(AndroidModel otherModel) {
        int score = 0;
        if (this.mManufacturer.equals(otherModel.mManufacturer)) {
            score = 1;
        }
        if (this.mModel.equals(otherModel.mModel)) {
            score = 2;
        }
        if (this.mBuildNumber.equals(otherModel.mBuildNumber)) {
            score = 3;
        }
        if (this.mVersion.equals(otherModel.mVersion)) {
            score = 4;
        }
        Log.d(TAG, "Score is " + score + " for " + this + " compared to " + otherModel);
        return score;
    }

    @Override
    public String toString() {
        return ""+mManufacturer+" "+mModel+" "+mBuildNumber+" "+mVersion;
    }
}
