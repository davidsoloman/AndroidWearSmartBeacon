package com.smartcl.accountAlert;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.smartcl.communicationlibrary.LCLAppIntentService;
import com.smartcl.communicationlibrary.LCLPreferences;

import org.json.simple.JSONObject;

/**
 * Trigger a notification on the wearable device.
 * This class displays the notification as well as different pages (questions)
 * and several actions.
 */
public class NotificationsTrigger {

    public static void TriggerNotification(Context packageContext, JSONObject accountsJson) {
        Notification accountNotif = buildAccountNotification(packageContext, accountsJson);
        PendingIntent accountHistoryIntent = buildAccountPrevisionIntent(packageContext,
                                                                         accountsJson);

        Intent appIntent = new Intent(packageContext, LCLAppIntentService.class);
        PendingIntent appPendingIntent = PendingIntent
                .getService(packageContext, 0, appIntent, 0);

        Notification notification =
                new NotificationCompat.Builder(packageContext)
                        .setSmallIcon(R.drawable.icon_bank)
                        .setContentTitle(packageContext.getString(R.string.account_notif_title))
                        .setContentText(String.format(
                                packageContext.getString(R.string.account_notif_content),
                                LCLPreferences.GetNameUser(packageContext)))
                        .extend(new NotificationCompat.WearableExtender().addPage(accountNotif))
                        .addAction(R.drawable.icon_bank,
                                   packageContext.getString(R.string.account_previsions),
                                   accountHistoryIntent)
                        .addAction(R.drawable.icon_lcl,
                                   packageContext.getString(R.string.open_app),
                                   appPendingIntent)
                        .setVibrate(new long[]{500, 500})
                        .build();

        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(packageContext);

        notificationManager.notify(001, notification);
    }

    private static Notification buildAccountNotification(Context packageContext,
                                                         JSONObject accountsJson) {
        Intent accountIntent = new Intent(packageContext, AccountStatesOverviewActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("account_data", accountsJson.toJSONString());
        accountIntent.putExtras(bundle);

        PendingIntent accountPendingIntent =
                PendingIntent.getActivity(packageContext, 0, accountIntent,
                                          PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notificationAccounts =
                new NotificationCompat.Builder(packageContext)
                        .setSmallIcon(R.drawable.ic_full_sad)
                        .extend(new NotificationCompat.WearableExtender()
                                        .setDisplayIntent(accountPendingIntent)
                                        .setCustomSizePreset(
                                                NotificationCompat.WearableExtender.SIZE_FULL_SCREEN))
                        .build();
        return notificationAccounts;
    }

    private static PendingIntent buildAccountPrevisionIntent(Context packageContext,
                                                             JSONObject accountsJson) {
        Intent accountIntent = new Intent(packageContext, AccountStatesDetailedActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("account_data", accountsJson.toJSONString());
        accountIntent.putExtras(bundle);

        PendingIntent accountPendingIntent =
                PendingIntent.getActivity(packageContext, 0, accountIntent,
                                          PendingIntent.FLAG_UPDATE_CURRENT);
        return accountPendingIntent;
    }

}
