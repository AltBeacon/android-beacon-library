package com.android.test;

import android.util.Log;
import android.test.InstrumentationTestCase;
public class IBeaconTest extends InstrumentationTestCase {
    private static final String TAG = "IBeaconTest";

    @Override
    protected void setUp() throws Exception {
	Log.d(TAG, "setUp(): " + getName());
        super.setUp();
    }

    @Override 
    protected void tearDown() throws Exception {
	Log.d(TAG, "tearDown(): " + getName());
        super.tearDown();
    }

    public void testCase1() {
	assertTrue(true);
    }

}
