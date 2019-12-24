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

import com.mokee.utils.HashUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class StreamUtil {

    private static final int BUF_SIZE = 16 * 1024;

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

}
