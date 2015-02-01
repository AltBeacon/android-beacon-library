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
 * Ensure the debug logger logs correctly.
 *
 * @author Andrew Reitz
 */
@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class DebugLoggerTest {
    Logger logger = new DebugLogger();

    @Test
    public void verboseLoggedCorrectly() {
        String expectedTag = "TestTag";
        String expectedMessage = "Hello World";

        logger.v(expectedTag, "Hello %s", "World");

        assertLogged(VERBOSE, expectedTag, expectedMessage, null);
    }

    @Test
    public void verboseWithThrowableLoggedCorrectly() {
        String expectedTag = "TestTag";
        String expectedMessage = "Hello World";
        Throwable t = new Throwable("Test Throwable");

        logger.v(t, expectedTag, "Hello %s", "World");

        assertLogged(VERBOSE, expectedTag, expectedMessage, t);
    }

    @Test
    public void debugLoggedCorrectly() {
        String expectedTag = "TestTag";
        String expectedMessage = "Hello World";

        logger.d(expectedTag, "Hello %s", "World");

        assertLogged(DEBUG, expectedTag, expectedMessage, null);
    }

    @Test
    public void debugWithThrowableLoggedCorrectly() {
        String expectedTag = "TestTag";
        String expectedMessage = "Hello World";
        Throwable t = new Throwable("Test Throwable");

        logger.d(t, expectedTag, "Hello %s", "World");

        assertLogged(DEBUG, expectedTag, expectedMessage, t);
    }

    @Test
    public void infoLoggedCorrectly() {
        String expectedTag = "TestTag";
        String expectedMessage = "Hello World";

        logger.v(expectedTag, "Hello %s", "World");

        assertLogged(VERBOSE, expectedTag, expectedMessage, null);
    }

    @Test
    public void infoWithThrowableLoggedCorrectly() {
        String expectedTag = "TestTag";
        String expectedMessage = "Hello World";
        Throwable t = new Throwable("Test Throwable");

        logger.i(t, expectedTag, "Hello %s", "World");

        assertLogged(INFO, expectedTag, expectedMessage, t);
    }

    @Test
    public void warningLoggedCorrectly() {
        String expectedTag = "TestTag";
        String expectedMessage = "Hello World";

        logger.w(expectedTag, "Hello %s", "World");

        assertLogged(WARN, expectedTag, expectedMessage, null);
    }

    @Test
    public void warningWithThrowableLoggedCorrectly() {
        String expectedTag = "TestTag";
        String expectedMessage = "Hello World";
        Throwable t = new Throwable("Test Throwable");

        logger.w(t, expectedTag, "Hello %s", "World");

        assertLogged(WARN, expectedTag, expectedMessage, t);
    }

    @Test
    public void errorLoggedCorrectly() {
        String expectedTag = "TestTag";
        String expectedMessage = "Hello World";

        logger.e(expectedTag, "Hello %s", "World");

        assertLogged(ERROR, expectedTag, expectedMessage, null);
    }

    @Test
    public void errorWithThrowableLoggedCorrectly() {
        String expectedTag = "TestTag";
        String expectedMessage = "Hello World";
        Throwable t = new Throwable("Test Throwable");

        logger.e(t, expectedTag, "Hello %s", "World");

        assertLogged(ERROR, expectedTag, expectedMessage, t);
    }

    private void assertLogged(int type, String tag, String msg, Throwable throwable) {
        ShadowLog.LogItem lastLog = ShadowLog.getLogs().get(0);
        assertEquals(type, lastLog.type);
        assertEquals(msg, lastLog.msg);
        assertEquals(tag, lastLog.tag);
        assertEquals(throwable, lastLog.throwable);
    }
}
