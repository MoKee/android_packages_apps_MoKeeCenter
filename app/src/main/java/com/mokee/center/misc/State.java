/*
 * Copyright (C) 2014-2018 The MoKee Open Source Project
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

package com.mokee.center.misc;

import android.util.Log;

import com.mokee.center.model.UpdateInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedList;

public class State {

    private static final String TAG = "State";

    public static void saveState(LinkedList<UpdateInfo> availableUpdates, File file) {
        ObjectOutputStream oos = null;
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            oos = new ObjectOutputStream(fos);
            oos.writeObject(availableUpdates);
            oos.flush();
        } catch (IOException e) {
            Log.e(TAG, "Exception on saving instance state", e);
        } finally {
            try {
                if (oos != null) {
                    oos.close();
                }
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                // ignored, can't do anything anyway
            }
        }
    }

    public static LinkedList<UpdateInfo> loadState(File file) {
        LinkedList<UpdateInfo> availableUpdates = new LinkedList<UpdateInfo>();
        ObjectInputStream ois = null;
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            ois = new ObjectInputStream(fis);

            Object o = ois.readObject();
            if (o != null && o instanceof LinkedList<?>) {
                availableUpdates = (LinkedList<UpdateInfo>) o;
            }
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "Unable to load stored class", e);
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "Unexpected state file format", e);
        } catch (FileNotFoundException e) {
            Log.i(TAG, "No state info stored");
        } catch (IOException e) {
            Log.e(TAG, "Exception on loading state", e);
        } finally {
            try {
                if (ois != null) {
                    ois.close();
                }
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException e) {
                // ignored, can't do anything anyway
            }
        }
        return availableUpdates;
    }
}
