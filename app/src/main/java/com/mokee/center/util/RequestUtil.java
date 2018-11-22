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

package com.mokee.center.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.model.HttpParams;
import com.mokee.center.MKCenterApplication;
import com.mokee.center.R;
import com.mokee.center.model.DonationInfo;
import com.mokee.os.Build;
import com.mokee.security.RSAUtils;

import static com.mokee.center.misc.Constants.AVAILABLE_UPDATES_TAG;
import static com.mokee.center.misc.Constants.PREF_INCREMENTAL_UPDATES;
import static com.mokee.center.misc.Constants.PREF_UPDATE_TYPE;
import static com.mokee.center.misc.Constants.PREF_VERIFIED_UPDATES;

public class RequestUtil {

    public static void fetchAvailableUpdates(Context context, StringCallback callback) {
        HttpParams params = new HttpParams();
        DonationInfo donationInfo = MKCenterApplication.getInstance().getDonationInfo();
        SharedPreferences mMainPrefs = CommonUtil.getMainPrefs(context);

        String suggestUpdateType = BuildInfoUtil.getSuggestUpdateType();
        String configUpdateType = mMainPrefs.getString(PREF_UPDATE_TYPE, String.valueOf(suggestUpdateType));
        // Reset update type for unofficial version or different version
        if (!suggestUpdateType.equals("3") && configUpdateType.equals("3")
                || !donationInfo.isBasic() && !TextUtils.equals(suggestUpdateType, configUpdateType)) {
            configUpdateType = String.valueOf(suggestUpdateType);
            mMainPrefs.edit().putString(PREF_UPDATE_TYPE, configUpdateType).apply();
        }

        String url;
        if (mMainPrefs.getBoolean(PREF_INCREMENTAL_UPDATES, false) && donationInfo.isBasic()) {
            url = context.getString(R.string.conf_fetch_ota_update_url_def);
        } else {
            url = context.getString(R.string.conf_fetch_full_update_url_def);
            mMainPrefs.edit().putBoolean(PREF_INCREMENTAL_UPDATES, false).apply();
            params.put("user_id", Build.getUniqueID(context));
            params.put("device_official", configUpdateType);
        }

        if (mMainPrefs.getBoolean(PREF_VERIFIED_UPDATES, false) && donationInfo.isAdvanced()) {
            params.put("is_verified", 1);
        } else {
            mMainPrefs.edit().putBoolean(PREF_VERIFIED_UPDATES, false).apply();
        }

        try {
            params.put("device_name", RSAUtils.rsaEncryptByPublicKey(Build.PRODUCT));
            params.put("device_version", RSAUtils.rsaEncryptByPublicKey(Build.VERSION));
        } catch (Exception e) {
            e.printStackTrace();
        }
        params.put("build_user", android.os.Build.USER);
        params.put("is_encrypted", 1);

        OkGo.<String>post(url).tag(AVAILABLE_UPDATES_TAG).params(params).execute(callback);
    }
}
