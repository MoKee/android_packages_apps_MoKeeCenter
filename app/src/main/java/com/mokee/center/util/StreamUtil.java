/*
 * Copyright (C) 2018-2019 The MoKee Open Source Project
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

import android.icu.text.MeasureFormat;
import android.icu.text.MeasureFormat.FormatWidth;
import android.icu.util.Measure;
import android.icu.util.MeasureUnit;

import com.mokee.utils.HashUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

public class StreamUtil {

    private static final int BUF_SIZE = 16 * 1024;

    public static final long SECOND_IN_MILLIS = 1000;
    public static final long MINUTE_IN_MILLIS = SECOND_IN_MILLIS * 60;
    public static final long HOUR_IN_MILLIS = MINUTE_IN_MILLIS * 60;

    public static final int LENGTH_LONG = 10;
    public static final int LENGTH_MEDIUM = 20;
    public static final int LENGTH_SHORT = 30;
    public static final int LENGTH_SHORTER = 40;
    public static final int LENGTH_SHORTEST = 50;

    public static String calculateMd5(InputStream inputSource) throws IOException {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("md5");
        } catch (NoSuchAlgorithmException e) {
            // This should not happen
            throw new RuntimeException(e);
        }
        InputStream input = new BufferedInputStream(new DigestInputStream(inputSource, md));
        byte[] buf = new byte[BUF_SIZE];
        while (input.read(buf) != -1) {
            // Read through the stream to update digest.
        }
        input.close();
        return HashUtils.toHex(md.digest());
    }

    public static CharSequence formatDuration(long millis) {
        return formatDuration(millis, LENGTH_LONG);
    }

    public static CharSequence formatDuration(long millis, int abbrev) {
        final FormatWidth width;
        switch (abbrev) {
            case LENGTH_LONG:
                width = FormatWidth.WIDE;
                break;
            case LENGTH_SHORT:
            case LENGTH_SHORTER:
            case LENGTH_MEDIUM:
                width = FormatWidth.SHORT;
                break;
            case LENGTH_SHORTEST:
                width = FormatWidth.NARROW;
                break;
            default:
                width = FormatWidth.WIDE;
        }
        final MeasureFormat formatter = MeasureFormat.getInstance(Locale.getDefault(), width);
        if (millis >= HOUR_IN_MILLIS) {
            final int hours = (int) ((millis + 1800000) / HOUR_IN_MILLIS);
            return formatter.format(new Measure(hours, MeasureUnit.HOUR));
        } else if (millis >= MINUTE_IN_MILLIS) {
            final int minutes = (int) ((millis + 30000) / MINUTE_IN_MILLIS);
            return formatter.format(new Measure(minutes, MeasureUnit.MINUTE));
        } else {
            final int seconds = (int) ((millis + 500) / SECOND_IN_MILLIS);
            return formatter.format(new Measure(seconds, MeasureUnit.SECOND));
        }
    }
}
