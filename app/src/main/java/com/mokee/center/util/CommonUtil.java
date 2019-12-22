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

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.SystemProperties;
import android.text.format.DateUtils;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.google.android.material.snackbar.Snackbar;
import com.lzy.okgo.db.DownloadManager;
import com.lzy.okserver.OkDownload;
import com.lzy.okserver.download.DownloadTask;
import com.mokee.center.R;
import com.mokee.center.controller.UpdaterService;
import com.mokee.center.misc.Constants;
import com.mokee.center.misc.State;
import com.mokee.center.model.UpdateInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipFile;

public class CommonUtil {

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        return !(info == null || !info.isConnected() || !info.isAvailable());
    }

    public static boolean isOnWifiOrEthernet(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        return (info != null && (info.getType() == ConnectivityManager.TYPE_ETHERNET
                || info.getType() == ConnectivityManager.TYPE_WIFI));
    }

    public static void cleanupDownloadsDir(Context context) {
        SharedPreferences mMainPrefs = getMainPrefs(context);
        boolean deleteUpdates = mMainPrefs.getBoolean(Constants.PREF_AUTO_DELETE_UPDATES, false);
        if (deleteUpdates) {
            Map<String, DownloadTask> downloadTaskMap = CommonUtil.getDownloadTaskMap();
            for (String version : downloadTaskMap.keySet()) {
                if (!BuildInfoUtil.isCompatible(version)) {
                    downloadTaskMap.get(version).remove(true);
                }
            }
        }
    }

    public static void openLink(Activity context, String url) {
        Uri uri = Uri.parse(url);
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            context.startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            Snackbar.make(context.findViewById(R.id.updater), R.string.browser_not_found, Snackbar.LENGTH_LONG).show();
        }
    }

    public static boolean checkForNewUpdates(File oldJson, File newJson) {
        List<UpdateInfo> oldList = State.loadState(oldJson);
        List<UpdateInfo> newList = State.loadState(newJson);
        Set<String> oldUpdates = new HashSet<>();
        for (UpdateInfo update : oldList) {
            oldUpdates.add(update.getName());
        }
        // In case of no new updates, the old list should
        // have all (if not more) the updates
        for (UpdateInfo update : newList) {
            if (!oldUpdates.contains(update.getName())) {
                return true;
            }
        }
        return false;
    }

    public static int compare(String o1, String o2) {
        float codeo1 = BuildInfoUtil.getReleaseCode(o1);
        float codeo2 = BuildInfoUtil.getReleaseCode(o2);
        if (codeo2 - codeo1 == 0) {
            return Long.compare(BuildInfoUtil.getBuildDate(o2), BuildInfoUtil.getBuildDate(o1));
        } else {
            return Float.compare(codeo1, codeo2);
        }
    }

    private static LinkedList<UpdateInfo> getSortedUpdates(LinkedList<UpdateInfo> updates) {
        Collections.sort(updates, (o1, o2) -> compare(o1.getName(), o2.getName()));
        return updates;
    }

    public static LinkedList<UpdateInfo> parseJson(Context context, String json, String tag) throws JSONException {
        LinkedList<UpdateInfo> updates = new LinkedList<>();
        JSONArray updatesList = new JSONArray(json);
        for (int i = 0; i < updatesList.length(); i++) {
            if (updatesList.isNull(i)) {
                continue;
            }
            try {
                UpdateInfo updateInfo = parseJsonUpdate(context, updatesList.getJSONObject(i));
                if (updateInfo != null) {
                    updates.add(updateInfo);
                }
            } catch (JSONException e) {
                Log.e(tag, "Could not parse update object, index=" + i, e);
            }
        }
        return CommonUtil.getSortedUpdates(updates);
    }

    private static UpdateInfo parseJsonUpdate(Context context, JSONObject object) throws JSONException {
        return new UpdateInfo.Builder()
                .setName(object.getString("name"))
                .setDisplayVersion(BuildInfoUtil.getDisplayVersion(context, object.getString("name")))
                .setMD5Sum(object.getString("md5"))
                .setDiffSize(object.getLong("diff"))
                .setFileSize(object.getLong("length"))
                .setTimestamp(object.getLong("timestamp"))
                .setDownloadUrl(object.getString("url")).build();
    }

    public static CharSequence calculateEta(Context context, long speed, long totalBytes, long totalBytesRead) {
        return context.getString(R.string.download_remaining, DateUtils.formatDuration((totalBytes - totalBytesRead) / speed * 1000));
    }

    public static boolean isABDevice() {
        return SystemProperties.getBoolean(Constants.PROP_AB_DEVICE, false);
    }

    public static boolean isABUpdate(ZipFile zipFile) {
        return zipFile.getEntry(Constants.AB_PAYLOAD_BIN_PATH) != null &&
                zipFile.getEntry(Constants.AB_PAYLOAD_PROPERTIES_PATH) != null;
    }

    public static boolean isABUpdate(File file) throws IOException {
        ZipFile zipFile = new ZipFile(file);
        boolean isAB = isABUpdate(zipFile);
        zipFile.close();
        return isAB;
    }

    public static void triggerUpdate(Context context, String downloadId) {
        final Intent intent = new Intent(context, UpdaterService.class);
        intent.setAction(UpdaterService.ACTION_INSTALL_UPDATE);
        intent.putExtra(UpdaterService.EXTRA_DOWNLOAD_ID, downloadId);
        context.startService(intent);
    }

    public static Map<String, DownloadTask> getDownloadTaskMap() {
        Map<String, DownloadTask> downloadTaskMap = new HashMap<>();
        List<DownloadTask> downloadTasks = OkDownload.restore(DownloadManager.getInstance().getAll());
        for (DownloadTask task : downloadTasks) {
            downloadTaskMap.put(task.progress.tag, task);
        }
        return downloadTaskMap;
    }

    public static SharedPreferences getDonationPrefs(Context context) {
        return context.getSharedPreferences(Constants.DONATION_PREF, Context.MODE_PRIVATE);
    }

    public static SharedPreferences getMainPrefs(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }
}
