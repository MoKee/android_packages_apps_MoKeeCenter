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
import android.support.design.widget.Snackbar;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.view.View;

import com.mokee.center.R;
import com.mokee.center.controller.UpdaterController;
import com.mokee.center.model.UpdateInfo;

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
                updatePreference.setTitle(updateInfo.getName());
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

    @Override
    public void onStartDownload(String downloadId) {
        if (!mUpdaterController.hasActiveDownloads()) {
            mUpdaterController.startDownload(downloadId);
        } else {
            Snackbar.make(mItemView, R.string.download_already_running, Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPauseDownload(String downloadId) {
        mUpdaterController.pauseDownload(downloadId);
    }

    @Override
    public void onResumeDownload(String downloadId) {
        if (!mUpdaterController.hasActiveDownloads()) {
            mUpdaterController.resumeDownload(downloadId);
        } else {
            Snackbar.make(mItemView, R.string.download_already_running, Snackbar.LENGTH_SHORT).show();
        }
    }
}
