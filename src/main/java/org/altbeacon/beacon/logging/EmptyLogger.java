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

/**
 * A logger that doesn't do anything.
 *
 * @author Android Reitz
 * @since 2.2
 */
final class EmptyLogger implements Logger {

    @Override
    public void v(String tag, String message, Object... args) {

    }

    @Override
    public void v(Throwable t, String tag, String message, Object... args) {

    }

    @Override
    public void d(String tag, String message, Object... args) {

    }

    @Override
    public void d(Throwable t, String tag, String message, Object... args) {

    }

    @Override
    public void i(String tag, String message, Object... args) {

    }

    @Override
    public void i(Throwable t, String tag, String message, Object... args) {

    }

    @Override
    public void w(String tag, String message, Object... args) {

    }

    @Override
    public void w(Throwable t, String tag, String message, Object... args) {

    }

    @Override
    public void e(String tag, String message, Object... args) {

    }

    @Override
    public void e(Throwable t, String tag, String message, Object... args) {

    }
}
