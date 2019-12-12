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

package com.mokee.center;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.lzy.okgo.OkGo;
import com.lzy.okgo.interceptor.HttpLoggingInterceptor;
import com.lzy.okgo.model.HttpHeaders;
import com.mokee.center.activity.MainActivity;
import com.mokee.center.model.DonationInfo;
import com.mokee.center.util.CommonUtil;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import cn.jpush.android.api.BasicPushNotificationBuilder;
import cn.jpush.android.api.JPushInterface;
import okhttp3.OkHttpClient;

import static com.mokee.center.misc.Constants.USER_AGENT;

public class MKCenterApplication extends Application implements
        Application.ActivityLifecycleCallbacks {

    public static final List<String> WHITELIST_HOSTNAME = Arrays.asList("api.mokeedev.com");
    private static MKCenterApplication mApp;
    private DonationInfo mDonationInfo = new DonationInfo();
    private OkHttpClient.Builder builder = new OkHttpClient.Builder();
    private boolean mMainActivityActive;

    public static synchronized MKCenterApplication getInstance() {
        return mApp;
    }

    public DonationInfo getDonationInfo() {
        return mDonationInfo;
    }

    public OkHttpClient.Builder getClient() {
        return builder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mMainActivityActive = false;
        registerActivityLifecycleCallbacks(this);
        mApp = this;
        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            CommonUtil.updateDonationInfo(this);
        }
        initOkGo();
        initJPush();
    }

    private void initJPush() {
        JPushInterface.setDebugMode(BuildConfig.DEBUG);
        JPushInterface.init(this);
        JPushInterface.setStatisticsEnable(false);
        JPushInterface.setPowerSaveMode(this, true);
        JPushInterface.stopCrashHandler(this);
        BasicPushNotificationBuilder builder = new BasicPushNotificationBuilder(this);
        builder.statusBarDrawable = R.drawable.ic_push_notify;
        JPushInterface.setDefaultPushNotificationBuilder(builder);
    }

    private void initOkGo() {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.put("User-Agent", USER_AGENT);

        //log
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor("OkGo");
        loggingInterceptor.setPrintLevel(HttpLoggingInterceptor.Level.NONE);
        builder.addInterceptor(loggingInterceptor);
        //default timeout
        builder.connectTimeout(30, TimeUnit.SECONDS);
        builder.hostnameVerifier(new SafeHostnameVerifier());

        OkGo.getInstance().init(this)
                .setOkHttpClient(builder.build())
                .addCommonHeaders(httpHeaders);
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(Activity activity) {
        if (activity instanceof MainActivity) {
            mMainActivityActive = true;
        }
    }

    @Override
    public void onActivityResumed(Activity activity) {
    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {
        if (activity instanceof MainActivity) {
            mMainActivityActive = false;
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
    }

    public boolean isMainActivityActive() {
        return mMainActivityActive;
    }

    private class SafeHostnameVerifier implements HostnameVerifier {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            return WHITELIST_HOSTNAME.contains(hostname);
        }
    }

}
