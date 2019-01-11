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

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.verify;

/**
 * Tests for LogManager, ensuring correct delegation and expectations are met.
 *
 * @author Andrew Reitz
 */
public class LogManagerTest {

    @Mock
    Logger logger;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        LogManager.setLogger(logger);
    }

    @Test(expected = NullPointerException.class)
    public void canNotSetNullLogger() {
        LogManager.setLogger(null);
    }

    @Test
    public void verbose() {
        String tag = "TestTag";
        String message = "Test message";

        LogManager.v(tag, message);

        verify(logger).v(tag, message);
    }

    @Test
    public void verboseWithThrowable() {
        String tag = "TestTag";
        String message = "Test message";
        Throwable t = new Throwable();

        LogManager.v(t, tag, message);

        verify(logger).v(t, tag, message);
    }

    @Test
    public void debug() {
        String tag = "TestTag";
        String message = "Test message";

        LogManager.d(tag, message);

        verify(logger).d(tag, message);
    }

    @Test
    public void debugWithThrowable() {
        String tag = "TestTag";
        String message = "Test message";
        Throwable t = new Throwable();

        LogManager.d(t, tag, message);

        verify(logger).d(t, tag, message);
    }

    @Test
    public void info() {
        String tag = "TestTag";
        String message = "Test message";

        LogManager.i(tag, message);

        verify(logger).i(tag, message);
    }

    @Test
    public void infoWithThrowable() {
        String tag = "TestTag";
        String message = "Test message";
        Throwable t = new Throwable();

        LogManager.i(t, tag, message);

        verify(logger).i(t, tag, message);
    }

    @Test
    public void warning() {
        String tag = "TestTag";
        String message = "Test message";

        LogManager.w(tag, message);

        verify(logger).w(tag, message);
    }

    @Test
    public void warningWithThrowable() {
        String tag = "TestTag";
        String message = "Test message";
        Throwable t = new Throwable();

        LogManager.w(t, tag, message);

        verify(logger).w(t, tag, message);
    }

    @Test
    public void error() {
        String tag = "TestTag";
        String message = "Test message";

        LogManager.e(tag, message);

        verify(logger).e(tag, message);
    }

    @Test
    public void errorWithThrowable() {
        String tag = "TestTag";
        String message = "Test message";
        Throwable t = new Throwable();

        LogManager.e(t, tag, message);

        verify(logger).e(t, tag, message);
    }
}
