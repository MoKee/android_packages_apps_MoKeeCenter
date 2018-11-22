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
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.model.Response;
import com.lzy.okserver.OkDownload;
import com.mokee.center.MKCenterApplication;
import com.mokee.center.R;
import com.mokee.center.activity.MainActivity;
import com.mokee.center.controller.UpdaterController;
import com.mokee.center.misc.Constants;
import com.mokee.center.misc.State;
import com.mokee.center.model.UpdateInfo;
import com.mokee.center.preference.AvailableUpdatesPreferenceCategory;
import com.mokee.center.preference.DonationRecordPreference;
import com.mokee.center.preference.IncrementalUpdatesPreference;
import com.mokee.center.preference.LastUpdateCheckPreference;
import com.mokee.center.preference.UpdatePreference;
import com.mokee.center.preference.UpdateTypePreference;
import com.mokee.center.preference.VerifiedUpdatesPreference;
import com.mokee.center.receiver.UpdatesCheckReceiver;
import com.mokee.center.controller.UpdaterService;
import com.mokee.center.util.CommonUtil;
import com.mokee.center.util.FileUtil;
import com.mokee.center.util.RequestUtil;

import org.json.JSONException;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import static com.mokee.center.misc.Constants.DONATION_RESULT_OK;
import static com.mokee.center.misc.Constants.DONATION_RESULT_SUCCESS;
import static com.mokee.center.misc.Constants.KEY_DONATION_AMOUNT;
import static com.mokee.center.misc.Constants.PREF_DONATION_RECORD;
import static com.mokee.center.misc.Constants.PREF_INCREMENTAL_UPDATES;
import static com.mokee.center.misc.Constants.PREF_LAST_UPDATE_CHECK;
import static com.mokee.center.misc.Constants.PREF_UPDATES_CATEGORY;
import static com.mokee.center.misc.Constants.PREF_UPDATE_TYPE;
import static com.mokee.center.misc.Constants.PREF_VERIFIED_UPDATES;

public class UpdaterFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

    private static final String TAG = UpdaterFragment.class.getName();

    private UpdaterService mUpdaterService;
    private BroadcastReceiver mBroadcastReceiver;

    private View mRefreshIconView;
    private RotateAnimation mRefreshAnimation;

    private MainActivity mMainActivity;
    private InterstitialAd mWelcomeInterstitialAd;

    private AvailableUpdatesPreferenceCategory mUpdatesCategory;
    private DonationRecordPreference mDonationRecordPreference;
    private IncrementalUpdatesPreference mIncrementalUpdatesPreference;
    private VerifiedUpdatesPreference mVerifiedUpdatesPreference;
    private UpdateTypePreference mUpdateTypePreference;

    private SharedPreferences mDonationPrefs;
    private SharedPreferences mMainPrefs;

    private OkDownload mOkDownload;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!MKCenterApplication.getInstance().getDonationInfo().isAdvanced()) {
            MobileAds.initialize(getContext(), getString(R.string.app_id));
            mWelcomeInterstitialAd = new InterstitialAd(getContext());
            mWelcomeInterstitialAd.setAdUnitId(getString(R.string.interstitial_ad_unit_id));
            AdRequest adRequest = new AdRequest.Builder().build();
            mWelcomeInterstitialAd.setAdListener(new AdListener() {
                @Override
                public void onAdLoaded() {
                    super.onAdLoaded();
                    mWelcomeInterstitialAd.show();
                }
            });
            mWelcomeInterstitialAd.loadAd(adRequest);
        }

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (UpdaterController.ACTION_UPDATE_STATUS.equals(intent.getAction())
                        || UpdaterController.ACTION_DOWNLOAD_PROGRESS.equals(intent.getAction())) {
                    String downloadId = intent.getStringExtra(UpdaterController.EXTRA_DOWNLOAD_ID);
                    UpdatePreference updatePreference = (UpdatePreference) findPreference(downloadId);
                    if (updatePreference.getProgress() == null) {
                        updatePreference.setProgress(OkDownload.getInstance().getTask(downloadId).progress);

                    }
                    updatePreference.updatePreferenceView();
                }
            }
        };

        mRefreshAnimation = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        mRefreshAnimation.setInterpolator(new LinearInterpolator());
        mRefreshAnimation.setDuration(1000);

        mOkDownload = OkDownload.getInstance();
        mOkDownload.setFolder(CommonUtil.getDownloadPath(getContext()).getAbsolutePath());
        mOkDownload.getThreadPool().setCorePoolSize(1);
    }


    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.updater);
        setHasOptionsMenu(true);
        mDonationPrefs = CommonUtil.getDonationPrefs(getContext());
        mMainPrefs = CommonUtil.getMainPrefs(getContext());
        mDonationRecordPreference = (DonationRecordPreference) findPreference(PREF_DONATION_RECORD);
        mIncrementalUpdatesPreference = (IncrementalUpdatesPreference) findPreference(PREF_INCREMENTAL_UPDATES);
        mIncrementalUpdatesPreference.setOnPreferenceClickListener(this);
        mVerifiedUpdatesPreference = (VerifiedUpdatesPreference) findPreference(PREF_VERIFIED_UPDATES);
        mVerifiedUpdatesPreference.setOnPreferenceClickListener(this);
        mUpdateTypePreference = (UpdateTypePreference) findPreference(PREF_UPDATE_TYPE);
        mUpdateTypePreference.setOnPreferenceChangeListener(this);
        mUpdatesCategory = (AvailableUpdatesPreferenceCategory) findPreference(PREF_UPDATES_CATEGORY);
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
                mDonationRecordPreference.updateRankInfo();
                mIncrementalUpdatesPreference.updateStatus();
                mVerifiedUpdatesPreference.updateStatus();
                break;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mDonationPrefs.registerOnSharedPreferenceChangeListener(this);
        mMainPrefs.registerOnSharedPreferenceChangeListener(this);

        Intent intent = new Intent(mMainActivity, UpdaterService.class);
        mMainActivity.startService(intent);
        mMainActivity.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UpdaterController.ACTION_UPDATE_STATUS);
        intentFilter.addAction(UpdaterController.ACTION_DOWNLOAD_PROGRESS);
        intentFilter.addAction(UpdaterController.ACTION_INSTALL_PROGRESS);
        intentFilter.addAction(UpdaterController.ACTION_UPDATE_REMOVED);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mBroadcastReceiver, intentFilter);
    }

    @Override
    public void onStop() {
        mDonationPrefs.unregisterOnSharedPreferenceChangeListener(this);
        mMainPrefs.unregisterOnSharedPreferenceChangeListener(this);
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mBroadcastReceiver);
        if (mUpdaterService != null) {
            mMainActivity.unbindService(mConnection);
        }
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        OkGo.cancelAll(MKCenterApplication.getInstance().getClient().build());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                downloadUpdatesList(true);
                return true;
            case R.id.action_restore:
                CommonUtil.restoreLicenseRequest(getActivity());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            UpdaterService.LocalBinder binder = (UpdaterService.LocalBinder) service;
            mUpdaterService = binder.getService();
            mUpdatesCategory.setUpdaterController(mUpdaterService.getUpdaterController());
            mUpdatesCategory.refreshPreferences();
            getUpdatesList();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            //mAdapter.setUpdaterController(null);
            mUpdaterService = null;
            //mAdapter.notifyDataSetChanged();
        }
    };

    private void loadUpdatesList(LinkedList<UpdateInfo> updates, boolean manualRefresh) {
        Log.d(TAG, "Adding remote updates");
        UpdaterController controller = mUpdaterService.getUpdaterController();
        boolean newUpdates = false;

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
        File jsonFile = FileUtil.getCachedUpdateList(getContext());
        if (jsonFile.exists()) {
            loadUpdatesList(State.loadState(jsonFile), false);
            Log.d(TAG, "Cached list parsed");
        } else {
            downloadUpdatesList(false);
        }
    }

    private void processNewJson(Response<String> response, File json, File jsonNew, boolean manualRefresh) {
        try {
            final LinkedList<UpdateInfo> updates = CommonUtil.parseJson(response.body(), TAG);
            State.saveState(updates, jsonNew);
            loadUpdatesList(updates, manualRefresh);
            mMainPrefs.edit().putLong(Constants.PREF_LAST_UPDATE_CHECK, System.currentTimeMillis()).apply();
            ((LastUpdateCheckPreference)findPreference(PREF_LAST_UPDATE_CHECK)).updateSummary();
            if (json.exists() && mMainPrefs.getBoolean(Constants.PREF_AUTO_UPDATES_CHECK, true)
                    && CommonUtil.checkForNewUpdates(json, jsonNew)) {
                UpdatesCheckReceiver.updateRepeatingUpdatesCheck(getContext());
            }
            // In case we set a one-shot check because of a previous failure
            UpdatesCheckReceiver.cancelUpdatesCheck(getContext());
            jsonNew.renameTo(json);
        } catch (JSONException e) {
            Log.e(TAG, "Could not read json", e);
            json.delete();
            loadUpdatesList(new LinkedList<>(), manualRefresh);
        }
    }

    private void downloadUpdatesList(final boolean manualRefresh) {
        final File json = FileUtil.getCachedUpdateList(getContext());
        final File jsonNew = new File(json.getAbsolutePath() + UUID.randomUUID());
        RequestUtil.fetchAvailableUpdates(getContext(), new StringCallback() {
            @Override
            public void onSuccess(Response<String> response) {
                processNewJson(response, json, jsonNew, manualRefresh);
            }

            @Override
            public void onError(Response<String> response) {
                super.onError(response);
                mMainActivity.makeSnackbar(R.string.updates_check_failed).show();
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
            mRefreshIconView = getActivity().findViewById(R.id.action_refresh);
        }
        if (mRefreshIconView != null) {
            mRefreshAnimation.setRepeatCount(Animation.INFINITE);
            mRefreshIconView.startAnimation(mRefreshAnimation);
            mRefreshIconView.setEnabled(false);
            mIncrementalUpdatesPreference.setEnabled(false);
            mVerifiedUpdatesPreference.setEnabled(false);
        }
    }

    private void refreshAnimationStop() {
        if (mRefreshIconView != null) {
            mRefreshAnimation.setRepeatCount(0);
            mRefreshIconView.setEnabled(true);
            mIncrementalUpdatesPreference.setEnabled(true);
            mVerifiedUpdatesPreference.setEnabled(true);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(KEY_DONATION_AMOUNT)) {
            if (mDonationPrefs.getInt(KEY_DONATION_AMOUNT, 0)
                    > MKCenterApplication.getInstance().getDonationInfo().getPaid()) {
                CommonUtil.restoreLicenseRequest(getActivity());
            }
        } else if (key.equals(PREF_INCREMENTAL_UPDATES)) {
            String suggestUpdateType = CommonUtil.getSuggestUpdateType();
            String configUpdateType = mMainPrefs.getString(PREF_UPDATE_TYPE, String.valueOf(suggestUpdateType));
            if (!TextUtils.equals(suggestUpdateType, configUpdateType)) {
                mMainPrefs.edit().putString(PREF_UPDATE_TYPE, suggestUpdateType).apply();
                mUpdateTypePreference.setValue(suggestUpdateType);
                mUpdateTypePreference.setSummary(mUpdateTypePreference.getEntries()[mUpdateTypePreference.findIndexOfValue(suggestUpdateType)]);
            }
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference instanceof IncrementalUpdatesPreference
                || preference instanceof VerifiedUpdatesPreference) {
            File jsonFile = FileUtil.getCachedUpdateList(getContext());
            jsonFile.delete();
            loadUpdatesList(new LinkedList<>(), false);
            downloadUpdatesList(true);
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference instanceof UpdateTypePreference) {
            if (TextUtils.equals(mUpdateTypePreference.getValue(), newValue.toString())) return false;
            File jsonFile = FileUtil.getCachedUpdateList(getContext());
            jsonFile.delete();
            loadUpdatesList(new LinkedList<>(), false);
            mMainPrefs.edit().putString(PREF_UPDATE_TYPE, newValue.toString()).apply();
            downloadUpdatesList(true);
            return true;
        }
        return false;
    }

}
