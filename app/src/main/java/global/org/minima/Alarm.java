package global.org.minima;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import org.minima.utils.MinimaLogger;

public class Alarm extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent){
        //Try and start thew service...
        Intent serviceintent = new Intent(context, MinimaService.class);
        context.startForegroundService(serviceintent);
    }

    public void setAlarm(Context context){
        AlarmManager am =( AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, Alarm.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), AlarmManager.INTERVAL_FIFTEEN_MINUTES , pi); // Millisec * Second * Minute
    }

    public void cancelAlarm(Context context){
        Intent intent = new Intent(context, Alarm.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);
    }
}
