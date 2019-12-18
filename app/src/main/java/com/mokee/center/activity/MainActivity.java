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

package com.mokee.center.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.mokee.center.BuildConfig;
import com.mokee.center.MKCenterApplication;
import com.mokee.center.R;
import com.mokee.center.dialog.DonationDialogBuilder;
import com.mokee.center.misc.Constants;
import com.mokee.center.util.CommonUtil;

import io.fabric.sdk.android.Fabric;

import static com.mokee.center.misc.Constants.DONATION_RESULT_FAILURE;
import static com.mokee.center.misc.Constants.DONATION_RESULT_NOT_FOUND;
import static com.mokee.center.misc.Constants.DONATION_RESULT_OK;
import static com.mokee.center.misc.Constants.DONATION_RESULT_SUCCESS;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private final int PERMISSION_REQUEST_CODE = 1;
    private MKCenterApplication mApp;
    private int mIndexPermissionRequestStorageWrite = 0;
    private int mIndexPermissionRequestStorageRead = 0;
    private int mIndexPermissionRequestPhone = 0;

    private boolean mShouldRequestStoragePermission = false;
    private boolean mShouldRequestPhonePermission = false;
    private int mNumPermissionsToRequest = 0;
    private boolean mFlagHasStoragePermission = false;
    private boolean mFlagHasPhonePermission = false;
    private boolean mCriticalPermissionDenied = false;

    private InterstitialAd mWelcomeInterstitialAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mApp = MKCenterApplication.getInstance();
        // Set up Crashlytics, disabled for debug builds
        Crashlytics crashlyticsKit = new Crashlytics.Builder()
                .core(new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build())
                .build();

        // Initialize Fabric with the debug-disabled crashlytics.
        Fabric.with(this, crashlyticsKit);
        checkPermissions();
    }

    private void checkPermissions() {
        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            mNumPermissionsToRequest++;
            mShouldRequestPhonePermission = true;
        } else {
            mFlagHasPhonePermission = true;
        }
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            mNumPermissionsToRequest = mNumPermissionsToRequest + 2;
            mShouldRequestStoragePermission = true;
        } else {
            mFlagHasStoragePermission = true;
        }
        if (mNumPermissionsToRequest != 0) {
            buildPermissionsRequest();
        } else {
            handlePermissionsSuccess();
        }
    }

    private void handlePermissionsSuccess() {
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        if (!mApp.getDonationInfo().isAdvanced()) {
            MobileAds.initialize(this, getString(R.string.app_id));
            mWelcomeInterstitialAd = new InterstitialAd(this);
            mWelcomeInterstitialAd.setAdUnitId(getString(R.string.interstitial_ad_unit_id));
            mWelcomeInterstitialAd.loadAd(new AdRequest.Builder().build());
            mWelcomeInterstitialAd.setAdListener(new AdListener() {
                @Override
                public void onAdLoaded() {
                    if (mApp.isMainActivityActive()) {
                        mWelcomeInterstitialAd.show();
                    }
                }
            });
        }
    }

    private void handlePermissionsFailure() {
        if (!mFlagHasStoragePermission) {
            Toast.makeText(this, R.string.no_storage_permissions, Toast.LENGTH_LONG).show();
        } else if (!mFlagHasPhonePermission) {
            Toast.makeText(this, R.string.no_phone_permissions, Toast.LENGTH_LONG).show();
        }
        finish();
    }

    private void buildPermissionsRequest() {
        String[] permissionsToRequest = new String[mNumPermissionsToRequest];
        int permissionsRequestIndex = 0;

        if (mShouldRequestStoragePermission) {
            permissionsToRequest[permissionsRequestIndex] = Manifest.permission.WRITE_EXTERNAL_STORAGE;
            mIndexPermissionRequestStorageWrite = permissionsRequestIndex;
            permissionsRequestIndex++;
            permissionsToRequest[permissionsRequestIndex] = Manifest.permission.READ_EXTERNAL_STORAGE;
            mIndexPermissionRequestStorageRead = permissionsRequestIndex;
            permissionsRequestIndex++;
        }

        if (mShouldRequestPhonePermission) {
            permissionsToRequest[permissionsRequestIndex] = Manifest.permission.READ_PHONE_STATE;
            mIndexPermissionRequestPhone = permissionsRequestIndex;
        }

        requestPermissions(permissionsToRequest, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (mShouldRequestStoragePermission) {
            if ((grantResults.length >= mIndexPermissionRequestStorageRead + 1) &&
                    (grantResults[mIndexPermissionRequestStorageWrite] == PackageManager.PERMISSION_GRANTED) &&
                    (grantResults[mIndexPermissionRequestStorageRead] == PackageManager.PERMISSION_GRANTED)) {
                mFlagHasStoragePermission = true;
            } else {
                mCriticalPermissionDenied = true;
            }
        }
        if (mShouldRequestPhonePermission) {
            if ((grantResults.length >= mIndexPermissionRequestPhone + 1) &&
                    (grantResults[mIndexPermissionRequestPhone] == PackageManager.PERMISSION_GRANTED)) {
                mFlagHasPhonePermission = true;
            } else {
                mCriticalPermissionDenied = true;
            }
        }
        if (mFlagHasStoragePermission && mFlagHasPhonePermission) {
            handlePermissionsSuccess();
        } else if (mCriticalPermissionDenied) {
            handlePermissionsFailure();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (resultCode) {
            case DONATION_RESULT_OK:
                invalidateOptionsMenu();
                makeSnackbar(R.string.donation_payment_success).show();
                break;
            case DONATION_RESULT_SUCCESS:
                invalidateOptionsMenu();
                makeSnackbar(R.string.donation_restore_success).show();
                break;
            case DONATION_RESULT_FAILURE:
                invalidateOptionsMenu();
                makeSnackbar(R.string.donation_restore_failure).show();
                break;
            case DONATION_RESULT_NOT_FOUND:
                makeSnackbar(R.string.donation_restore_not_found).setAction(R.string.action_solution, (view) -> {
                    Uri uri = Uri.parse("https://bbs.mokeedev.com/t/topic/577");
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                }).show();
                break;
        }
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            if (fragment != null) {
                fragment.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        if (mApp.getDonationInfo().isAdvanced()) {
            menu.findItem(R.id.menu_donation).setTitle(R.string.menu_donation);
        } else {
            menu.findItem(R.id.menu_donation).setTitle(R.string.menu_unlock_features);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_donation:
                new DonationDialogBuilder(this).show();
                return true;
            case R.id.menu_restore:
                CommonUtil.restoreLicenseRequest(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.nav_forum:
                CommonUtil.openLink(this, Constants.NAV_FORUM_URL);
                break;
            case R.id.nav_guide:
                CommonUtil.openLink(this, Constants.NAV_GUIDE_URL);
                break;
            case R.id.nav_bug_reports:
                CommonUtil.openLink(this, Constants.NAV_BUG_REPORTS_URL);
                break;
            case R.id.nav_open_source:
                CommonUtil.openLink(this, Constants.NAV_OPEN_SOURCE_URL);
                break;
            case R.id.nav_team:
                CommonUtil.openLink(this, Constants.NAV_TEAM_URL);
                break;
            case R.id.nav_code_review:
                CommonUtil.openLink(this, Constants.NAV_CODE_REVIEW_URL);
                break;
            case R.id.nav_translate:
                CommonUtil.openLink(this, Constants.NAV_TRANSLATE_URL);
                break;
            case R.id.nav_weibo:
                CommonUtil.openLink(this, Constants.NAV_WEIBO_URL);
                break;
            case R.id.nav_qqchat:
                CommonUtil.openLink(this, Constants.NAV_QQCHAT_URL);
                break;
            case R.id.nav_telegram:
                CommonUtil.openLink(this, Constants.NAV_TELEGRAM_URL);
                break;
        }
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return false;
    }

    public Snackbar makeSnackbar(@StringRes int resId) {
        return makeSnackbar(resId, Snackbar.LENGTH_SHORT);
    }

    public Snackbar makeSnackbar(@StringRes int resId, int duration) {
        return Snackbar.make(findViewById(R.id.updater), resId, duration);
    }
}
