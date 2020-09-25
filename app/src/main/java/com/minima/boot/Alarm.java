package com.minima.boot;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.minima.service.ServiceStarterJobService;

public class Alarm extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent){
//        MinimaLogger.log("ALARM RECEIVED");

        //Send a start service JOB
        ServiceStarterJobService.enqueueWork(context, new Intent());
    }

    public void setAlarm(Context context){
//        MinimaLogger.log("ALARM SET");

        AlarmManager am =( AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, Alarm.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), AlarmManager.INTERVAL_FIFTEEN_MINUTES , pi); // Millisec * Second * Minute
//        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 1000 * 60 * 5 , pi); // Millisec * Second * Minute
    }

    public void cancelAlarm(Context context){
//        MinimaLogger.log("ALARM CANCELLED");

        Intent intent = new Intent(context, Alarm.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);
    }
}
