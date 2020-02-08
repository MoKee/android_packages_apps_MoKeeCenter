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

package com.mokee.center.fragment;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;

import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.model.Response;
import com.lzy.okserver.OkDownload;
import com.mokee.center.MKCenterApplication;
import com.mokee.center.R;
import com.mokee.center.activity.MainActivity;
import com.mokee.center.controller.UpdaterController;
import com.mokee.center.controller.UpdaterService;
import com.mokee.center.dialog.PreferencesDialogBuilder;
import com.mokee.center.misc.State;
import com.mokee.center.model.DonationInfo;
import com.mokee.center.model.UpdateInfo;
import com.mokee.center.preference.AvailableUpdatesPreferenceCategory;
import com.mokee.center.preference.DonationRecordPreference;
import com.mokee.center.preference.IncrementalUpdatesPreference;
import com.mokee.center.preference.UpdatePreference;
import com.mokee.center.preference.UpdateTypePreference;
import com.mokee.center.preference.VerifiedUpdatesPreference;
import com.mokee.center.receiver.UpdatesCheckReceiver;
import com.mokee.center.util.BuildInfoUtil;
import com.mokee.center.util.CommonUtil;
import com.mokee.center.util.FileUtil;
import com.mokee.center.util.RequestUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import static com.mokee.center.misc.Constants.AVAILABLE_UPDATES_TAG;
import static com.mokee.center.misc.Constants.DONATION_RESULT_OK;
import static com.mokee.center.misc.Constants.DONATION_RESULT_SUCCESS;
import static com.mokee.center.misc.Constants.KEY_DONATION_AMOUNT;
import static com.mokee.center.misc.Constants.PREF_DONATION_RECORD;
import static com.mokee.center.misc.Constants.PREF_FEATURES_CATEGORY;
import static com.mokee.center.misc.Constants.PREF_INCREMENTAL_UPDATES;
import static com.mokee.center.misc.Constants.PREF_LAST_UPDATE_CHECK;
import static com.mokee.center.misc.Constants.PREF_OUT_OF_DATE;
import static com.mokee.center.misc.Constants.PREF_UPDATES_CATEGORY;
import static com.mokee.center.misc.Constants.PREF_UPDATE_TYPE;
import static com.mokee.center.misc.Constants.PREF_VERIFIED_UPDATES;

public class UpdaterFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener,
        Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

    private static final String TAG = UpdaterFragment.class.getName();

    private UpdaterService mUpdaterService;
    private BroadcastReceiver mBroadcastReceiver;

    private View mRefreshIconView;
    private RotateAnimation mRefreshAnimation;

    private MainActivity mMainActivity;

    private AvailableUpdatesPreferenceCategory mUpdatesCategory;
    private PreferenceCategory mFeaturesCategory;
    private DonationRecordPreference mDonationRecordPreference;
    private IncrementalUpdatesPreference mIncrementalUpdatesPreference;
    private VerifiedUpdatesPreference mVerifiedUpdatesPreference;
    private UpdateTypePreference mUpdateTypePreference;

    private SharedPreferences mDonationPrefs;
    private SharedPreferences mMainPrefs;

    private OkDownload mOkDownload;
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            UpdaterService.LocalBinder binder = (UpdaterService.LocalBinder) service;
            mUpdaterService = binder.getService();
            mUpdatesCategory.setUpdaterController(mUpdaterService.getUpdaterController());
            updateFeatureStatus();
            getUpdatesList();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mUpdatesCategory.setUpdaterController(null);
            mUpdaterService = null;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (UpdaterController.ACTION_UPDATE_STATUS.equals(intent.getAction())) {
                    updateFeatureStatus();
                }
                String downloadId = intent.getStringExtra(UpdaterController.EXTRA_DOWNLOAD_ID);
                UpdatePreference updatePreference = findPreference(downloadId);
                if (updatePreference != null) {
                    updatePreference.updateStatus();
                }
            }
        };

        mRefreshAnimation = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        mRefreshAnimation.setInterpolator(new LinearInterpolator());
        mRefreshAnimation.setDuration(1000);

        mOkDownload = OkDownload.getInstance();
        mOkDownload.setFolder(FileUtil.getDownloadPath().getAbsolutePath());
        mOkDownload.getThreadPool().setCorePoolSize(1);
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.updater);
        setHasOptionsMenu(true);
        mDonationPrefs = CommonUtil.getDonationPrefs(mMainActivity);
        mMainPrefs = CommonUtil.getMainPrefs(getContext());
        mDonationRecordPreference = findPreference(PREF_DONATION_RECORD);
        mIncrementalUpdatesPreference = findPreference(PREF_INCREMENTAL_UPDATES);
        mIncrementalUpdatesPreference.setOnPreferenceClickListener(this);
        mVerifiedUpdatesPreference = findPreference(PREF_VERIFIED_UPDATES);
        mVerifiedUpdatesPreference.setOnPreferenceClickListener(this);
        mUpdateTypePreference = findPreference(PREF_UPDATE_TYPE);
        mUpdateTypePreference.setOnPreferenceChangeListener(this);
        mUpdatesCategory = findPreference(PREF_UPDATES_CATEGORY);
        mFeaturesCategory = findPreference(PREF_FEATURES_CATEGORY);
        if (CommonUtil.isABDevice()) {
            mFeaturesCategory.removePreference(mIncrementalUpdatesPreference);
            mIncrementalUpdatesPreference = null;
            mUpdateTypePreference.setDependency(null);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mMainActivity = (MainActivity) context;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (resultCode) {
            case DONATION_RESULT_OK:
            case DONATION_RESULT_SUCCESS:
                CommonUtil.updateDonationInfo(getContext());
                mDonationRecordPreference.updateRankingsInfo();
                mUpdateTypePreference.refreshPreference();
                if (mIncrementalUpdatesPreference != null) {
                    mIncrementalUpdatesPreference.refreshPreference();
                }
                mVerifiedUpdatesPreference.refreshPreference();
                mUpdateTypePreference.refreshPreference();
                mUpdatesCategory.setInterstitialAd();
                updateFeatureStatus();
                break;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mDonationPrefs.registerOnSharedPreferenceChangeListener(this);
        mMainPrefs.registerOnSharedPreferenceChangeListener(this);

        Intent intent = new Intent(getContext(), UpdaterService.class);
        mMainActivity.startService(intent);
        mMainActivity.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UpdaterController.ACTION_UPDATE_STATUS);
        intentFilter.addAction(UpdaterController.ACTION_DOWNLOAD_PROGRESS);
        intentFilter.addAction(UpdaterController.ACTION_INSTALL_PROGRESS);
        intentFilter.addAction(UpdaterController.ACTION_UPDATE_REMOVED);
        LocalBroadcastManager.getInstance(mMainActivity).registerReceiver(mBroadcastReceiver, intentFilter);
    }

    @Override
    public void onStop() {
        mDonationPrefs.unregisterOnSharedPreferenceChangeListener(this);
        mMainPrefs.unregisterOnSharedPreferenceChangeListener(this);
        LocalBroadcastManager.getInstance(mMainActivity).unregisterReceiver(mBroadcastReceiver);
        if (mUpdaterService != null) {
            mMainActivity.unbindService(mConnection);
        }
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        OkGo.cancelTag(MKCenterApplication.getInstance().getClient().build(), AVAILABLE_UPDATES_TAG);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_preferences:
                new PreferencesDialogBuilder(mMainActivity, mUpdaterService).show();
                return true;
            case R.id.menu_refresh:
                downloadUpdatesList(true);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void loadUpdatesList(LinkedList<UpdateInfo> updates, boolean manualRefresh) {
        if (updates.size() > 0) {
            Log.d(TAG, "Adding remote updates");
        }
        UpdaterController controller = mUpdaterService.getUpdaterController();
        boolean newUpdates = false;

        if (manualRefresh) {
            controller.cleanAvailableUpdates();
        }

        List<String> updatesOnline = new ArrayList<>();
        for (UpdateInfo update : updates) {
            newUpdates |= controller.addUpdate(update);
            updatesOnline.add(update.getName());
        }
        controller.setUpdatesAvailableOnline(updatesOnline);

        if (manualRefresh) {
            mMainActivity.makeSnackbar(newUpdates ? R.string.updates_found : R.string.no_updates_found).show();
        }

        mUpdatesCategory.refreshPreferences();
    }

    private void getUpdatesList() {
        File jsonFile = FileUtil.getCachedUpdateList(mMainActivity);
        if (jsonFile.exists()) {
            loadUpdatesList(State.loadState(jsonFile), false);
            Log.d(TAG, "Cached list parsed");
        } else {
            mUpdatesCategory.refreshPreferences();
        }
    }

    private void processNewJson(Response<String> response, File json, File jsonNew, boolean manualRefresh) {
        try {
            final LinkedList<UpdateInfo> updates = CommonUtil.parseJson(getContext(), response.body(), TAG);
            State.saveState(updates, jsonNew);
            loadUpdatesList(updates, manualRefresh);
            mMainPrefs.edit().remove(PREF_OUT_OF_DATE)
                    .putLong(PREF_LAST_UPDATE_CHECK, System.currentTimeMillis()).apply();
            if (json.exists() && CommonUtil.checkForNewUpdates(json, jsonNew)) {
                UpdatesCheckReceiver.updateRepeatingUpdatesCheck(getContext());
            }
            // In case we set a one-shot check because of a previous failure
            UpdatesCheckReceiver.cancelUpdatesCheck(mMainActivity);
            jsonNew.renameTo(json);
        } catch (JSONException e) {
            try {
                JSONObject status = new JSONObject(response.body());
                mMainPrefs.edit().putBoolean(PREF_OUT_OF_DATE, status.has(PREF_OUT_OF_DATE)).apply();
            } catch (JSONException ex) {
            }
            Log.e(TAG, "Could not read json", e);
            json.delete();
            loadUpdatesList(new LinkedList<>(), manualRefresh);
        }
    }

    private void downloadUpdatesList(boolean manualRefresh) {
        final File json = FileUtil.getCachedUpdateList(mMainActivity);
        final File jsonNew = new File(mMainActivity.getCacheDir().getAbsolutePath() + UUID.randomUUID());
        RequestUtil.fetchAvailableUpdates(getContext(), new StringCallback() {
            @Override
            public void onSuccess(Response<String> response) {
                processNewJson(response, json, jsonNew, manualRefresh);
            }

            @Override
            public void onError(Response<String> response) {
                super.onError(response);
                if (manualRefresh) {
                    mMainActivity.makeSnackbar(R.string.updates_check_failed).show();
                }
            }

            @Override
            public void onFinish() {
                super.onFinish();
                refreshAnimationStop();
            }
        });
        refreshAnimationStart();
    }

    private void refreshAnimationStart() {
        if (mRefreshIconView == null) {
            mRefreshIconView = getActivity().findViewById(R.id.menu_refresh);
        }
        if (mRefreshIconView != null) {
            mRefreshAnimation.setRepeatCount(Animation.INFINITE);
            mRefreshIconView.startAnimation(mRefreshAnimation);
            mRefreshIconView.setEnabled(false);
            if (mIncrementalUpdatesPreference != null) {
                mIncrementalUpdatesPreference.setEnabled(false);
            }
            mVerifiedUpdatesPreference.setEnabled(false);
            mUpdatesCategory.setPendingListPreferences();
        }
    }

    private void refreshAnimationStop() {
        if (mRefreshIconView != null) {
            mRefreshAnimation.setRepeatCount(0);
            mRefreshIconView.setEnabled(true);
            updateFeatureStatus();
            mUpdatesCategory.refreshPreferences();
        }
    }

    private void updateFeatureStatus() {
        DonationInfo donationInfo = MKCenterApplication.getInstance().getDonationInfo();
        if (mIncrementalUpdatesPreference != null) {
            mIncrementalUpdatesPreference.setEnabled(donationInfo.isBasic()
                    && !mUpdaterService.getUpdaterController().hasActiveDownloads()
                    && !mUpdaterService.getUpdaterController().isInstallingUpdate());
        }
        mVerifiedUpdatesPreference.setEnabled(donationInfo.isAdvanced()
                && !mUpdaterService.getUpdaterController().hasActiveDownloads()
                && !mUpdaterService.getUpdaterController().isInstallingUpdate());
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(KEY_DONATION_AMOUNT)) {
            if (mDonationPrefs.getInt(KEY_DONATION_AMOUNT, 0)
                    > MKCenterApplication.getInstance().getDonationInfo().getPaid()) {
                CommonUtil.restoreLicenseRequest(mMainActivity);
            }
        } else if (key.equals(PREF_INCREMENTAL_UPDATES)) {
            String suggestUpdateType = BuildInfoUtil.getSuggestUpdateType();
            String configUpdateType = mMainPrefs.getString(PREF_UPDATE_TYPE, suggestUpdateType);
            if (!TextUtils.equals(suggestUpdateType, configUpdateType)) {
                mMainPrefs.edit().putString(PREF_UPDATE_TYPE, suggestUpdateType).apply();
                mUpdateTypePreference.setValue(suggestUpdateType);
            }
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference instanceof IncrementalUpdatesPreference
                || preference instanceof VerifiedUpdatesPreference) {
            File jsonFile = FileUtil.getCachedUpdateList(mMainActivity);
            jsonFile.delete();
            downloadUpdatesList(true);
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference instanceof UpdateTypePreference) {
            if (TextUtils.equals(mUpdateTypePreference.getValue(), newValue.toString()))
                return false;
            File jsonFile = FileUtil.getCachedUpdateList(mMainActivity);
            jsonFile.delete();
            mMainPrefs.edit().putString(PREF_UPDATE_TYPE, newValue.toString()).apply();
            downloadUpdatesList(true);
            int index = mUpdateTypePreference.findIndexOfValue(newValue.toString());
            preference.setSummary(mUpdateTypePreference.getEntries()[index]);
            return true;
        }
        return false;
    }
}
