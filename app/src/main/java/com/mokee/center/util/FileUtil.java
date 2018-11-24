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

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Locale;

public class FileUtil {

    public static File getDownloadPath() {
        return new File(Environment.getExternalStorageDirectory(), "mokee_updates");
    }

    public static File getCachedUpdateList(Context context) {
        return new File(context.getCacheDir(), "updates.cached");
    }

    public static boolean checkMd5(String md5, File file) {
        try {
            return TextUtils.equals(md5, calculateMd5(file));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String calculateMd5(File file) throws IOException {
        FileInputStream inputSource = new FileInputStream(file);
        return StreamUtil.calculateMd5(inputSource).toUpperCase(Locale.ENGLISH);
    }

}
