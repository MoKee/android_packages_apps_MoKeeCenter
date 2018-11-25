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
import android.support.v7.internal.widget.PreferenceImageView;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.support.v7.widget.PopupMenu;
import android.text.format.Formatter;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.lzy.okgo.exception.HttpException;
import com.lzy.okgo.model.Progress;
import com.mokee.center.R;
import com.mokee.center.controller.UpdaterController;
import com.mokee.center.model.UpdateInfo;
import com.mokee.center.util.BuildInfoUtil;

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.NumberFormat;

import javax.net.ssl.SSLException;

public class UpdatePreference extends Preference implements View.OnClickListener, View.OnLongClickListener, PopupMenu.OnMenuItemClickListener {

    private OnActionListener mOnActionListener;

    private PreferenceImageView mIconView;
    private TextView mTitleView;
    private TextView mFileSizeView;
    private TextView mSummaryView;
    private ProgressBar mDownloadProgress;
    private ProgressBar mActionProgress;
    private View mUpdateButton;

    private UpdaterController mUpdaterController;

    public void setUpdaterController(UpdaterController updaterController) {
        mUpdaterController = updaterController;
    }

    public UpdatePreference(Context context) {
        super(context);
        setLayoutResource(R.layout.preference_update);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        UpdateInfo updateInfo = mUpdaterController.getUpdate(getKey());
        if (updateInfo == null) return;
        holder.itemView.setOnLongClickListener(this);

        mUpdateButton = holder.findViewById(R.id.action_frame);
        mUpdateButton.setOnClickListener(this);

        mIconView = (PreferenceImageView) holder.findViewById(R.id.action_icon);
        mTitleView = (TextView) holder.findViewById(android.R.id.title);

        mFileSizeView = (TextView) holder.findViewById(R.id.file_size);
        mFileSizeView.setText(Formatter.formatFileSize(getContext(), updateInfo.getFileSize()));

        mDownloadProgress = (ProgressBar) holder.findViewById(R.id.download_progress);
        mActionProgress = (ProgressBar) holder.findViewById(R.id.action_progress);

        mSummaryView = (TextView) holder.findViewById(R.id.summary);
        updatePreferenceView(updateInfo);
    }

    public void updatePreferenceView(UpdateInfo updateInfo) {
        if (mDownloadProgress == null || mIconView == null
                || mSummaryView == null && mFileSizeView == null
                || mActionProgress == null) {
            return;
        }
        Progress progress = updateInfo.getProgress();
        if (progress != null) {
            mDownloadProgress.setMax((int) progress.totalSize);
            mDownloadProgress.setProgress((int) progress.currentSize);
            switch (progress.status) {
                case Progress.WAITING:
                    mIconView.setVisibility(View.GONE);
                    mDownloadProgress.setIndeterminate(true);
                    mDownloadProgress.setVisibility(View.VISIBLE);
                    mSummaryView.setText(R.string.download_starting_notification);
                    mActionProgress.setVisibility(View.VISIBLE);
                    mUpdateButton.setEnabled(false);
                    break;
                case Progress.LOADING:
                    mIconView.setImageResource(R.drawable.ic_action_pause);
                    mIconView.setVisibility(View.VISIBLE);
                    mDownloadProgress.setIndeterminate(false);
                    mDownloadProgress.setVisibility(View.VISIBLE);
                    if (progress.extra1 != null) {
                        mSummaryView.setText(getContext().getString(R.string.download_progress_eta_new,
                                Formatter.formatFileSize(getContext(), progress.currentSize),
                                Formatter.formatFileSize(getContext(), progress.totalSize),
                                progress.extra1,
                                NumberFormat.getPercentInstance().format(progress.fraction)));
                    }
                    mActionProgress.setVisibility(View.GONE);
                    mUpdateButton.setEnabled(true);
                    break;
                case Progress.PAUSE:
                    mIconView.setImageResource(R.drawable.ic_action_download);
                    mIconView.setVisibility(View.VISIBLE);
                    mDownloadProgress.setIndeterminate(false);
                    mDownloadProgress.setVisibility(View.VISIBLE);
                    mSummaryView.setText(R.string.download_paused_notification);
                    mActionProgress.setVisibility(View.GONE);
                    mUpdateButton.setEnabled(true);
                    break;
                case Progress.FINISH:
                    mIconView.setImageResource(R.drawable.ic_action_install);
                    mIconView.setVisibility(View.VISIBLE);
                    mDownloadProgress.setIndeterminate(false);
                    mDownloadProgress.setVisibility(View.GONE);
                    mSummaryView.setText(R.string.download_completed_notification);
                    mActionProgress.setVisibility(View.GONE);
                    mUpdateButton.setEnabled(true);
                    break;
                case Progress.ERROR:
                    if (progress.exception instanceof SSLException || progress.exception instanceof UnknownHostException) {
                        mIconView.setVisibility(View.GONE);
                        mDownloadProgress.setIndeterminate(true);
                        mDownloadProgress.setVisibility(View.VISIBLE);
                        mSummaryView.setText(R.string.download_waiting_network_notification);
                        mActionProgress.setVisibility(View.VISIBLE);
                        mUpdateButton.setEnabled(false);
                        break;
                    } else if (progress.exception instanceof UnsupportedOperationException
                            || progress.fraction == 1) {
                        mIconView.setImageResource(R.drawable.ic_action_download);
                        mIconView.setVisibility(View.VISIBLE);
                        mDownloadProgress.setIndeterminate(false);
                        mDownloadProgress.setVisibility(View.GONE);
                        mSummaryView.setText(R.string.download_verification_failed_notification);
                        mActionProgress.setVisibility(View.GONE);
                        mUpdateButton.setEnabled(true);
                        break;
                    } else if (progress.exception instanceof HttpException) {
                        mIconView.setImageResource(R.drawable.ic_action_download);
                        mIconView.setVisibility(View.VISIBLE);
                        mDownloadProgress.setIndeterminate(false);
                        mDownloadProgress.setVisibility(View.GONE);
                        mSummaryView.setText(R.string.download_file_not_found_notification);
                        mActionProgress.setVisibility(View.GONE);
                        mUpdateButton.setEnabled(true);
                        break;
                    }
                default:
                    mIconView.setImageResource(R.drawable.ic_action_download);
                    mIconView.setVisibility(View.VISIBLE);
                    mDownloadProgress.setIndeterminate(false);
                    mDownloadProgress.setVisibility(View.VISIBLE);
                    mSummaryView.setText(getContext().getString(R.string.download_progress_new,
                            Formatter.formatFileSize(getContext(), progress.currentSize),
                            Formatter.formatFileSize(getContext(), progress.totalSize),
                            NumberFormat.getPercentInstance().format(progress.fraction)));
                    mActionProgress.setVisibility(View.GONE);
                    mUpdateButton.setEnabled(true);
            }
        } else {
            mIconView.setImageResource(R.drawable.ic_action_download);
            mIconView.setVisibility(View.VISIBLE);
            mDownloadProgress.setIndeterminate(false);
            mDownloadProgress.setVisibility(View.GONE);
            long diffSize = Long.valueOf(updateInfo.getDiffSize());
            if (diffSize == 0) {
                mSummaryView.setText(R.string.incremental_updates_not_support_summary);
            } else {
                mSummaryView.setText(getContext().getString(BuildInfoUtil.isIncrementalUpdate(getKey())
                                ? R.string.incremental_updates_supported_ota_summary
                                : R.string.incremental_updates_supported_full_summary,
                        Formatter.formatFileSize(getContext(), diffSize)));
            }
            mActionProgress.setVisibility(View.GONE);
            mUpdateButton.setEnabled(true);
        }
    }

    private void onStartAction(Progress progress) {
        if (progress == null) {
            mOnActionListener.onStartDownload(getKey());
        } else if (progress.status == Progress.ERROR
                && progress.fraction == 1) {
            mOnActionListener.onRestartDownload(getKey());
        } else {
            mOnActionListener.onResumeDownload(getKey());
        }
    }

    @Override
    public boolean onLongClick(View view) {
        Progress progress = mUpdaterController.getUpdate(getKey()).getProgress();
        if (progress != null && progress.status == Progress.FINISH) {
            PopupMenu popupMenu = new PopupMenu(getContext(), mTitleView);
            popupMenu.getMenuInflater().inflate(R.menu.menu_action_mode, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(this);
            popupMenu.show();
            return true;
        }
        return false;
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.menu_delete_action:
                mOnActionListener.onDeleteDownload(getKey());
                return true;
        }
        return false;
    }

    @Override
    public void onClick(View view) {
        if (mOnActionListener == null) return;
        Progress progress = mUpdaterController.getUpdate(getKey()).getProgress();
        if (progress == null || progress.status == Progress.PAUSE
                || progress.status == Progress.ERROR || progress.status == Progress.NONE) {
            onStartAction(progress);
        } else if (progress.status == Progress.LOADING){
            mOnActionListener.onPauseDownload(getKey());
        } else if (progress.status == Progress.FINISH) {

        }
    }

    public void setOnActionListener(OnActionListener listener) {
        mOnActionListener = listener;
    }

    public interface OnActionListener {
        void onStartDownload(String downloadId);
        void onRestartDownload(String downloadId);
        void onResumeDownload(String downloadId);
        void onPauseDownload(String downloadId);
        void onDeleteDownload(String downloadId);
    }

}
