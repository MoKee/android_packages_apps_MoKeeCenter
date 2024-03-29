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

package com.mokee.center.preference;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.BatteryManager;
import android.os.PowerManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.material.snackbar.Snackbar;
import com.mokee.center.MKCenterApplication;
import com.mokee.center.R;
import com.mokee.center.controller.UpdaterController;
import com.mokee.center.controller.UpdaterService;
import com.mokee.center.misc.Constants;
import com.mokee.center.model.UpdateInfo;
import com.mokee.center.util.CommonUtil;

import java.io.IOException;
import java.util.LinkedList;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceViewHolder;

public class AvailableUpdatesPreferenceCategory extends PreferenceCategory implements UpdatePreference.OnActionListener {

    private static final String TAG = AvailableUpdatesPreferenceCategory.class.getSimpleName();

    private static final int BATTERY_PLUGGED_ANY = BatteryManager.BATTERY_PLUGGED_AC
            | BatteryManager.BATTERY_PLUGGED_USB | BatteryManager.BATTERY_PLUGGED_WIRELESS;

    private UpdaterController mUpdaterController;
    private View mItemView;
    private InterstitialAd mDownloadInterstitialAd;

    public AvailableUpdatesPreferenceCategory(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setUpdaterController(UpdaterController updaterController) {
        mUpdaterController = updaterController;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        mItemView = holder.itemView;
        if (!MKCenterApplication.getInstance().getDonationInfo().isAdvanced()) {
            mDownloadInterstitialAd = new InterstitialAd(getContext());
            mDownloadInterstitialAd.setAdUnitId(getContext().getString(R.string.interstitial_ad_unit_id));
            mDownloadInterstitialAd.setAdListener(new AdListener() {
                @Override
                public void onAdClosed() {
                    mDownloadInterstitialAd.loadAd(new AdRequest.Builder().build());
                    if (!MKCenterApplication.getInstance().getDonationInfo().isBasic()) {
                        Snackbar.make(mItemView, getContext().getString(R.string.download_limited_speed, Constants.DONATION_BASIC), Snackbar.LENGTH_LONG).show();
                    }
                }
            });
            try {
                mDownloadInterstitialAd.loadAd(new AdRequest.Builder().build());
            } catch (ActivityNotFoundException e) {
                Toast.makeText(getContext(), R.string.ad_blocker_detected, Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        }
    }

    public void setInterstitialAd() {
        if (MKCenterApplication.getInstance().getDonationInfo().isAdvanced()) {
            mDownloadInterstitialAd = null;
        }
    }

    public void setPendingListPreferences() {
        removeAll();
        PendingListPreference pendingListPreference = new PendingListPreference(getContext());
        addPreference(pendingListPreference);
    }

    public void refreshPreferences() {
        removeAll();
        LinkedList<UpdateInfo> availableUpdates = mUpdaterController.getUpdates();
        if (availableUpdates != null && availableUpdates.size() > 0) {
            for (UpdateInfo updateInfo : availableUpdates) {
                UpdatePreference updatePreference = new UpdatePreference(getContext());
                updatePreference.setTitle(updateInfo.getDisplayVersion());
                updatePreference.setKey(updateInfo.getName());
                updatePreference.setOnActionListener(this);
                updatePreference.setUpdaterController(mUpdaterController);
                addPreference(updatePreference);
            }
        } else {
            EmptyListPreference emptyListPreference = new EmptyListPreference(getContext());
            addPreference(emptyListPreference);
        }
    }

    private void onStartAction(String downloadId, int action) {
        if (mDownloadInterstitialAd != null) {
            if (mDownloadInterstitialAd.isLoaded()) {
                mDownloadInterstitialAd.show();
            } else {
                if (!MKCenterApplication.getInstance().getDonationInfo().isBasic()) {
                    Snackbar.make(mItemView, getContext().getString(R.string.download_limited_speed, Constants.DONATION_BASIC), Snackbar.LENGTH_LONG).show();
                }
            }
        }
        if (action == UpdaterService.DOWNLOAD_START) {
            mUpdaterController.startDownload(downloadId);
        } else if (action == UpdaterService.DOWNLOAD_RESTART) {
            mUpdaterController.restartDownload(downloadId);
        } else {
            mUpdaterController.resumeDownload(downloadId);
        }
    }

    private void onCheckWarn(String downloadId, int action) {
        if (mUpdaterController.hasActiveDownloads()) {
            Snackbar.make(mItemView, R.string.download_already_running, Snackbar.LENGTH_SHORT).show();
        } else if (mUpdaterController.isInstallingUpdate()) {
            Snackbar.make(mItemView, R.string.install_already_running, Snackbar.LENGTH_SHORT).show();
        } else {
            SharedPreferences mMainPrefs = CommonUtil.getMainPrefs(getContext());
            boolean warn = mMainPrefs.getBoolean(Constants.PREF_MOBILE_DATA_WARNING, true);

            if (CommonUtil.isOnWifiOrEthernet(getContext()) || !warn) {
                onStartAction(downloadId, action);
                return;
            }

            View checkboxView = LayoutInflater.from(getContext()).inflate(R.layout.checkbox_view, null);
            CheckBox checkbox = checkboxView.findViewById(R.id.checkbox);
            checkbox.setText(R.string.checkbox_mobile_data_warning);

            new AlertDialog.Builder(getContext())
                    .setTitle(R.string.update_on_mobile_data_title)
                    .setMessage(R.string.update_on_mobile_data_message)
                    .setView(checkboxView)
                    .setPositiveButton(R.string.action_download,
                            (dialog, which) -> {
                                if (checkbox.isChecked()) {
                                    mMainPrefs.edit()
                                            .putBoolean(Constants.PREF_MOBILE_DATA_WARNING, false)
                                            .apply();
                                }
                                onStartAction(downloadId, action);
                            })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }
    }

    @Override
    public void onStartDownload(String downloadId) {
        onCheckWarn(downloadId, UpdaterService.DOWNLOAD_START);
    }

    @Override
    public void onRestartDownload(String downloadId) {
        onCheckWarn(downloadId, UpdaterService.DOWNLOAD_RESTART);
    }

    @Override
    public void onResumeDownload(String downloadId) {
        onCheckWarn(downloadId, UpdaterService.DOWNLOAD_RESUME);
    }

    @Override
    public void onPauseDownload(String downloadId) {
        mUpdaterController.pauseDownload(downloadId);
    }

    @Override
    public void onDeleteDownload(String downloadId) {
        mUpdaterController.deleteDownload(downloadId);
    }

    @Override
    public void onReboot() {
        PowerManager pm =
                (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
        pm.reboot(null);
    }

    @Override
    public void onInstallUpdate(String downloadId) {
        if (!isBatteryLevelOk()) {
            Resources resources = getContext().getResources();
            String message = resources.getString(R.string.dialog_battery_low_message_pct,
                    resources.getInteger(R.integer.battery_ok_percentage_discharging),
                    resources.getInteger(R.integer.battery_ok_percentage_charging));
            new AlertDialog.Builder(getContext())
                    .setTitle(R.string.dialog_battery_low_title)
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok, null).show();
        } else {
            UpdateInfo updateInfo = mUpdaterController.getUpdate(downloadId);
            int resId = R.string.apply_update_dialog_message;
            try {
                if (CommonUtil.isABUpdate(updateInfo.getFile())) {
                    resId = R.string.apply_update_dialog_message_ab;
                }
            } catch (IOException e) {
                Log.e(TAG, "Could not determine the type of the update");
            }
            new AlertDialog.Builder(getContext())
                    .setTitle(R.string.apply_update_dialog_title)
                    .setMessage(getContext().getString(resId,
                            updateInfo.getDisplayVersion(), getContext().getString(android.R.string.ok)))
                    .setPositiveButton(android.R.string.ok,
                            (dialog, which) -> CommonUtil.triggerUpdate(getContext(), downloadId))
                    .setNegativeButton(android.R.string.cancel, null).show();

        }
    }

    private boolean isBatteryLevelOk() {
        Intent intent = getContext().registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (!intent.getBooleanExtra(BatteryManager.EXTRA_PRESENT, false)) {
            return true;
        }
        int percent = Math.round(100.f * intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 100) /
                intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100));
        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
        int required = (plugged & BATTERY_PLUGGED_ANY) != 0 ?
                getContext().getResources().getInteger(R.integer.battery_ok_percentage_charging) :
                getContext().getResources().getInteger(R.integer.battery_ok_percentage_discharging);
        return percent >= required;
    }

}
