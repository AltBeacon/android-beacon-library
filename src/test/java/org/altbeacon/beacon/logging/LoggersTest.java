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

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;

/**
 * Ensure correct instances are returned from factory methods.
 *
 * @author Andrew Reitz
 */
public class LoggersTest {
    @Test
    public void loggerReturnsDebugInstance() {
        Logger logger = Loggers.debug();

        assertThat(logger, instanceOf(DebugLogger.class));
    }

    @Test
    public void debugLoggerReturnsSameInstance() {
        Logger logger1 = Loggers.debug();
        Logger logger2 = Loggers.debug();

        assertThat(logger1, sameInstance(logger2));
    }

    @Test
    public void loggerReturnsEmptyInstance() {
        Logger logger = Loggers.empty();

        assertThat(logger, instanceOf(EmptyLogger.class));
    }

    @Test
    public void emptyLoggerReturnsSameInstance() {
        Logger logger1 = Loggers.empty();
        Logger logger2 = Loggers.empty();

        assertThat(logger1, sameInstance(logger2));
    }
}
