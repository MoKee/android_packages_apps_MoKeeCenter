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
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.SparseArray;

import com.mokee.center.model.TagAliasBean;
import com.mokee.center.util.CommonUtil;
import com.mokee.center.util.Logger;
import com.mokee.os.Build;
import com.mokee.os.Build.VERSION;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import cn.jpush.android.api.JPushInterface;
import cn.jpush.android.api.JPushMessage;

public class TagAliasOperatorHelper {

    static final int ACTION_GET = 2;
    private static final String TAG = TagAliasOperatorHelper.class.getSimpleName();
    private static final int ACTION_SET = 1;
    static int sequence = 0;

    private static TagAliasOperatorHelper mInstance;

    private static DelaySendHandler mDelaySendHandler;
    private SparseArray<Object> setActionCache = new SparseArray<>();

    private TagAliasOperatorHelper(Context context) {
        mDelaySendHandler = new DelaySendHandler(context);
    }

    public static synchronized TagAliasOperatorHelper getInstance(Context context) {
        if (mInstance == null) {
            synchronized (TagAliasOperatorHelper.class) {
                if (mInstance == null) {
                    mInstance = new TagAliasOperatorHelper(context);
                }
            }
        }
        return mInstance;
    }

    private void put(int sequence, Object tagAliasBean) {
        setActionCache.put(sequence, tagAliasBean);
    }

    void handleAction(Context context, int sequence, TagAliasBean tagAliasBean) {
        if (tagAliasBean == null) {
            Logger.w(TAG, "tagAliasBean was null");
            return;
        }
        put(sequence, tagAliasBean);
        if (tagAliasBean.isAliasAction()) {
            switch (tagAliasBean.getAction()) {
                case ACTION_GET:
                    JPushInterface.getAlias(context, sequence);
                    break;
                case ACTION_SET:
                    JPushInterface.setAlias(context, sequence, tagAliasBean.getAlias());
                    break;
                default:
                    Logger.w(TAG, "un-support alias action type");
            }
        } else {
            switch (tagAliasBean.getAction()) {
                case ACTION_SET:
                    JPushInterface.setTags(context, sequence, tagAliasBean.getTags());
                    break;
                case ACTION_GET:
                    JPushInterface.getAllTags(context, sequence);
                    break;
                default:
                    Logger.w(TAG, "un-support tag action type");
            }
        }
    }

    private void RetryActionIfNeeded(Context context, int errorCode, TagAliasBean tagAliasBean) {
        if (!CommonUtil.isNetworkAvailable(context)) {
            Logger.w(TAG, "no network");
        }
        //返回的错误码为6002 超时,6014 服务器繁忙,都建议延迟重试
        if (errorCode == 6002 || errorCode == 6014) {
            Logger.d(TAG, "need retry");
            if (tagAliasBean != null) {
                Message message = new Message();
                message.obj = tagAliasBean;
                mDelaySendHandler.sendMessageDelayed(message, 1000 * 60);
                String logs = getRetryStr(tagAliasBean.isAliasAction(), tagAliasBean.getAction(), errorCode);
                Logger.i(TAG, logs);
            }
        }
    }

    private String getRetryStr(boolean isAliasAction, int actionType, int errorCode) {
        String str = "Failed to %s %s due to %s. Try again after 60s.";
        str = String.format(Locale.ENGLISH, str, getActionStr(actionType), (isAliasAction ? "alias" : " tags"), (errorCode == 6002 ? "timeout" : "server too busy"));
        return str;
    }

    private String getActionStr(int actionType) {
        switch (actionType) {
            case ACTION_SET:
                return "set";
            case ACTION_GET:
                return "get";
        }
        return "unknown operation";
    }

    void onTagOperatorResult(Context context, JPushMessage jPushMessage) {
        int sequence = jPushMessage.getSequence();
        Logger.i(TAG, "action - onTagOperatorResult, sequence: " + sequence + ", tags: " + jPushMessage.getTags());
        //根据sequence从之前操作缓存中获取缓存记录
        TagAliasBean tagAliasBean = (TagAliasBean) setActionCache.get(sequence);
        if (tagAliasBean == null) {
            Logger.e(TAG, "获取缓存记录失败");
            return;
        }
        if (jPushMessage.getErrorCode() == 0) {
            setActionCache.remove(sequence);
            String logs = getActionStr(tagAliasBean.getAction()) + " tags success";
            if (jPushMessage.getTags().size() == 0) {
                logs = "tags is empty";
            }
            Logger.i(TAG, logs);
            Set<String> tags = new HashSet<>();
            tags.add(Build.PRODUCT);
            tags.add(VERSION.CODENAME);
            tags.add(Build.MAINTAINER);
            tagAliasBean.setTags(tags);
            if (!tagAliasBean.getTags().equals(jPushMessage.getTags())) {
                tagAliasBean.setAction(ACTION_SET);
                sequence++;
                handleAction(context, sequence, tagAliasBean);
            }
        } else {
            String logs = "Failed to " + getActionStr(tagAliasBean.getAction()) + " tags";
            if (jPushMessage.getErrorCode() == 6018) {
                //tag数量超过限制,需要先清除一部分再add
                logs += ", tags is exceed limit need to clean";
            }
            logs += ", errorCode:" + jPushMessage.getErrorCode();
            Logger.e(TAG, logs);
            RetryActionIfNeeded(context, jPushMessage.getErrorCode(), tagAliasBean);
        }
    }

    void onAliasOperatorResult(Context context, JPushMessage jPushMessage) {
        Logger.i(TAG, "action - onAliasOperatorResult, sequence: " + sequence + ", alias: " + jPushMessage.getAlias());
        int sequence = jPushMessage.getSequence();
        //根据sequence从之前操作缓存中获取缓存记录
        TagAliasBean tagAliasBean = (TagAliasBean) setActionCache.get(sequence);
        if (tagAliasBean == null) {
            Logger.e(TAG, "获取缓存记录失败");
            return;
        }
        if (jPushMessage.getErrorCode() == 0) {
            setActionCache.remove(sequence);
            String logs = getActionStr(tagAliasBean.getAction()) + " alias success";
            if (TextUtils.isEmpty(jPushMessage.getAlias())) {
                logs = "alias is empty";
            }
            Logger.i(TAG, logs);
            tagAliasBean.setAlias(Build.getUniqueID(context));
            if (!TextUtils.equals(jPushMessage.getAlias(), tagAliasBean.getAlias())) {
                tagAliasBean.setAction(ACTION_SET);
                sequence++;
                handleAction(context, sequence, tagAliasBean);
            }
        } else {
            String logs = "Failed to " + getActionStr(tagAliasBean.getAction()) + " alias, errorCode:" + jPushMessage.getErrorCode();
            Logger.e(TAG, logs);
            RetryActionIfNeeded(context, jPushMessage.getErrorCode(), tagAliasBean);
        }
    }

    static class DelaySendHandler extends Handler {
        WeakReference<Context> mContext;

        DelaySendHandler(Context context) {
            mContext = new WeakReference<>(context);
        }

        @Override
        public void handleMessage(Message msg) {
            Context context = mContext.get();
            if (msg.obj instanceof TagAliasBean) {
                Logger.i(TAG, "on delay time");
                sequence++;
                TagAliasBean tagAliasBean = (TagAliasBean) msg.obj;
                mInstance.setActionCache.put(sequence, tagAliasBean);
                if (context != null) {
                    mInstance.handleAction(context, sequence, tagAliasBean);
                } else {
                    Logger.e(TAG, "#unexcepted - context was null");
                }
            } else {
                Logger.w(TAG, "#unexcepted - msg obj was incorrect");
            }
        }
    }
}
