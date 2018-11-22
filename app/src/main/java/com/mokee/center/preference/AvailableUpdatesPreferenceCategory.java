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

package com.mokee.center.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;

import com.mokee.center.R;
import com.mokee.center.controller.UpdaterController;
import com.mokee.center.misc.Constants;
import com.mokee.center.model.UpdateInfo;
import com.mokee.center.util.CommonUtil;

import java.util.LinkedList;

public class AvailableUpdatesPreferenceCategory extends PreferenceCategory implements UpdatePreference.OnActionListener {

    private UpdaterController mUpdaterController;
    private View mItemView;

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
    }

    public void refreshPreferences() {
        removeAll();
        LinkedList<UpdateInfo> availableUpdates = mUpdaterController.getUpdates();
        if (availableUpdates != null && availableUpdates.size() > 0) {
            for (UpdateInfo updateInfo : availableUpdates) {
                UpdatePreference updatePreference = new UpdatePreference(getContext(), updateInfo);
                updatePreference.setTitle(updateInfo.getDisplayVersion());
                updatePreference.setKey(updateInfo.getName());
                updatePreference.setProgress(updateInfo.getProgress());
                updatePreference.setOnActionListener(this);
                addPreference(updatePreference);
            }
        } else {
            EmptyListPreference emptyListPreference = new EmptyListPreference(getContext());
            addPreference(emptyListPreference);
        }
    }

    private void onStartAction(String downloadId, boolean isNew) {
        if (isNew) {
            mUpdaterController.startDownload(downloadId);
        } else {
            mUpdaterController.resumeDownload(downloadId);
        }
    }

    private void onCheckWarn(String downloadId, boolean isNew) {
        if (mUpdaterController.hasActiveDownloads()) {
            Snackbar.make(mItemView, R.string.download_already_running, Snackbar.LENGTH_SHORT).show();
        } else {
            SharedPreferences mMainPrefs = CommonUtil.getMainPrefs(getContext());
            boolean warn = mMainPrefs.getBoolean(Constants.PREF_MOBILE_DATA_WARNING, true);

            if (CommonUtil.isOnWifiOrEthernet(getContext()) || !warn) {
                onStartAction(downloadId, isNew);
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
                                onStartAction(downloadId, isNew);
                            })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }
    }

    @Override
    public void onStartDownload(String downloadId) {
        onCheckWarn(downloadId, true);
    }

    @Override
    public void onResumeDownload(String downloadId) {
        onCheckWarn(downloadId, false);
    }

    @Override
    public void onPauseDownload(String downloadId) {
        mUpdaterController.pauseDownload(downloadId);
    }
}
