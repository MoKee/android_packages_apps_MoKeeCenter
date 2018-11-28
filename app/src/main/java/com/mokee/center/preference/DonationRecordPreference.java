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
import androidx.preference.Preference;
import android.text.format.DateUtils;
import android.util.AttributeSet;

import com.google.gson.Gson;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.model.Response;
import com.mokee.center.MKCenterApplication;
import com.mokee.center.R;
import com.mokee.center.model.RankInfo;
import com.mokee.center.util.CommonUtil;
import com.mokee.os.Build;

import static com.mokee.center.misc.Constants.KEY_DONATION_AMOUNT;
import static com.mokee.center.misc.Constants.KEY_DONATION_FIRST_CHECK_COMPLETED;
import static com.mokee.center.misc.Constants.KEY_DONATION_LAST_CHECK_TIME;
import static com.mokee.center.misc.Constants.KEY_DONATION_PERCENT;
import static com.mokee.center.misc.Constants.KEY_DONATION_RANK;
import static com.mokee.center.misc.Constants.PARAM_UNIQUE_IDS;

public class DonationRecordPreference extends Preference {

    private static final String TAG = DonationRecordPreference.class.getName();

    private SharedPreferences mDonationPrefs;
    private int mPaid, mPercent, mRank;

    public DonationRecordPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mDonationPrefs = CommonUtil.getDonationPrefs(context);
    }

    @Override
    public void onAttached() {
        super.onAttached();
        mPaid = MKCenterApplication.getInstance().getDonationInfo().getPaid();
        mPercent = mDonationPrefs.getInt(KEY_DONATION_PERCENT, 0);
        mRank = mDonationPrefs.getInt(KEY_DONATION_RANK, 0);
        setSummary(mPaid, mPercent, mRank);

        if (mPaid > 0 && mDonationPrefs.getLong(KEY_DONATION_LAST_CHECK_TIME, 0) + DateUtils.DAY_IN_MILLIS < System.currentTimeMillis()
                || !mDonationPrefs.getBoolean(KEY_DONATION_FIRST_CHECK_COMPLETED, false)) {
            fetchRankInfo();
        }
    }

    public void updateRankInfo() {
        setSummary(MKCenterApplication.getInstance().getDonationInfo().getPaid(), mPercent, mRank);
        fetchRankInfo();
    }

    public void fetchRankInfo() {
        OkGo.<String>post(getContext().getString(R.string.conf_fetch_donation_ranking_url_def))
                .tag(TAG).params(PARAM_UNIQUE_IDS, Build.getUniqueIDS(getContext())).execute(new StringCallback() {
            @Override
            public void onSuccess(Response<String> response) {
                Gson gson = new Gson();
                RankInfo rankInfo = gson.fromJson(response.body(), RankInfo.class);
                if (rankInfo != null) {
                    mDonationPrefs.edit()
                            .putInt(KEY_DONATION_AMOUNT, rankInfo.getAmount())
                            .putInt(KEY_DONATION_PERCENT, rankInfo.getPercent())
                            .putInt(KEY_DONATION_RANK, rankInfo.getRank())
                            .putLong(KEY_DONATION_LAST_CHECK_TIME, System.currentTimeMillis())
                            .putBoolean(KEY_DONATION_FIRST_CHECK_COMPLETED, true).apply();
                    mPercent = rankInfo.getPercent();
                    mRank = rankInfo.getRank();
                    setSummary(rankInfo.getAmount(), rankInfo.getPercent(), rankInfo.getRank());
                }
            }
        });
    }

    private void setSummary(int paid, int percent, int rank) {
        if (paid == 0) {
            setSummary(R.string.donation_record_none);
        } else if (rank == 0) {
            setSummary(getContext().getString(R.string.donation_record_without_rank, paid));
        } else {
            setSummary(getContext().getString(R.string.donation_record_with_rank, paid, percent + "%", rank));
        }
    }
}
