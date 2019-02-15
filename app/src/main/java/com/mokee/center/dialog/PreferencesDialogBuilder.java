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
package com.mokee.center.dialog;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Switch;

import com.mokee.center.R;
import com.mokee.center.misc.Constants;
import com.mokee.center.util.CommonUtil;

public class PreferencesDialogBuilder extends AlertDialog.Builder {

    public PreferencesDialogBuilder(@NonNull Context context) {
        super(context);
    }

    @Override
    public AlertDialog create() {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.preferences_dialog, null);
        Switch autoDelete = view.findViewById(R.id.preferences_auto_delete_updates);
        Switch dataWarning = view.findViewById(R.id.preferences_mobile_data_warning);
        Switch abPerfMode = view.findViewById(R.id.preferences_ab_perf_mode);

        if (!CommonUtil.isABDevice()) {
            abPerfMode.setVisibility(View.GONE);
        }

        SharedPreferences prefs = CommonUtil.getMainPrefs(getContext());
        autoDelete.setChecked(prefs.getBoolean(Constants.PREF_AUTO_DELETE_UPDATES, false));
        dataWarning.setChecked(prefs.getBoolean(Constants.PREF_MOBILE_DATA_WARNING, true));
        abPerfMode.setChecked(prefs.getBoolean(Constants.PREF_AB_PERF_MODE, false));

        setTitle(R.string.menu_preferences);
        setView(view);
        setOnDismissListener(dialogInterface -> {
            prefs.edit()
                    .putBoolean(Constants.PREF_AUTO_DELETE_UPDATES,
                            autoDelete.isChecked())
                    .putBoolean(Constants.PREF_MOBILE_DATA_WARNING,
                            dataWarning.isChecked())
                            .putBoolean(Constants.PREF_AB_PERF_MODE,
                                    abPerfMode.isChecked())
                    .apply();

//                    boolean enableABPerfMode = abPerfMode.isChecked();
//                    mUpdaterService.getUpdaterController().setPerformanceMode(enableABPerfMode);
        });

        return super.create();
    }
}
