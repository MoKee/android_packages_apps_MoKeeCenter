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

package com.mokee.center.misc;

import android.app.Activity;
import android.os.Environment;

public class Constants {

    public static final String USER_AGENT = "com.mokee.center/2.0";

    // Actions
    public static final String ACTION_PAYMENT_REQUEST = "com.mokee.pay.action.PAYMENT_REQUEST";
    public static final String ACTION_RESTORE_REQUEST = "com.mokee.pay.action.RESTORE_REQUEST";

    // Nav URLs
    public static final String NAV_FORUM_URL = "https://bbs.mokeedev.com/";
    public static final String NAV_GUIDE_URL = "https://www.mokeedev.com/guide/feature.html";
    public static final String NAV_BUG_REPORTS_URL = "https://bbs.mokeedev.com/c/bug-reports";
    public static final String NAV_OPEN_SOURCE_URL = "https://github.com/MoKee";
    public static final String NAV_CODE_REVIEW_URL = "https://mokeedev.review";
    public static final String NAV_TRANSLATE_URL = "http://translate.mokeedev.com";
    public static final String NAV_WEIBO_URL = "https://weibo.com/martincz";
    public static final String NAV_TELEGRAM_URL = "https://t.me/mokeecommunity";
    public static final String NAV_QQCHAT_URL = "https://bbs.mokeedev.com/t/topic/9551/1";

    // License
    public static final String LICENSE_FILE = Environment.getExternalStorageDirectory().getAbsolutePath() + "/mokee.license";
    public static final String LICENSE_PUB_KEY =
            "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCwN8FdvNOu5A8yP2Bfb7rk1o6N" +
                    "dXik/DO+Kw6+q7nIZjTh4qpPL3Gyoa7A3MI01gTRKaM+MU2+zkiZND8qoB8EGlF6" +
                    "BfDfi9BLyFyx+nOTgz3KDEYutLJhopS18DfrdZTohNXsM7+MEsk5y+GHFjYHePXN" +
                    "oE4fjtfCg3xbtwU29wIDAQAB";

    // Donation
    public static final String DONATION_PREF = "DonationPrefs";
    public static final int DONATION_MAX = 1000;
    public static final int DONATION_ADVANCED = 68;
    public static final int DONATION_BASIC = 30;
    public static final int DONATION_MIN = 10;
    public static final int DONATION_RESULT_OK = Activity.RESULT_OK;
    public static final int DONATION_RESULT_SUCCESS = 200;
    public static final int DONATION_RESULT_NOT_FOUND = 500;
    public static final int DONATION_RESULT_FAILURE = 408;

    // Donation Keys
    public static final String KEY_DONATION_PERCENT = "percent";
    public static final String KEY_DONATION_RANK = "rank";
    public static final String KEY_DONATION_AMOUNT = "amount";
    public static final String KEY_DONATION_FIRST_CHECK_COMPLETED = "first_check_completed";
    public static final String KEY_DONATION_LAST_CHECK_TIME = "last_check_time";

    // Prefs
    public static final String PREF_DONATION_RECORD = "donation_record";
    public static final String PREF_LAST_UPDATE_CHECK = "last_update_check";
    public static final String PREF_AUTO_DELETE_UPDATES = "auto_delete_updates";
    public static final String PREF_AB_PERF_MODE = "ab_perf_mode";
    public static final String PREF_UPDATE_TYPE = "update_type";
    public static final String PREF_VERIFIED_UPDATES = "verified_updates";
    public static final String PREF_INCREMENTAL_UPDATES = "incremental_updates";
    public static final String PREF_UPDATES_CATEGORY = "updates_category";
    public static final String PREF_MOBILE_DATA_WARNING = "pref_mobile_data_warning";

    // HTTP Params
    public static final String PARAM_UNIQUE_IDS = "user_ids";

    // Request Tags
    public static final String AVAILABLE_UPDATES_TAG = "FetchAvailableUpdates";

    // Props
    public static final String PROP_AB_DEVICE = "ro.build.ab_update";

}
