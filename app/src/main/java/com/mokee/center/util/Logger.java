/*
 * Copyright (C) 2019 The MoKee Open Source Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mokee.center.util;

import android.util.Log;

import com.mokee.center.BuildConfig;

public class Logger {

    // 设为 false 关闭日志
    private static final boolean LOG_ENABLE = BuildConfig.DEBUG;

    public static void i(String tag, String msg) {
        if (LOG_ENABLE) Log.i(tag, msg);
    }

    public static void v(String tag, String msg) {
        if (LOG_ENABLE) Log.v(tag, msg);
    }

    public static void d(String tag, String msg) {
        if (LOG_ENABLE) Log.d(tag, msg);
    }

    public static void w(String tag, String msg) {
        if (LOG_ENABLE) Log.w(tag, msg);
    }

    public static void e(String tag, String msg) {
        if (LOG_ENABLE) Log.e(tag, msg);
    }
}
