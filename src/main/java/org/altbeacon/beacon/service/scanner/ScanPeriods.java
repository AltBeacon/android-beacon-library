package org.altbeacon.beacon.service.scanner;

import android.os.Parcel;
import android.os.Parcelable;

/**
 */
public class ScanPeriods implements Parcelable{

    private long scanPeriod;
    private long betweenScanPeriod;
    private long fullPeriod;

    public ScanPeriods(long scanPeriod, long betweenScanPeriod) {
        this.scanPeriod = scanPeriod;
        this.betweenScanPeriod = betweenScanPeriod;
        fullPeriod = scanPeriod + betweenScanPeriod;
    }

    public long getBetweenScanPeriod() {
        return betweenScanPeriod;
    }

    public long getScanPeriod() {
        return scanPeriod;
    }

    public long getFullPeriod() {
        return fullPeriod;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(scanPeriod);
        dest.writeLong(betweenScanPeriod);
        dest.writeLong(fullPeriod);
    }

    protected ScanPeriods(Parcel in){
        scanPeriod = in.readLong();
        betweenScanPeriod = in.readLong();
        fullPeriod = in.readLong();
    }

    public static final Creator<ScanPeriods> CREATOR
            = new Creator<ScanPeriods>() {
        public ScanPeriods createFromParcel(Parcel in) {
            return new ScanPeriods(in);
        }

        public ScanPeriods[] newArray(int size) {
            return new ScanPeriods[size];
        }
    };


}
