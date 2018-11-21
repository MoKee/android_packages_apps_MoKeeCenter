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
import android.support.v7.app.AlertDialog;
import android.support.v7.internal.widget.PreferenceImageView;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.lzy.okgo.model.Progress;
import com.mokee.center.R;
import com.mokee.center.misc.Constants;
import com.mokee.center.model.UpdateInfo;
import com.mokee.center.util.CommonUtils;

import java.io.IOException;
import java.net.SocketException;
import java.text.NumberFormat;

public class UpdatePreference extends Preference implements View.OnClickListener {

    private OnActionListener mOnActionListener;

    private PreferenceImageView mIconView;
    private TextView mFileSizeView;
    private TextView mSummaryView;
    private ProgressBar mDownloadProgress;
    private ProgressBar mActionProgress;
    private View mUpdateButton;

    public Progress getProgress() {
        return mProgress;
    }

    public void setProgress(Progress progress) {
        this.mProgress = progress;
    }

    private Progress mProgress;
    private UpdateInfo mUpdateInfo;


    public UpdatePreference(Context context, UpdateInfo updateInfo) {
        super(context);
        mUpdateInfo  = updateInfo;
        setLayoutResource(R.layout.preference_update);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        mUpdateButton = holder.findViewById(R.id.action_frame);
        mUpdateButton.setOnClickListener(this);

        mIconView = (PreferenceImageView) holder.findViewById(R.id.action_icon);

        mFileSizeView = (TextView) holder.findViewById(R.id.file_size);
        mFileSizeView.setText(Formatter.formatFileSize(getContext(), mUpdateInfo.getFileSize()));

        mDownloadProgress = (ProgressBar) holder.findViewById(R.id.download_progress);
        mActionProgress = (ProgressBar) holder.findViewById(R.id.action_progress);

        mSummaryView = (TextView) holder.findViewById(R.id.summary);
        updatePreferenceView();

    }

    public void updatePreferenceView() {
        if (mDownloadProgress == null || mIconView == null
                ||mSummaryView == null && mFileSizeView == null
                || mActionProgress == null) {
            return;
        }
        if (mProgress != null) {
            mDownloadProgress.setMax((int) mProgress.totalSize);
            mDownloadProgress.setProgress((int) mProgress.currentSize);
            switch (mProgress.status) {
                case Progress.WAITING:
                    mIconView.setImageResource(R.drawable.ic_action_pause);
                    mDownloadProgress.setVisibility(View.VISIBLE);
                    mDownloadProgress.setIndeterminate(true);
                    mSummaryView.setText(R.string.download_starting_notification);
                    mActionProgress.setVisibility(View.VISIBLE);
                    mIconView.setVisibility(View.GONE);
                    mUpdateButton.setEnabled(false);
                    break;
                case Progress.LOADING:
                    mIconView.setImageResource(R.drawable.ic_action_pause);
                    mDownloadProgress.setVisibility(View.VISIBLE);
                    if (mProgress.extra1 != null) {
                        mDownloadProgress.setIndeterminate(false);
                        mSummaryView.setText(getContext().getString(R.string.download_progress_eta_new,
                                Formatter.formatFileSize(getContext(), mProgress.currentSize),
                                Formatter.formatFileSize(getContext(), mProgress.totalSize),
                                mProgress.extra1,
                                NumberFormat.getPercentInstance().format(mProgress.fraction)));
                    }
                    mActionProgress.setVisibility(View.GONE);
                    mIconView.setVisibility(View.VISIBLE);
                    mUpdateButton.setEnabled(true);
                    break;
                case Progress.PAUSE:
                    mIconView.setImageResource(R.drawable.ic_action_download);
                    mDownloadProgress.setVisibility(View.VISIBLE);
                    mDownloadProgress.setIndeterminate(false);
                    mSummaryView.setText(R.string.download_paused_notification);
                    break;
                case Progress.FINISH:
                    mIconView.setImageResource(R.drawable.ic_action_install);
                    mDownloadProgress.setVisibility(View.GONE);
                    mSummaryView.setText(R.string.download_completed_notification);
                    break;
                case Progress.ERROR:
                    if (mProgress.exception instanceof SocketException
                            || mProgress.exception instanceof IOException) {
                        mSummaryView.setText(R.string.download_waiting_network_notification);
                        mActionProgress.setVisibility(View.VISIBLE);
                        mIconView.setVisibility(View.GONE);
                        mUpdateButton.setEnabled(false);
                        break;
                    }
                default:
                    mIconView.setImageResource(R.drawable.ic_action_download);
                    mDownloadProgress.setVisibility(View.VISIBLE);
                    mDownloadProgress.setIndeterminate(false);
                    mSummaryView.setText(getContext().getString(R.string.download_progress_new,
                            Formatter.formatFileSize(getContext(), mProgress.currentSize),
                            Formatter.formatFileSize(getContext(), mProgress.totalSize),
                            NumberFormat.getPercentInstance().format(mProgress.fraction)));
            }
        } else {
            mIconView.setImageResource(R.drawable.ic_action_download);
            mDownloadProgress.setVisibility(View.GONE);
            long diffSize = Long.valueOf(mUpdateInfo.getDiffSize());
            if (diffSize == 0) {
                mSummaryView.setText(R.string.update_not_support_incremental_updates);
            } else {
                mSummaryView.setText(getContext().getString(CommonUtils.isIncrementalUpdate(mUpdateInfo.getName())
                                ? R.string.update_supported_incremental_updates_ota
                                : R.string.update_supported_incremental_updates_full,
                        Formatter.formatFileSize(getContext(), diffSize)));
            }
        }
    }

    private void showWarnDialog(SharedPreferences prefs) {
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
                                prefs.edit()
                                        .putBoolean(Constants.PREF_MOBILE_DATA_WARNING, false)
                                        .apply();
                            }
                            execAction();
                        })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void execAction() {
        if (mProgress == null) {
            mOnActionListener.onStartDownload(mUpdateInfo.getName());
        } else {
            mOnActionListener.onResumeDownload(mUpdateInfo.getName());
        }
    }

    @Override
    public void onClick(View view) {
        if (mOnActionListener == null) return;
        SharedPreferences mMainPrefs = CommonUtils.getMainPrefs(getContext());
        boolean warn = mMainPrefs.getBoolean(Constants.PREF_MOBILE_DATA_WARNING, true);
        if (mProgress == null || mProgress.status == Progress.PAUSE
                || mProgress.status == Progress.ERROR || mProgress.status == Progress.NONE) {
            if (CommonUtils.isOnWifiOrEthernet(getContext()) || !warn) {
                execAction();
                return;
            }
            showWarnDialog(mMainPrefs);
        } else {
            switch (mProgress.status) {
                case Progress.WAITING:
                case Progress.LOADING:
                    mOnActionListener.onPauseDownload(mUpdateInfo.getName());
                    break;
            }
        }
    }

    public void setOnActionListener(OnActionListener listener) {
        mOnActionListener = listener;
    }

    public interface OnActionListener {
        void onStartDownload(String downloadId);
        void onPauseDownload(String downloadId);
        void onResumeDownload(String downloadId);
    }

}
