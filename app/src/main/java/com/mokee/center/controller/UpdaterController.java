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

package com.mokee.center.controller;

import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.DateUtils;
import android.text.format.Formatter;
import android.util.Log;

import com.lzy.okgo.OkGo;
import com.lzy.okgo.db.DownloadManager;
import com.lzy.okgo.model.Progress;
import com.lzy.okgo.request.GetRequest;
import com.lzy.okserver.OkDownload;
import com.lzy.okserver.download.DownloadListener;
import com.lzy.okserver.download.DownloadTask;
import com.mokee.center.R;
import com.mokee.center.misc.State;
import com.mokee.center.model.UpdateInfo;
import com.mokee.center.util.CommonUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class UpdaterController {

    public static final String ACTION_DOWNLOAD_PROGRESS = "action_download_progress";
    public static final String ACTION_INSTALL_PROGRESS = "action_install_progress";
    public static final String ACTION_UPDATE_REMOVED = "action_update_removed";
    public static final String ACTION_UPDATE_STATUS = "action_update_status_change";
    public static final String EXTRA_DOWNLOAD_ID = "extra_download_id";

    private final String TAG = UpdaterController.class.getName();

    private static UpdaterController sUpdaterController;

    private final Context mContext;
    private final LocalBroadcastManager mBroadcastManager;
    private OkDownload mOkDownload;

    private final PowerManager.WakeLock mWakeLock;

    private int mActiveDownloads = 0;
    private String mActiveDownloadTag;

    public static synchronized UpdaterController getInstance() {
        return sUpdaterController;
    }

    protected static synchronized UpdaterController getInstance(Context context) {
        if (sUpdaterController == null) {
            sUpdaterController = new UpdaterController(context);
        }
        return sUpdaterController;
    }

    private UpdaterController(Context context) {
        mBroadcastManager = LocalBroadcastManager.getInstance(context);
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "system:Updater");
        mWakeLock.setReferenceCounted(false);
        mContext = context.getApplicationContext();
        mOkDownload = OkDownload.getInstance();

//        CommonUtils.cleanupDownloadsDir(context);

        Map<String, DownloadTask> downloadTaskMap = getDownloadTaskMap();
        for (UpdateInfo updateInfo : State.loadState(CommonUtils.getCachedUpdateList(context))) {
            DownloadTask downloadTask = downloadTaskMap.get(updateInfo.getName());
            if (downloadTask != null) {
                if (!new File(downloadTask.progress.filePath).exists()) {
                    downloadTask.remove();
                } else {
                    updateInfo.setProgress(downloadTask.progress);
                }
            }
            mAvailableUpdates.put(updateInfo.getName(), updateInfo);
        }

    }

    public LinkedList<UpdateInfo> getUpdates() {
        LinkedList<UpdateInfo> availableUpdates = new LinkedList<>();
        for (UpdateInfo updateInfo : mAvailableUpdates.values()) {
            availableUpdates.add(updateInfo);
        }
        return availableUpdates;
    }

    private Map<String, UpdateInfo> mAvailableUpdates = new LinkedHashMap<>();

    public void setUpdatesAvailableOnline(List<String> downloadIds) {
        for (Iterator<Entry<String, UpdateInfo>> iterator = mAvailableUpdates.entrySet().iterator(); iterator.hasNext();) {
            Entry<String, UpdateInfo> item = iterator.next();
            if (!downloadIds.contains(item.getKey())) {
                Log.d(TAG, item.getKey() + " no longer available online, removing");
                iterator.remove();
            }
        }
    }

    public boolean addUpdate(UpdateInfo updateInfo) {
        Log.d(TAG, "Adding download: " + updateInfo.getName());
        if (mAvailableUpdates.containsKey(updateInfo.getName())) {
            Log.d(TAG, "Download (" + updateInfo.getName() + ") already added");
            UpdateInfo updateAdded = mAvailableUpdates.get(updateInfo.getName());
            updateAdded.setDownloadUrl(updateInfo.getDownloadUrl());
            updateAdded.setChangelogUrl(updateInfo.getChangelogUrl());
            return false;
        }
        mAvailableUpdates.put(updateInfo.getName(), updateInfo);
        return true;
    }

    void notifyUpdateChange(String downloadId) {
        Intent intent = new Intent();
        intent.setAction(ACTION_UPDATE_STATUS);
        intent.putExtra(EXTRA_DOWNLOAD_ID, downloadId);
        mBroadcastManager.sendBroadcast(intent);
    }

    void notifyDownloadProgress(String downloadId) {
        Intent intent = new Intent();
        intent.setAction(ACTION_DOWNLOAD_PROGRESS);
        intent.putExtra(EXTRA_DOWNLOAD_ID, downloadId);
        mBroadcastManager.sendBroadcast(intent);
    }

    private void tryReleaseWakelock() {
        if (!hasActiveDownloads()) {
            mWakeLock.release();
        }
    }

    public void startDownload(String downloadId) {
        Log.d(TAG, "Starting " + downloadId);
        GetRequest<File> request = OkGo.get(mAvailableUpdates.get(downloadId).getDownloadUrl());
        DownloadTask task = OkDownload.request(mAvailableUpdates.get(downloadId).getName(), request).save().register(new LogDownloadListener());
        task.start();
        mAvailableUpdates.get(downloadId).setProgress(task.progress);
    }

    public void resumeDownload(String downloadId) {
        Log.d(TAG, "Resuming " + downloadId);
        mOkDownload.getTask(downloadId).register(new LogDownloadListener()).start();
    }

    public void pauseDownload(String downloadId) {
        Log.d(TAG, "Pausing " + downloadId);
        mOkDownload.getTask(downloadId).pause();
        mActiveDownloads--;
        mActiveDownloadTag = null;
    }

    public String getActiveDownloadTag() {
        return mActiveDownloadTag;
    }

    public boolean hasActiveDownloads() {
        return mActiveDownloads > 0;
    }

    public class LogDownloadListener extends DownloadListener {

        private long mLastUpdate = 0;
        private int mStatus = 0;

        public LogDownloadListener() {
            super(LogDownloadListener.class.getName());
        }

        @Override
        public void onStart(Progress progress) {
            mWakeLock.acquire();
            mActiveDownloads++;
            mActiveDownloadTag = progress.tag;
        }

        @Override
        public void onProgress(Progress progress) {
            if (mStatus != progress.status) {
                Log.i("MOKEEE", "Status changed: " + mStatus + " to " + String.valueOf(progress.status));
                mStatus = progress.status;
                notifyUpdateChange(progress.tag);
            } else {
                final long now = SystemClock.elapsedRealtime();
                if (now - DateUtils.SECOND_IN_MILLIS >= mLastUpdate) {
                    mLastUpdate = now;

                    long spendTime = (System.currentTimeMillis() - progress.date) / DateUtils.SECOND_IN_MILLIS;
                    long speed = progress.speed != 0 ? progress.speed : progress.currentSize / spendTime;
                    if (speed == 0) return;

                    CharSequence eta = CommonUtils.calculateEta(mContext, speed, progress.totalSize, progress.currentSize);
                    CharSequence etaWithSpeed = mContext.getString(R.string.download_speed, eta, Formatter.formatFileSize(mContext, speed));
                    progress.extra1 = etaWithSpeed.toString();

                    notifyDownloadProgress(progress.tag);
                }
            }
        }

        @Override
        public void onError(Progress progress) {
        }

        @Override
        public void onFinish(File file, Progress progress) {
            tryReleaseWakelock();
            mActiveDownloads--;
        }

        @Override
        public void onRemove(Progress progress) {
        }
    }

    private Map<String, DownloadTask> getDownloadTaskMap() {
        Map<String, DownloadTask> downloadTaskMap = new HashMap<>();
        List<DownloadTask> downloadTasks = OkDownload.restore(DownloadManager.getInstance().getAll());
        for (Iterator iterator = downloadTasks.iterator(); iterator.hasNext(); ) {
            DownloadTask task = (DownloadTask) iterator.next();
            downloadTaskMap.put(task.progress.tag, task);
        }
        return downloadTaskMap;
    }

}
