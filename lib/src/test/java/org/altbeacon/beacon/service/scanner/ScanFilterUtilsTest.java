package org.altbeacon.beacon.service.scanner;


import android.bluetooth.le.ScanFilter;
import android.content.Context;

import org.altbeacon.beacon.AltBeaconParser;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.service.scanner.ScanFilterUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.mockito.Mockito;

@Config(sdk = 28)

@RunWith(RobolectricTestRunner.class)
public class ScanFilterUtilsTest {


    @BeforeClass
    public static void testSetup() {
    }

    @AfterClass
    public static void testCleanup() {

    }

    @Test
    public void testGetAltBeaconScanFilter() throws Exception {
        org.robolectric.shadows.ShadowLog.stream = System.err;
        BeaconParser parser = new AltBeaconParser();
        BeaconManager.setManifestCheckingDisabled(true); // no manifest available in robolectric
        List<ScanFilterUtils.ScanFilterData> scanFilterDatas = new ScanFilterUtils().createScanFilterDataForBeaconParser(parser, null);
        assertEquals("scanFilters should be of correct size", 1, scanFilterDatas.size());
        ScanFilterUtils.ScanFilterData sfd = scanFilterDatas.get(0);
        assertEquals("manufacturer should be right", 0x0118, sfd.manufacturer);
        assertEquals("mask length should be right", 2, sfd.mask.length);
        assertArrayEquals("mask should be right", new byte[] {(byte)0xff, (byte)0xff}, sfd.mask);
        assertArrayEquals("filter should be right", new byte[] {(byte)0xbe, (byte)0xac}, sfd.filter);
    }
    @Test
    public void testGenericScanFilter() throws Exception {
        org.robolectric.shadows.ShadowLog.stream = System.err;
        BeaconParser parser = new BeaconParser();
        parser.setBeaconLayout("m:2-3=1111,i:4-6,p:24-24");
        BeaconManager.setManifestCheckingDisabled(true); // no manifest available in robolectric
        List<ScanFilterUtils.ScanFilterData> scanFilterDatas = new ScanFilterUtils().createScanFilterDataForBeaconParser(parser, null);
        assertEquals("scanFilters should be of correct size", 1, scanFilterDatas.size());
        ScanFilterUtils.ScanFilterData sfd = scanFilterDatas.get(0);
        assertEquals("manufacturer should be right", 0x004c, sfd.manufacturer);
        assertEquals("mask length should be right", 2, sfd.mask.length);
        assertArrayEquals("mask should be right", new byte[]{(byte) 0xff, (byte) 0xff}, sfd.mask);
        assertArrayEquals("filter should be right", new byte[] {(byte)0x11, (byte) 0x11}, sfd.filter);
        assertNull("serviceUuid should be null", sfd.serviceUuid);
    }
    @Test
    public void testEddystoneScanFilterData() throws Exception {
        org.robolectric.shadows.ShadowLog.stream = System.err;
        BeaconParser parser = new BeaconParser();
        parser.setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT);
        BeaconManager.setManifestCheckingDisabled(true); // no manifest available in robolectric
        List<ScanFilterUtils.ScanFilterData> scanFilterDatas = new ScanFilterUtils().createScanFilterDataForBeaconParser(parser, null);
        assertEquals("scanFilters should be of correct size", 1, scanFilterDatas.size());
        ScanFilterUtils.ScanFilterData sfd = scanFilterDatas.get(0);
        assertEquals("serviceUuid should be right", new Long(0xfeaa), sfd.serviceUuid);
    }

    @Test
    public void testZeroOffsetScanFilter() throws Exception {
        org.robolectric.shadows.ShadowLog.stream = System.err;
        BeaconParser parser = new BeaconParser();
        parser.setBeaconLayout("m:0-3=11223344,i:4-6,p:24-24");
        BeaconManager.setManifestCheckingDisabled(true); // no manifest available in robolectric
        List<ScanFilterUtils.ScanFilterData> scanFilterDatas = new ScanFilterUtils().createScanFilterDataForBeaconParser(parser, null);
        assertEquals("scanFilters should be of correct size", 1, scanFilterDatas.size());
        ScanFilterUtils.ScanFilterData sfd = scanFilterDatas.get(0);
        assertEquals("manufacturer should be right", 0x004c, sfd.manufacturer);
        assertEquals("mask length should be right", 2, sfd.mask.length);
        assertArrayEquals("mask should be right", new byte[] {(byte)0xff, (byte)0xff}, sfd.mask);
        assertArrayEquals("filter should be right", new byte[] {(byte)0x33, (byte)0x44}, sfd.filter);
    }

    @Test
    public void testScanFilterWithIdentifiers() throws Exception {
        org.robolectric.shadows.ShadowLog.stream = System.err;
        BeaconParser parser = new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24");
        parser.setHardwareAssistManufacturerCodes(new int[] {0x004c});
        BeaconManager.setManifestCheckingDisabled(true); // no manifest available in robolectric
        ArrayList<Identifier> identifiers = new ArrayList<Identifier>();
        identifiers.add(Identifier.parse("2F234454-CF6D-4A0F-ADF2-F4911BA9FFA6"));
        identifiers.add(Identifier.parse("0x0102"));
        identifiers.add(Identifier.parse("0x0304"));
        List<ScanFilterUtils.ScanFilterData> scanFilterDatas = new ScanFilterUtils().createScanFilterDataForBeaconParser(parser, identifiers);
        assertEquals("scanFilters should be of correct size", 1, scanFilterDatas.size());
        ScanFilterUtils.ScanFilterData sfd = scanFilterDatas.get(0);
        assertEquals("manufacturer should be right", 0x004c, sfd.manufacturer);
        assertEquals("mask length should be right", 22, sfd.mask.length);
        assertArrayEquals("mask should be right", new byte[] {(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff}, sfd.mask);
        assertArrayEquals("filter should be right", new byte[] {(byte)0x02, (byte)0x15, (byte)0x2F, (byte)0x23, (byte)0x44, (byte)0x54, (byte)0xCF, (byte)0x6D, (byte)0x4A, (byte)0x0F, (byte)0xAD, (byte)0xF2, (byte)0xF4, (byte)0x91, (byte)0x1B, (byte)0xA9, (byte)0xFF, (byte)0xA6, (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04}, sfd.filter);
    }

}
