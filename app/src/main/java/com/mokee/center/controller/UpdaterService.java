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

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import com.lzy.okgo.exception.OkGoException;
import com.lzy.okgo.model.Progress;
import com.lzy.okserver.OkDownload;
import com.lzy.okserver.download.DownloadTask;
import com.mokee.center.R;
import com.mokee.center.activity.MainActivity;
import com.mokee.center.misc.Constants;
import com.mokee.center.util.CommonUtil;
import com.mokee.center.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.net.ProtocolException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.text.NumberFormat;

public class UpdaterService extends Service {

    private static final String TAG = UpdaterService.class.getName();

    public static final String ACTION_DOWNLOAD_CONTROL = "action_download_control";
    public static final String EXTRA_DOWNLOAD_ID = "extra_download_id";
    public static final String EXTRA_DOWNLOAD_CONTROL = "extra_download_control";

    private static final String ONGOING_NOTIFICATION_CHANNEL =
            "ongoing_notification_channel";

    public static final int DOWNLOAD_RESUME = 0;
    public static final int DOWNLOAD_PAUSE = 1;

    public static final int NOTIFICATION_ID = 10;

    private final IBinder mBinder = new LocalBinder();
    private boolean mHasClients;

    private BroadcastReceiver mBroadcastReceiver;
    private NotificationCompat.Builder mNotificationBuilder;
    private NotificationManager mNotificationManager;
    private NotificationCompat.BigTextStyle mNotificationStyle;

    private UpdaterController mUpdaterController;
    private OkDownload mOkDownload;
    private ConnectivityManager mConnectivityManager;

    @Override
    public void onCreate() {
        super.onCreate();
        mOkDownload = OkDownload.getInstance();
        mUpdaterController = UpdaterController.getInstance(this);
        mConnectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationChannel notificationChannel = new NotificationChannel(
                ONGOING_NOTIFICATION_CHANNEL,
                getString(R.string.ongoing_channel_title),
                NotificationManager.IMPORTANCE_LOW);
        mNotificationManager.createNotificationChannel(notificationChannel);
        mNotificationBuilder = new NotificationCompat.Builder(this,
                ONGOING_NOTIFICATION_CHANNEL);
        mNotificationBuilder.setSmallIcon(R.drawable.ic_system_update);
        mNotificationBuilder.setShowWhen(false);
        mNotificationStyle = new NotificationCompat.BigTextStyle();
        mNotificationBuilder.setStyle(mNotificationStyle);

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent intent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        mNotificationBuilder.setContentIntent(intent);

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String downloadId = intent.getStringExtra(UpdaterController.EXTRA_DOWNLOAD_ID);
                if (UpdaterController.ACTION_UPDATE_STATUS.equals(intent.getAction())) {
                    DownloadTask downloadTask = mOkDownload.getTask(downloadId);
                    setNotificationTitle(downloadTask.progress.fileName);
                    Bundle extras = new Bundle();
                    extras.putString(UpdaterController.EXTRA_DOWNLOAD_ID, downloadId);
                    mNotificationBuilder.setExtras(extras);
                    handleUpdateStatusChange(downloadTask);
                } else if (UpdaterController.ACTION_DOWNLOAD_PROGRESS.equals(intent.getAction())) {
                    DownloadTask downloadTask = mOkDownload.getTask(downloadId);
                    handleDownloadProgressChange(downloadTask);
                }/* else if (UpdaterController.ACTION_INSTALL_PROGRESS.equals(intent.getAction())) {
                    UpdateInfo update = mUpdaterController.getUpdate(downloadId);
                    setNotificationTitle(update);
                    handleInstallProgress(update);
                } else if (UpdaterController.ACTION_UPDATE_REMOVED.equals(intent.getAction())) {
                    Bundle extras = mNotificationBuilder.getExtras();
                    if (extras != null && downloadId.equals(
                            extras.getString(UpdaterController.EXTRA_DOWNLOAD_ID))) {
                        mNotificationBuilder.setExtras(null);
                        mNotificationManager.cancel(NOTIFICATION_ID);
                    }
                }*/
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UpdaterController.ACTION_DOWNLOAD_PROGRESS);
//        intentFilter.addAction(UpdaterController.ACTION_INSTALL_PROGRESS);
        intentFilter.addAction(UpdaterController.ACTION_UPDATE_STATUS);
//        intentFilter.addAction(UpdaterController.ACTION_UPDATE_REMOVED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, intentFilter);

        NetworkRequest.Builder req = new NetworkRequest.Builder();
        req.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
        req.addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET);
        req.addTransportType(NetworkCapabilities.TRANSPORT_BLUETOOTH);
        req.addTransportType(NetworkCapabilities.TRANSPORT_VPN);
        boolean warn = CommonUtil.getMainPrefs(this).getBoolean(Constants.PREF_MOBILE_DATA_WARNING, true);
        if (!warn) {
            req.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);
        }
        mConnectivityManager.registerNetworkCallback(req.build(), mNetworkCallback);
    }

    private NetworkCallback mNetworkCallback = new NetworkCallback() {
        @Override
        public void onAvailable(Network network) {
            super.onAvailable(network);
            mConnectivityManager.bindProcessToNetwork(network);
            String downloadId = mUpdaterController.getActiveDownloadTag();
            if (!TextUtils.isEmpty(downloadId)) {
                mOkDownload.getTask(downloadId).start();
            }
        }
    };

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        mConnectivityManager.unregisterNetworkCallback(mNetworkCallback);
        super.onDestroy();
    }

    public class LocalBinder extends Binder {
        public UpdaterService getService() {
            return UpdaterService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        mHasClients = true;
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mHasClients = false;
        tryStopSelf();
        return false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Starting service");
        if (ACTION_DOWNLOAD_CONTROL.equals(intent.getAction())) {
            String downloadId = intent.getStringExtra(EXTRA_DOWNLOAD_ID);
            int action = intent.getIntExtra(EXTRA_DOWNLOAD_CONTROL, -1);
            if (action == DOWNLOAD_RESUME) {
                mUpdaterController.resumeDownload(downloadId);
            } else if (action == DOWNLOAD_PAUSE) {
                mUpdaterController.pauseDownload(downloadId);
            } else {
                Log.e(TAG, "Unknown download action");
            }
        }
        return START_NOT_STICKY;
    }

    public UpdaterController getUpdaterController() {
        return mUpdaterController;
    }

    private void tryStopSelf() {
        Log.i("MOKEEE", "hasActiveDownloads: " + mUpdaterController.hasActiveDownloads());
        if (!mHasClients && !mUpdaterController.hasActiveDownloads()) {
//        if (!mHasClients && !mUpdaterController.hasActiveDownloads() &&
//                !mUpdaterController.isInstallingUpdate()) {
            Log.d(TAG, "Service no longer needed, stopping");
            stopSelf();
        }
    }

    private void handleUpdateStatusChange(DownloadTask downloadTask) {
        Progress progress = downloadTask.progress;
        switch (progress.status) {
            case Progress.NONE:
            case Progress.WAITING: {
                mNotificationBuilder.mActions.clear();
                mNotificationBuilder.setProgress(0, 0, true);
//                mNotificationStyle.setSummaryText(null);
                String text = getString(R.string.download_starting_notification);
                mNotificationStyle.bigText(text);
                mNotificationBuilder.setStyle(mNotificationStyle);
                mNotificationBuilder.setSmallIcon(android.R.drawable.stat_sys_download);
                mNotificationBuilder.setTicker(text);
                mNotificationBuilder.setOngoing(true);
                mNotificationBuilder.setAutoCancel(false);
                startForeground(NOTIFICATION_ID, mNotificationBuilder.build());
                mNotificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build());
                break;
            }
            case Progress.LOADING: {
                String text = getString(R.string.downloading_notification);
                mNotificationStyle.bigText(text);
                mNotificationBuilder.setStyle(mNotificationStyle);
                mNotificationBuilder.setSmallIcon(android.R.drawable.stat_sys_download);
                mNotificationBuilder.addAction(android.R.drawable.ic_media_pause,
                        getString(R.string.action_pause),
                        getPausePendingIntent(progress.tag));
                mNotificationBuilder.setTicker(text);
                mNotificationBuilder.setOngoing(true);
                mNotificationBuilder.setAutoCancel(false);
                mNotificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build());
                break;
            }
            case Progress.PAUSE: {
                stopForeground(STOP_FOREGROUND_DETACH);
                // In case we pause before the first progress update
                mNotificationBuilder.mActions.clear();
                mNotificationBuilder.setProgress((int) progress.totalSize, (int) progress.currentSize, false);
                String text = getString(R.string.download_paused_notification);
                mNotificationStyle.bigText(text);
                mNotificationBuilder.setStyle(mNotificationStyle);
                mNotificationBuilder.setSmallIcon(R.drawable.ic_action_pause);
                mNotificationBuilder.addAction(android.R.drawable.ic_media_play,
                        getString(R.string.action_resume),
                        getResumePendingIntent(progress.tag));
                mNotificationBuilder.setTicker(text);
                mNotificationBuilder.setOngoing(false);
                mNotificationBuilder.setAutoCancel(false);
                mNotificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build());
                tryStopSelf();
                break;
            }
            case Progress.FINISH: {
                stopForeground(STOP_FOREGROUND_DETACH);
                mNotificationBuilder.mActions.clear();
                mNotificationBuilder.setStyle(null);
                mNotificationBuilder.setSmallIcon(R.drawable.ic_system_update);
                mNotificationBuilder.setProgress(0, 0, false);
                String text = getString(R.string.download_completed_notification);
                mNotificationBuilder.setContentText(text);
                mNotificationBuilder.setTicker(text);
                mNotificationBuilder.setOngoing(false);
                mNotificationBuilder.setAutoCancel(true);
                mNotificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build());
                tryStopSelf();
                break;
            }
            case Progress.ERROR: {
                if (progress.exception instanceof ProtocolException
                        || progress.exception instanceof SocketTimeoutException) {
                    downloadTask.start();
                } else if (progress.exception instanceof OkGoException) {
                    downloadTask.restart();
                } else if (progress.exception instanceof SocketException
                        || progress.exception instanceof IOException) {
                    mNotificationBuilder.mActions.clear();
                    mNotificationBuilder.setProgress(0, 0, true);
                    String text = getString(R.string.download_waiting_network_notification);
                    mNotificationStyle.bigText(text);
                    mNotificationBuilder.setStyle(mNotificationStyle);
                    mNotificationBuilder.setSmallIcon(R.drawable.ic_action_pause);
                    mNotificationBuilder.addAction(android.R.drawable.ic_media_play,
                            getString(R.string.action_resume),
                            getResumePendingIntent(progress.tag));
                    mNotificationBuilder.setTicker(text);
                    mNotificationBuilder.setOngoing(true);
                    mNotificationBuilder.setAutoCancel(false);
                    mNotificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build());
                    tryStopSelf();
                    break;
                } else {
                    Log.i("MOKEEE", progress.exception.getMessage());
                    progress.exception.printStackTrace();
                }
            }
        }
    }

    private void handleDownloadProgressChange(DownloadTask downloadTask) {
        Progress progress = downloadTask.progress;
        mNotificationBuilder.setProgress((int) progress.totalSize, (int) progress.currentSize, false);

        String percent = NumberFormat.getPercentInstance().format(progress.fraction);
        mNotificationStyle.setSummaryText(percent);

        setNotificationTitle(progress.fileName);

        mNotificationStyle.bigText(progress.extra1.toString());

        mNotificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build());
    }

//    private void handleInstallProgress(UpdateInfo update) {
//        setNotificationTitle(update);
//        int progress = update.getInstallProgress();
//        mNotificationBuilder.setProgress(100, progress, false);
//        String percent = NumberFormat.getPercentInstance().format(progress / 100.f);
//        mNotificationStyle.setSummaryText(percent);
//        boolean notAB = UpdateInstaller.isInstalling();
//        mNotificationStyle.bigText(notAB ? getString(R.string.dialog_prepare_zip_message) :
//                update.getFinalizing() ?
//                        getString(R.string.finalizing_package) :
//                        getString(R.string.preparing_ota_first_boot));
//        mNotificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build());
//    }

    private void setNotificationTitle(String fileName) {
        mNotificationStyle.setBigContentTitle(fileName);
        mNotificationBuilder.setContentTitle(fileName);
    }

    private PendingIntent getResumePendingIntent(String downloadId) {
        final Intent intent = new Intent(this, UpdaterService.class);
        intent.setAction(ACTION_DOWNLOAD_CONTROL);
        intent.putExtra(EXTRA_DOWNLOAD_ID, downloadId);
        intent.putExtra(EXTRA_DOWNLOAD_CONTROL, DOWNLOAD_RESUME);
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent getPausePendingIntent(String downloadId) {
        final Intent intent = new Intent(this, UpdaterService.class);
        intent.setAction(ACTION_DOWNLOAD_CONTROL);
        intent.putExtra(EXTRA_DOWNLOAD_ID, downloadId);
        intent.putExtra(EXTRA_DOWNLOAD_CONTROL, DOWNLOAD_PAUSE);
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    //    private PendingIntent getRebootPendingIntent() {
//        final Intent intent = new Intent(this, UpdaterReceiver.class);
//        intent.setAction(UpdaterReceiver.ACTION_INSTALL_REBOOT);
//        return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
//    }

}
