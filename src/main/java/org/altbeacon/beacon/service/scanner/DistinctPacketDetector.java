package org.altbeacon.beacon.service.scanner;

import android.util.Log;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by dyoung on 4/8/17.
 *
 * This class tracks whether multiple distinct BLE packets have been seen, with the purpose of
 * determining if the Android device supports detecting multiple distinct packets in a single scan.
 * Some older devices are not capable of this (e.g. Nexus 4, Moto G1), so detecting multiple packets
 * requires stopping and restarting scanning on these devices.  This allows detecting if that is
 * neessary
 */
public class DistinctPacketDetector {
    // Sanity limit for the number of packets to track, so we don't use too much memory
    private static final int MAX_PACKETS_TO_TRACK = 1000;
    protected Set<ByteBuffer> mDistinctPacketsDetected = new HashSet<ByteBuffer>();

    public void clearDetections() {
        mDistinctPacketsDetected.clear();
    }

    public boolean isPacketDistinct(String originMacAddress, byte[] scanRecord) {
        byte[] macBytes = originMacAddress.getBytes();
        ByteBuffer buffer = ByteBuffer.allocate(macBytes.length+scanRecord.length);
        buffer.put(macBytes);
        buffer.put(scanRecord);
        buffer.rewind(); // rewind puts position back to beginning so .equals and .hashCode work

        boolean distinct = !mDistinctPacketsDetected.contains(buffer);
        if (mDistinctPacketsDetected.size() == MAX_PACKETS_TO_TRACK) {
            return mDistinctPacketsDetected.contains(buffer);
        }
        else {
            return mDistinctPacketsDetected.add(buffer);
        }
    }

}
