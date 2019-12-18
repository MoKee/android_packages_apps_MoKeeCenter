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

package com.mokee.center.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.model.HttpParams;
import com.mokee.center.MKCenterApplication;
import com.mokee.center.R;
import com.mokee.center.misc.Constants;
import com.mokee.center.model.DonationInfo;
import com.mokee.os.Build;

import static com.mokee.center.misc.Constants.AVAILABLE_UPDATES_TAG;
import static com.mokee.center.misc.Constants.PREF_INCREMENTAL_UPDATES;
import static com.mokee.center.misc.Constants.PREF_UPDATE_TYPE;
import static com.mokee.center.misc.Constants.PREF_VERIFIED_UPDATES;

public class RequestUtil {

    public static void fetchAvailableUpdates(Context context, StringCallback callback) {
        HttpParams params = buildParams(context);
        DonationInfo donationInfo = MKCenterApplication.getInstance().getDonationInfo();
        SharedPreferences mMainPrefs = CommonUtil.getMainPrefs(context);

        // Reset update type for premium version or different version
        String suggestUpdateType = BuildInfoUtil.getSuggestUpdateType();
        String configUpdateType = mMainPrefs.getString(PREF_UPDATE_TYPE, suggestUpdateType);
        if (!donationInfo.isBasic() && !TextUtils.equals(suggestUpdateType, configUpdateType)) {
            configUpdateType = suggestUpdateType;
            mMainPrefs.edit().putString(PREF_UPDATE_TYPE, configUpdateType).apply();
        }

        String url;
        if (mMainPrefs.getBoolean(PREF_INCREMENTAL_UPDATES, false) && donationInfo.isBasic() && !CommonUtil.isABDevice()) {
            url = context.getString(R.string.conf_fetch_ota_update_url_def);
            params.put("build_user", android.os.Build.USER);
        } else {
            url = context.getString(R.string.conf_fetch_firmware_update_url_def);
            mMainPrefs.edit().putBoolean(PREF_INCREMENTAL_UPDATES, false).apply();
        }

        if (mMainPrefs.getBoolean(PREF_VERIFIED_UPDATES, false) && donationInfo.isAdvanced()) {
            params.put("is_verified", 1);
        } else {
            mMainPrefs.edit().putBoolean(PREF_VERIFIED_UPDATES, false).apply();
        }

        params.put("update_type", configUpdateType);
        params.put("version", Build.VERSION);

        OkGo.<String>post(url).tag(AVAILABLE_UPDATES_TAG).params(params).execute(callback);
    }

    public static HttpParams buildParams(Context context) {
        HttpParams params = new HttpParams();
        params.put("license", CommonUtil.loadLicense(Constants.LICENSE_FILE));
        params.put("unique_ids", Build.getUniqueIDS(context));
        return params;
    }

}
