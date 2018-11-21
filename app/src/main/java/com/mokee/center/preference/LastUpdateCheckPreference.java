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
import android.support.v7.preference.Preference;
import android.text.format.DateUtils;
import android.util.AttributeSet;

import com.mokee.center.R;
import com.mokee.center.misc.Constants;
import com.mokee.center.util.CommonUtil;

public class LastUpdateCheckPreference extends Preference {

    private SharedPreferences mMainPrefs;

    public LastUpdateCheckPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mMainPrefs = CommonUtil.getMainPrefs(context);
    }

    public void updateSummary() {
        long lastCheckTime = mMainPrefs.getLong(Constants.PREF_LAST_UPDATE_CHECK, 0);
        if (lastCheckTime == 0) {
            setSummary(R.string.never);
        } else {
            setSummary(DateUtils.formatDateTime(getContext(), lastCheckTime, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_YEAR));
        }
    }

    @Override
    public void onAttached() {
        super.onAttached();
        updateSummary();
    }
}
