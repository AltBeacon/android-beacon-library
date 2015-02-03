/*
 * Copyright 2015 Radius Networks, Inc.
 * Copyright 2015 Andrew Reitz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.altbeacon.beacon.logging;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import static android.util.Log.DEBUG;
import static android.util.Log.ERROR;
import static android.util.Log.INFO;
import static android.util.Log.VERBOSE;
import static android.util.Log.WARN;
import static junit.framework.Assert.assertEquals;

/**
 * Ensure the verbose logger logs correctly.
 *
 * @author Andrew Reitz
 */
@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class VerboseAndroidLoggerTest {
    private String tag = getClass().getName();
    private Logger logger = new VerboseAndroidLogger();

    @Test
    public void verboseLoggedCorrectly() {
        String expectedMessage = "Hello World";

        logger.v(tag, "Hello %s", "World");

        assertLogged(VERBOSE, tag, expectedMessage, null);
    }

    @Test
    public void verboseWithThrowableLoggedCorrectly() {
        String expectedMessage = "Hello World";
        Throwable t = new Throwable("Test Throwable");

        logger.v(t, tag, "Hello %s", "World");

        assertLogged(VERBOSE, tag, expectedMessage, t);
    }

    @Test
    public void debugLoggedCorrectly() {
        String expectedMessage = "Hello World";

        logger.d(tag, "Hello %s", "World");

        assertLogged(DEBUG, tag, expectedMessage, null);
    }

    @Test
    public void debugWithThrowableLoggedCorrectly() {
        String expectedMessage = "Hello World";
        Throwable t = new Throwable("Test Throwable");

        logger.d(t, tag, "Hello %s", "World");

        assertLogged(DEBUG, tag, expectedMessage, t);
    }

    @Test
    public void infoLoggedCorrectly() {
        String expectedMessage = "Hello World";

        logger.v(tag, "Hello %s", "World");

        assertLogged(VERBOSE, tag, expectedMessage, null);
    }

    @Test
    public void infoWithThrowableLoggedCorrectly() {
        String expectedMessage = "Hello World";
        Throwable t = new Throwable("Test Throwable");

        logger.i(t, tag, "Hello %s", "World");

        assertLogged(INFO, tag, expectedMessage, t);
    }

    @Test
    public void warningLoggedCorrectly() {
        String expectedMessage = "Hello World";

        logger.w(tag, "Hello %s", "World");

        assertLogged(WARN, tag, expectedMessage, null);
    }

    @Test
    public void warningWithThrowableLoggedCorrectly() {
        String expectedMessage = "Hello World";
        Throwable t = new Throwable("Test Throwable");

        logger.w(t, tag, "Hello %s", "World");

        assertLogged(WARN, tag, expectedMessage, t);
    }

    @Test
    public void errorLoggedCorrectly() {
        String expectedMessage = "Hello World";

        logger.e(tag, "Hello %s", "World");

        assertLogged(ERROR, tag, expectedMessage, null);
    }

    @Test
    public void errorWithThrowableLoggedCorrectly() {
        String expectedMessage = "Hello World";
        Throwable t = new Throwable("Test Throwable");

        logger.e(t, tag, "Hello %s", "World");

        assertLogged(ERROR, tag, expectedMessage, t);
    }

    private void assertLogged(int type, String tag, String msg, Throwable throwable) {
        ShadowLog.LogItem lastLog = ShadowLog.getLogs().get(0);
        assertEquals(type, lastLog.type);
        assertEquals(msg, lastLog.msg);
        assertEquals(tag, lastLog.tag);
        assertEquals(throwable, lastLog.throwable);
    }
}
