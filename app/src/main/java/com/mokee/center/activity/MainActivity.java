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

package com.mokee.center.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.mokee.center.MKCenterApplication;
import com.mokee.center.R;
import com.mokee.center.dialog.DonationDialogBuilder;
import com.mokee.center.misc.Constants;
import com.mokee.center.util.CommonUtils;

import static com.mokee.center.misc.Constants.DONATION_RESULT_FAILURE;
import static com.mokee.center.misc.Constants.DONATION_RESULT_NOT_FOUND;
import static com.mokee.center.misc.Constants.DONATION_RESULT_OK;
import static com.mokee.center.misc.Constants.DONATION_RESULT_SUCCESS;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
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
                makeSnackbar(R.string.donation_restore_not_found).setAction(R.string.solution, (view) -> {
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        if (MKCenterApplication.getInstance().getDonationInfo().isAdvanced()) {
            menu.findItem(R.id.action_donation).setTitle(R.string.action_donation);
        } else {
            menu.findItem(R.id.action_donation).setTitle(R.string.action_unlock_features);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                return true;
            case R.id.action_donation:
                new DonationDialogBuilder(this).show();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.nav_forum:
                CommonUtils.openLink(this, Constants.NAV_FORUM_URL);
                break;
            case R.id.nav_bug_reports:
                CommonUtils.openLink(this, Constants.NAV_BUG_REPORTS_URL);
                break;
            case R.id.nav_open_source:
                CommonUtils.openLink(this, Constants.NAV_OPEN_SOURCE_URL);
                break;
            case R.id.nav_code_review:
                CommonUtils.openLink(this, Constants.NAV_CODE_REVIEW_URL);
                break;
            case R.id.nav_translate:
                CommonUtils.openLink(this, Constants.NAV_TRANSLATE_URL);
                break;
            case R.id.nav_weibo:
                CommonUtils.openLink(this, Constants.NAV_WEIBO_URL);
                break;
            case R.id.nav_qqchat:
                CommonUtils.openLink(this, Constants.NAV_QQCHAT_URL);
                break;
            case R.id.nav_telegram:
                CommonUtils.openLink(this, Constants.NAV_TELEGRAM_URL);
                break;
        }
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public Snackbar makeSnackbar(CharSequence text) {
        return makeSnackbar(text, Snackbar.LENGTH_SHORT);
    }

    public Snackbar makeSnackbar(@StringRes int resId) {
        return makeSnackbar(resId, Snackbar.LENGTH_SHORT);
    }

    public Snackbar makeSnackbar(CharSequence text, int duration) {
        return Snackbar.make(findViewById(R.id.updater), text, duration);
    }

    public Snackbar makeSnackbar(@StringRes int resId, int duration) {
        return Snackbar.make(findViewById(R.id.updater), resId, duration);
    }

}
