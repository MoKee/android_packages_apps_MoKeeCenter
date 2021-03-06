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
import androidx.preference.SwitchPreference;
import android.text.TextUtils;
import android.util.AttributeSet;

import com.mokee.center.MKCenterApplication;
import com.mokee.center.R;
import com.mokee.center.model.DonationInfo;

import java.util.Arrays;

import static com.mokee.center.misc.Constants.DONATION_BASIC;

public class IncrementalUpdatesPreference extends SwitchPreference {

    public IncrementalUpdatesPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onAttached() {
        super.onAttached();
        refreshPreference();
    }

    public void refreshPreference() {
        DonationInfo donationInfo = MKCenterApplication.getInstance().getDonationInfo();
        if (!donationInfo.isBasic()) {
            if (isChecked()) {
                setChecked(false);
            }
            if (donationInfo.getPaid() == 0f) {
                setSummary(TextUtils.join(" ", Arrays.asList(getContext().getString(R.string.incremental_updates_summary),
                        getContext().getString(R.string.unlock_features_request_summary, DONATION_BASIC))));
            } else {
                setSummary(TextUtils.join(" ", Arrays.asList(getContext().getString(R.string.incremental_updates_summary),
                        getContext().getString(R.string.unlock_features_pending_summary, donationInfo.getPaid(), DONATION_BASIC - donationInfo.getPaid()))));
            }
        } else {
            setSummary(R.string.incremental_updates_summary);
        }
    }

    @Override
    public boolean callChangeListener(Object newValue) {
        return super.callChangeListener(newValue);
    }
}
