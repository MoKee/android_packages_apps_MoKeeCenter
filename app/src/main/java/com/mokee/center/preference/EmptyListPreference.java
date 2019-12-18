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

import com.mokee.center.R;
import com.mokee.center.misc.Constants;
import com.mokee.center.util.CommonUtil;

public class EmptyListPreference extends Preference {

    private SharedPreferences mMainPrefs;

    public EmptyListPreference(Context context) {
        super(context);
        mMainPrefs = CommonUtil.getMainPrefs(context);
    }

    @Override
    public void onAttached() {
        super.onAttached();
        setEnabled(false);
        setLayoutResource(R.layout.preference_empty_list);

        setSummary(mMainPrefs.getBoolean(Constants.PREF_OUT_OF_DATE, false)
                ? R.string.out_of_date_updates_intro : R.string.no_available_updates_intro);
    }

}
