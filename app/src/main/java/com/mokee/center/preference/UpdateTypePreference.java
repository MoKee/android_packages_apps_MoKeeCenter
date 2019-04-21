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
import android.content.res.Resources;
import androidx.preference.ListPreference;
import android.text.TextUtils;
import android.util.AttributeSet;

import com.mokee.center.MKCenterApplication;
import com.mokee.center.R;
import com.mokee.center.model.DonationInfo;
import com.mokee.center.util.BuildInfoUtil;
import com.mokee.center.util.CommonUtil;

import static com.mokee.center.misc.Constants.PREF_UPDATE_TYPE;

public class UpdateTypePreference extends ListPreference {

    private SharedPreferences mMainPrefs;

    public UpdateTypePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mMainPrefs = CommonUtil.getMainPrefs(context);
    }

    @Override
    public void onAttached() {
        super.onAttached();
        refreshPreference();
    }

    public void refreshPreference() {
        Resources resources = getContext().getResources();
        DonationInfo donationInfo = MKCenterApplication.getInstance().getDonationInfo();
        // Reset update type for premium version or different version
        String suggestUpdateType = BuildInfoUtil.getSuggestUpdateType();
        String configUpdateType = mMainPrefs.getString(PREF_UPDATE_TYPE, suggestUpdateType);
        if (!suggestUpdateType.equals("3") && configUpdateType.equals("3")
                || !donationInfo.isBasic() && !TextUtils.equals(suggestUpdateType, configUpdateType)) {
            configUpdateType = suggestUpdateType;
            mMainPrefs.edit().putString(PREF_UPDATE_TYPE, configUpdateType).apply();
        }

        if (donationInfo.isAdvanced() || suggestUpdateType.equals("3")) {
            setEntries(resources.getStringArray(R.array.all_type_entries));
            setEntryValues(resources.getStringArray(R.array.all_type_values));
        } else {
            setEntries(resources.getStringArray(R.array.normal_type_entries));
            setEntryValues(resources.getStringArray(R.array.normal_type_values));
        }
        setValue(configUpdateType);
        setSummary(getEntries()[findIndexOfValue(configUpdateType)]);
    }

    @Override
    public boolean callChangeListener(Object newValue) {
        setSummary(getEntries()[findIndexOfValue(String.valueOf(newValue))]);
        return super.callChangeListener(newValue);
    }
}
