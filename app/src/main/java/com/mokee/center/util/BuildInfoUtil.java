/*
 * Copyright (C) 2018 The MoKee Open Source Project
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

import android.text.TextUtils;

import com.mokee.os.Build;

import java.util.Locale;

public class BuildInfoUtil {

    public static long getBuildDate(String version) {
        String[] info = version.split("-");
        String date;
        if (isIncrementalUpdate(version)) {
            date = info[4];
        } else {
            date = info[2];
        }
        if (!TextUtils.isDigitsOnly(date)) {
            return 0;
        } else {
            return Long.valueOf(date);
        }
    }

    public static float getReleaseCode(String version) {
        String[] info = version.split("-");
        String code;
        if (isIncrementalUpdate(version)) {
            code = info[1];
        } else {
            code = info[0];
        }
        if (!code.toLowerCase(Locale.ENGLISH).startsWith("mk")) {
            return 0;
        } else {
            return Float.valueOf(code.substring(2, code.length()));
        }
    }

    public static boolean isIncrementalUpdate(String version) {
        return version.toLowerCase(Locale.ENGLISH).startsWith("ota");
    }

    public static String getSuggestUpdateType() {
        switch (Build.RELEASE_TYPE.toLowerCase(Locale.ENGLISH)) {
            case "release":
                return "0";
            case "unofficial":
                return "3";
            default:
                return "1";
        }
    }
}
