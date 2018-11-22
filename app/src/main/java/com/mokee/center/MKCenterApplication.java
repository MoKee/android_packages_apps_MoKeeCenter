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

package com.mokee.center;

import android.app.Application;

import com.lzy.okgo.OkGo;
import com.lzy.okgo.interceptor.HttpLoggingInterceptor;
import com.lzy.okgo.model.HttpHeaders;
import com.mokee.center.model.DonationInfo;
import com.mokee.center.util.CommonUtil;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import okhttp3.OkHttpClient;

import static com.mokee.center.misc.Constants.USER_AGENT;

public class MKCenterApplication extends Application {

    private static MKCenterApplication mApp;

    private DonationInfo mDonationInfo = new DonationInfo();

    private OkHttpClient.Builder builder = new OkHttpClient.Builder();

    public static synchronized MKCenterApplication getInstance() {
        return mApp;
    }

    public DonationInfo getDonationInfo() {
        return mDonationInfo;
    }

    public OkHttpClient.Builder getClient() {
        return builder;
    }

    public static final List<String> WHITELIST_HOSTNAME = Arrays.asList("ota.mokeedev.com", "cloud.mokeedev.com");

    @Override
    public void onCreate() {
        super.onCreate();
        mApp = this;
        CommonUtil.updateDonationInfo(this);
        initOkGo();
    }

    private void initOkGo() {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.put("User-Agent", USER_AGENT);

        //log
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor("OkGo");
        loggingInterceptor.setPrintLevel(HttpLoggingInterceptor.Level.NONE);
        builder.addInterceptor(loggingInterceptor);
        //default timeout
        builder.connectTimeout(15, TimeUnit.SECONDS);
        builder.hostnameVerifier(new SafeHostnameVerifier());

        OkGo.getInstance().init(this)
                .setOkHttpClient(builder.build())
                .addCommonHeaders(httpHeaders);
    }

    private class SafeHostnameVerifier implements HostnameVerifier {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            return WHITELIST_HOSTNAME.contains(hostname);
        }
    }

}
