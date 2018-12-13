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

package com.mokee.center.receiver;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import androidx.core.app.NotificationCompat;
import android.util.Log;

import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.model.Response;
import com.mokee.center.R;
import com.mokee.center.activity.MainActivity;
import com.mokee.center.misc.State;
import com.mokee.center.model.UpdateInfo;
import com.mokee.center.util.CommonUtil;
import com.mokee.center.util.FileUtil;
import com.mokee.center.util.RequestUtil;

import org.json.JSONException;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.UUID;

import com.mokee.center.misc.Constants;

public class UpdatesCheckReceiver extends BroadcastReceiver {

    private static final String TAG = UpdatesCheckReceiver.class.getName();

    private static final String DAILY_CHECK_ACTION = "daily_check_action";
    private static final String ONESHOT_CHECK_ACTION = "oneshot_check_action";

    private static final String NEW_UPDATES_NOTIFICATION_CHANNEL = "new_updates_notification_channel";

    // max. number of updates listed in the extras notification
    private static final int EXTRAS_NOTIF_UPDATE_COUNT = 4;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            //cleanup downloads dir
        }

        final SharedPreferences mMainPrefs = CommonUtil.getMainPrefs(context);

        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // Set a repeating alarm on boot to check for new updates once per day
            scheduleRepeatingUpdatesCheck(context);
        }

        if (!CommonUtil.isNetworkAvailable(context)) {
            Log.d(TAG, "Network not available, scheduling new check");
            scheduleUpdatesCheck(context);
            return;
        }

        final File json = FileUtil.getCachedUpdateList(context);
        final File jsonNew = new File(context.getCacheDir().getAbsolutePath() + UUID.randomUUID());
        RequestUtil.fetchAvailableUpdates(context, new StringCallback() {
            @Override
            public void onSuccess(Response<String> response) {
                try {
                    final LinkedList<UpdateInfo> updates = CommonUtil.parseJson(context, response.body(), TAG);
                    State.saveState(updates, jsonNew);
                    if (json.exists() && CommonUtil.checkForNewUpdates(json, jsonNew)) {
                        showNotification(context, updates);
                        updateRepeatingUpdatesCheck(context);
                    }
                    jsonNew.renameTo(json);
                    // In case we set a one-shot check because of a previous failure
                    cancelUpdatesCheck(context);
                } catch (JSONException e) {
                    Log.e(TAG, "Could not parse list, scheduling new check", e);
                    scheduleUpdatesCheck(context);
                }
                mMainPrefs.edit().putLong(Constants.PREF_LAST_UPDATE_CHECK, System.currentTimeMillis()).apply();
            }

            @Override
            public void onError(Response<String> response) {
                super.onError(response);
                Log.e(TAG, "Could not download updates list, scheduling new check");
                scheduleUpdatesCheck(context);
            }
        });
    }

    private static void showNotification(Context context, LinkedList<UpdateInfo> updates) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel notificationChannel = new NotificationChannel(
                NEW_UPDATES_NOTIFICATION_CHANNEL,
                context.getString(R.string.new_updates_channel_title),
                NotificationManager.IMPORTANCE_LOW);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context,
                NEW_UPDATES_NOTIFICATION_CHANNEL);

        Intent notificationIntent = new Intent(context, MainActivity.class);
        PendingIntent intent = PendingIntent.getActivity(context, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        String contentText = context.getResources().getQuantityString(
                R.plurals.new_updates_found_content, updates.size(), updates.size());

        notificationBuilder.setSmallIcon(R.drawable.ic_system_update);
        notificationBuilder.setContentIntent(intent);
        notificationBuilder.setContentTitle(context.getString(R.string.new_updates_found_title));
        notificationBuilder.setContentText(contentText);
        notificationBuilder.setAutoCancel(true);

        NotificationCompat.InboxStyle inbox = new NotificationCompat.InboxStyle(notificationBuilder).setBigContentTitle(contentText);
        int added = 0, count = updates.size();
        for (UpdateInfo update : updates) {
            if (added < EXTRAS_NOTIF_UPDATE_COUNT) {
                inbox.addLine(update.getDisplayVersion());
                added ++;
            }
        }
        if (added != count) {
            inbox.setSummaryText(context.getResources().getQuantityString(
                    R.plurals.new_updates_found_additional_count, count - added, count - added));
        }
        notificationBuilder.setStyle(inbox);
        notificationBuilder.setNumber(updates.size());

        if (count == 1) {
            // Add action here
        }

        notificationManager.createNotificationChannel(notificationChannel);
        notificationManager.notify(0, notificationBuilder.build());
    }

    private static PendingIntent getRepeatingUpdatesCheckIntent(Context context) {
        Intent intent = new Intent(context, UpdatesCheckReceiver.class);
        intent.setAction(DAILY_CHECK_ACTION);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    public static void updateRepeatingUpdatesCheck(Context context) {
        cancelRepeatingUpdatesCheck(context);
        scheduleRepeatingUpdatesCheck(context);
    }

    public static void scheduleRepeatingUpdatesCheck(Context context) {
        long millisToNextRelease = millisToNextRelease(context);
        PendingIntent updateCheckIntent = getRepeatingUpdatesCheckIntent(context);
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + millisToNextRelease,
                AlarmManager.INTERVAL_DAY, updateCheckIntent);

        Date nextCheckDate = new Date(System.currentTimeMillis() + millisToNextRelease);
        Log.d(TAG, "Setting daily updates check: " + nextCheckDate);
    }

    public static void cancelRepeatingUpdatesCheck(Context context) {
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmMgr.cancel(getRepeatingUpdatesCheckIntent(context));
    }

    private static PendingIntent getUpdatesCheckIntent(Context context) {
        Intent intent = new Intent(context, UpdatesCheckReceiver.class);
        intent.setAction(ONESHOT_CHECK_ACTION);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    public static void scheduleUpdatesCheck(Context context) {
        long millisToNextCheck = AlarmManager.INTERVAL_HOUR * 2;
        PendingIntent updateCheckIntent = getUpdatesCheckIntent(context);
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmMgr.set(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + millisToNextCheck,
                updateCheckIntent);

        Date nextCheckDate = new Date(System.currentTimeMillis() + millisToNextCheck);
        Log.d(TAG, "Setting one-shot updates check: " + nextCheckDate);
    }

    public static void cancelUpdatesCheck(Context context) {
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmMgr.cancel(getUpdatesCheckIntent(context));
        Log.d(TAG, "Cancelling pending one-shot check");
    }

    private static long millisToNextRelease(Context context) {
        final long extraMillis = 3 * AlarmManager.INTERVAL_HOUR;

        LinkedList<UpdateInfo> updates = State.loadState(FileUtil.getCachedUpdateList(context));

        if (updates == null || updates.size() == 0) {
            return SystemClock.elapsedRealtime() + AlarmManager.INTERVAL_DAY;
        }

        long buildTimestamp = 0;
        for (UpdateInfo update : updates) {
            if (update.getTimestamp() > buildTimestamp) {
                buildTimestamp = update.getTimestamp();
            }
        }
        buildTimestamp *= 1000;

        Calendar c = Calendar.getInstance();
        long now = c.getTimeInMillis();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        c.setTimeInMillis(c.getTimeInMillis() + millisSinceMidnight(buildTimestamp));
        long millisToNextRelease = (c.getTimeInMillis() - now);
        millisToNextRelease += extraMillis;
        if (c.getTimeInMillis() < now) {
            millisToNextRelease += AlarmManager.INTERVAL_DAY;
        }

        return millisToNextRelease;
    }

    private static long millisSinceMidnight(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return millis - c.getTimeInMillis();
    }
}
