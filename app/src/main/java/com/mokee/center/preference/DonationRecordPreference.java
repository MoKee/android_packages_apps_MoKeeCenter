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

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.Preference;

import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.model.Response;
import com.mokee.center.MKCenterApplication;
import com.mokee.center.R;
import com.mokee.center.model.RankingsInfo;
import com.mokee.center.util.CommonUtil;
import com.mokee.os.Build;

import static com.mokee.center.misc.Constants.KEY_DONATION_AMOUNT;
import static com.mokee.center.misc.Constants.KEY_DONATION_FIRST_CHECK_COMPLETED;
import static com.mokee.center.misc.Constants.KEY_DONATION_LAST_CHECK_TIME;
import static com.mokee.center.misc.Constants.KEY_DONATION_PERCENT;
import static com.mokee.center.misc.Constants.KEY_DONATION_RANKINGS;
import static com.mokee.center.misc.Constants.PARAM_UNIQUE_IDS;

public class DonationRecordPreference extends Preference {

    private static final String TAG = DonationRecordPreference.class.getName();

    private SharedPreferences mDonationPrefs;
    private int mPaid, mPercent, mRankings;

    public DonationRecordPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mDonationPrefs = CommonUtil.getDonationPrefs(context);
    }

    @Override
    public void onAttached() {
        super.onAttached();
        mPaid = MKCenterApplication.getInstance().getDonationInfo().getPaid();
        mPercent = mDonationPrefs.getInt(KEY_DONATION_PERCENT, 0);
        mRankings = mDonationPrefs.getInt(KEY_DONATION_RANKINGS, 0);
        setSummary(mPaid, mPercent, mRankings);

        if (mPaid > 0 && mDonationPrefs.getLong(KEY_DONATION_LAST_CHECK_TIME, 0) + DateUtils.DAY_IN_MILLIS < System.currentTimeMillis()
                || !mDonationPrefs.getBoolean(KEY_DONATION_FIRST_CHECK_COMPLETED, false)) {
            fetchRankingsInfo();
        }
    }

    public void updateRankingsInfo() {
        setSummary(MKCenterApplication.getInstance().getDonationInfo().getPaid(), mPercent, mRankings);
        fetchRankingsInfo();
    }

    private void fetchRankingsInfo() {
        OkGo.<String>post(getContext().getString(R.string.conf_fetch_donation_ranking_url_def))
                .tag(TAG).params(PARAM_UNIQUE_IDS, Build.getUniqueIDS(getContext())).execute(new StringCallback() {
            @Override
            public void onSuccess(Response<String> response) {
                if (!TextUtils.isEmpty(response.body())) {
                    Gson gson = new Gson();
                    try {
                        RankingsInfo rankingsInfo = gson.fromJson(response.body(), RankingsInfo.class);
                        if (rankingsInfo != null) {
                            mDonationPrefs.edit()
                                    .putInt(KEY_DONATION_AMOUNT, rankingsInfo.getAmount())
                                    .putInt(KEY_DONATION_PERCENT, rankingsInfo.getPercent())
                                    .putInt(KEY_DONATION_RANKINGS, rankingsInfo.getRankings())
                                    .putLong(KEY_DONATION_LAST_CHECK_TIME, System.currentTimeMillis())
                                    .putBoolean(KEY_DONATION_FIRST_CHECK_COMPLETED, true).apply();
                            mPercent = rankingsInfo.getPercent();
                            mRankings = rankingsInfo.getRankings();
                            setSummary(rankingsInfo.getAmount(), rankingsInfo.getPercent(), rankingsInfo.getRankings());
                        }
                    } catch (IllegalStateException | JsonSyntaxException ex) {
                        Log.e(TAG, ex.getMessage());
                    }
                }
            }
        });
    }

    private void setSummary(int paid, int percent, int rankings) {
        if (paid == 0) {
            setSummary(R.string.donation_record_none);
        } else if (rankings == 0) {
            setSummary(getContext().getString(R.string.donation_record_without_rankings, paid));
        } else {
            setSummary(getContext().getString(R.string.donation_record_with_rankings, paid, percent + "%", rankings));
        }
    }
}
