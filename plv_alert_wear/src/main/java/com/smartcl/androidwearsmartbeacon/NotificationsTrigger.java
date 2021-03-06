package com.smartcl.androidwearsmartbeacon;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.smartcl.communicationlibrary.LCLAppIntentService;
import com.smartcl.communicationlibrary.LCLPreferences;

/**
 * Trigger a notification on the wearable device.
 * This class displays the notification as well as different pages (questions)
 * and several actions.
 */
public class NotificationsTrigger {

    public static void TriggerNotification(Context packageContext, String question) {
        Notification questionNotif = buildQuestionNotification(packageContext, question);

        Intent appIntent = new Intent(packageContext, LCLAppIntentService.class);
        PendingIntent appPendingIntent = PendingIntent
                .getService(packageContext, 0, appIntent, 0);

        Notification notification =
                new NotificationCompat.Builder(packageContext)
                        .setSmallIcon(R.drawable.icon_ad)
                        .setContentTitle(packageContext.getString(R.string.title_activity_notif))
                        .setContentText(String.format(
                                packageContext.getString(R.string.content_activity_notif),
                                LCLPreferences.GetNameUser(packageContext)))
                        .addAction(R.drawable.icon_lcl,
                                   packageContext.getString(R.string.open_app),
                                   appPendingIntent)
                        .extend(new NotificationCompat.WearableExtender().addPage(questionNotif))
                        .setVibrate(new long[]{500, 500})
                        .build();


        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(packageContext);

        notificationManager.notify(001, notification);
    }

    private static Notification buildQuestionNotification(Context packageContext, String question) {
        Intent questionIntent = new Intent(packageContext, NotificationActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("question", question);
        questionIntent.putExtras(bundle);

        PendingIntent questionPendingIntent =
                PendingIntent.getActivity(packageContext, 0, questionIntent,
                                          PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notificationQuestion =
                new NotificationCompat.Builder(packageContext)
                        .setSmallIcon(R.drawable.ic_full_sad)
                        .extend(new NotificationCompat.WearableExtender()
                                        .setDisplayIntent(questionPendingIntent)
                                        .setCustomSizePreset(
                                                NotificationCompat.WearableExtender.SIZE_LARGE))
                        .build();
        return notificationQuestion;
    }

}
