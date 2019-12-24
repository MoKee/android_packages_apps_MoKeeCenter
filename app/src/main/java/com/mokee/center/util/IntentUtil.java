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

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.core.content.FileProvider;

import com.google.android.material.snackbar.Snackbar;
import com.mokee.center.BuildConfig;
import com.mokee.center.MKCenterApplication;
import com.mokee.center.R;
import com.mokee.center.misc.Constants;
import com.mokee.center.model.DonationInfo;
import com.mokee.security.License;
import com.mokee.security.LicenseUtils;
import com.mokee.utils.DonationUtils;

import java.io.File;

import static com.mokee.center.misc.Constants.ACTION_PAYMENT_REQUEST;

public class IntentUtil {

    public static void updateDonationInfo(Context context) {
        String licensePath = LicenseUtils.getLicensePath(context);
        DonationInfo donationInfo = MKCenterApplication.getInstance().getDonationInfo();
        DonationUtils.updateDonationInfo(context, donationInfo, licensePath, Constants.LICENSE_PUB_KEY);
        if (donationInfo.isBasic()) {
            License.LICENSE_APPLICATION_LIST.forEach(
                    pkg -> {
                        Intent intent = new Intent(Constants.ACTION_LICENSE_CHANGED);
                        Uri uri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".files", new File(licensePath));
                        intent.setDataAndType(uri, context.getContentResolver().getType(uri));
                        intent.setPackage(pkg);
                        context.grantUriPermission(pkg, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        context.sendBroadcast(intent);
                    }
            );
        }
    }

    public static void sendPaymentRequest(Activity context, String channel, String description, String price, String type) {
        Intent intent = new Intent(ACTION_PAYMENT_REQUEST);
        intent.putExtra("packagename", context.getPackageName());
        intent.putExtra("channel", channel);
        intent.putExtra("type", type);
        intent.putExtra("description", description);
        intent.putExtra("price", price);
        context.startActivityForResult(intent, 0);
    }

    public static void restoreLicenseRequest(Activity context) {
        try {
            Intent intent = new Intent(Constants.ACTION_RESTORE_REQUEST);
            context.startActivityForResult(intent, 0);
        } catch (ActivityNotFoundException ex) {
            Snackbar.make(context.findViewById(R.id.updater), R.string.mokeepay_not_found, Snackbar.LENGTH_LONG).show();
        }
    }
}
