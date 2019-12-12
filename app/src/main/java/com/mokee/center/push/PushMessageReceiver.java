/*
 * Copyright (C) 2019 The MoKee Open Source Project
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

package com.mokee.center.push;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.mokee.center.activity.AgentWebActivity;
import com.mokee.center.model.TagAliasBean;
import com.mokee.center.util.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import cn.jpush.android.api.JPushMessage;
import cn.jpush.android.api.NotificationMessage;
import cn.jpush.android.service.JPushMessageReceiver;

import static com.mokee.center.misc.Constants.KEY_PUSH_EXTRA_URL;
import static com.mokee.center.push.TagAliasOperatorHelper.ACTION_GET;
import static com.mokee.center.push.TagAliasOperatorHelper.sequence;

public class PushMessageReceiver extends JPushMessageReceiver {

    public static final String TAG = PushMessageReceiver.class.getSimpleName();

    @Override
    public void onNotifyMessageOpened(Context context, NotificationMessage message) {
        try {
            JSONObject jsonExtras = new JSONObject(message.notificationExtras);
            String url = jsonExtras.getString(KEY_PUSH_EXTRA_URL);
            if (!TextUtils.isEmpty(url)) {
                Intent intent = new Intent(context, AgentWebActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString(KEY_PUSH_EXTRA_URL, url);
                intent.putExtras(bundle);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                context.startActivity(intent);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConnected(Context context, boolean isConnected) {
        Logger.i(TAG, "[onConnected] " + isConnected);

        TagAliasBean tagAliasBean = new TagAliasBean();
        tagAliasBean.setAction(ACTION_GET);

        tagAliasBean.setAliasAction(true);
        TagAliasOperatorHelper.getInstance(context).handleAction(context, sequence++, tagAliasBean);

        tagAliasBean.setAliasAction(false);
        TagAliasOperatorHelper.getInstance(context).handleAction(context, sequence++, tagAliasBean);
    }

    @Override
    public void onTagOperatorResult(Context context, JPushMessage jPushMessage) {
        TagAliasOperatorHelper.getInstance(context).onTagOperatorResult(context, jPushMessage);
        super.onTagOperatorResult(context, jPushMessage);
    }

    @Override
    public void onAliasOperatorResult(Context context, JPushMessage jPushMessage) {
        TagAliasOperatorHelper.getInstance(context).onAliasOperatorResult(context, jPushMessage);
        super.onAliasOperatorResult(context, jPushMessage);
    }

}
