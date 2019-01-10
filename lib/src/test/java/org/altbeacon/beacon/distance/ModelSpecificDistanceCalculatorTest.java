package org.altbeacon.beacon.distance;

import android.content.Context;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.assertEquals;


@Config(sdk = 28)
@RunWith(RobolectricTestRunner.class)
/*
HOW TO SEE DEBUG LINES FROM YOUR UNIT TESTS:

1. set a line like this at the start of your test:
           org.robolectric.shadows.ShadowLog.stream = System.err;
2. run the tests from the command line
3. Look at the test report file in your web browser, e.g.
   file:///Users/dyoung/workspace/AndroidProximityLibrary/build/reports/tests/index.html
4. Expand the System.err section
/**
 * Created by dyoung on 8/28/14.
 */
public class ModelSpecificDistanceCalculatorTest {
    @Test
    public void testCalculatesDistance() {
        org.robolectric.shadows.ShadowLog.stream = System.err;

        ModelSpecificDistanceCalculator distanceCalculator = new ModelSpecificDistanceCalculator(null, null);
        Double distance = distanceCalculator.calculateDistance(-59, -59);
        assertEquals("Distance should be 1.0 for same power and rssi", 1.0, distance, 0.1);
    }

    @Test
    public void testSelectsDefaultModel() {
        org.robolectric.shadows.ShadowLog.stream = System.err;

        ModelSpecificDistanceCalculator distanceCalculator = new ModelSpecificDistanceCalculator(null, null);
        assertEquals("Default model should be Nexus 5", "Nexus 5", distanceCalculator.getModel().getModel());
    }

    @Test
    public void testSelectsNexus4OnExactMatch() {
        org.robolectric.shadows.ShadowLog.stream = System.err;
        AndroidModel model = new AndroidModel("4.4.2", "KOT49H","Nexus 4","LGE");

        ModelSpecificDistanceCalculator distanceCalculator = new ModelSpecificDistanceCalculator(null, null, model);
        assertEquals("should be Nexus 4", "Nexus 4", distanceCalculator.getModel().getModel());
    }

	@Test
	public void testCalculatesDistanceForMotoXPro() {
		final Context applicationContext = RuntimeEnvironment.application;
		org.robolectric.shadows.ShadowLog.stream = System.err;

		final AndroidModel model = new AndroidModel("5.0.2", "LXG22.67-7.1", "Moto X Pro", "XT1115");
		ModelSpecificDistanceCalculator distanceCalculator = new ModelSpecificDistanceCalculator(applicationContext, null, model);
		assertEquals("should be Moto X Pro", "Moto X Pro", distanceCalculator.getModel().getModel());
		Double distance = distanceCalculator.calculateDistance(-49, -58);
		assertEquals("Distance should be as predicted by coefficients at 3 meters", 2.661125466, distance, 0.1);
	}

	@Test
	public void testConcurrentModificationException() {
		org.robolectric.shadows.ShadowLog.stream = System.err;

		final Context applicationContext = RuntimeEnvironment.application;

		final AndroidModel model = new AndroidModel("4.4.2", "KOT49H", "Nexus 4", "LGE");
		final String modelMapJson =
				"{\"models\":[ \"coefficient1\": 0.89976,\"coefficient2\": 7.7095,\"coefficient3\": 0.111," +
				"\"version\":\"4.4.2\",\"build_number\":\"KOT49H\",\"model\":\"Nexus 4\"," +
				"\"manufacturer\":\"LGE\"},{\"coefficient1\": 0.42093,\"coefficient2\": 6.9476," +
				"\"coefficient3\": 0.54992,\"version\":\"4.4.2\",\"build_number\":\"LPV79\"," +
				"\"model\":\"Nexus 5\",\"manufacturer\":\"LGE\",\"default\": true}]}";
		final ModelSpecificDistanceCalculator distanceCalculator =
				new ModelSpecificDistanceCalculator(applicationContext, null, model);

		Runnable runnable2 = new Runnable() {
			@Override
			public void run() {
				try {
					while (true) {
						distanceCalculator.buildModelMapWithLock(modelMapJson);
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		};

		Thread thread2 = new Thread(runnable2);
		thread2.start();

		int i = 0;
		while (++i < 1000 && thread2.getState() != Thread.State.TERMINATED) {
			distanceCalculator.findCalculatorForModelWithLock(model);
		}

		thread2.interrupt();
	}
}
