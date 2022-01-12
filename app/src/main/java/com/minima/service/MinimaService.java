package com.minima.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import org.minima.Minima;
import org.minima.objects.TxPoW;
import org.minima.system.Main;
import org.minima.system.network.webhooks.NotifyManager;
import org.minima.utils.MinimaLogger;
import org.minima.utils.json.JSONObject;
import org.minima.utils.messages.Message;
import org.minima.utils.messages.MessageListener;

import java.util.ArrayList;
import java.util.Date;

import global.org.minima.MinimaActivity;
import global.org.minima.R;

import com.jraska.console.Console;
import com.minima.boot.Alarm;

/** Foreground Service for the Minima Node
 *
 *  Elias Nemr
 *
 * 23 April 2020
 * */
public class MinimaService extends Service {

    //Currently Binding doesn't work as we run in a separate process..
    public class MyBinder extends Binder {
        public MinimaService getService() {
            return mService;
        }
    }
    private IBinder mBinder = new MyBinder();
    MinimaService mService;

    Alarm mAlarm;

    //Minima Main Starter
    Minima mStart;

    //Used to update the Notification
    Handler mHandler;

    NotificationManager mNotificationManager;
    android.app.Notification mNotification;

    //Start Minima When Notification is clicked..
    PendingIntent mPendingIntent;

    //Information for the Notification
    JSONObject mTxPowJSON = null;

    public static final String CHANNEL_ID = "MinimaServiceChannel";

    PowerManager.WakeLock mWakeLock;
    WifiManager.WifiLock mWifiLock;

    @Override
    public void onCreate() {
        super.onCreate();

        //Power
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"Minima::MiniPower");
        if(!mWakeLock.isHeld()) {
            mWakeLock.acquire();
        }

        //WiFi..
        WifiManager wifi = (WifiManager) getSystemService(WIFI_SERVICE);
        mWifiLock = wifi.createWifiLock(WifiManager.WIFI_MODE_FULL, "Minima::MiniWiFi");
        if(!mWifiLock.isHeld()){
            mWifiLock.acquire();
        }

        //Start Minima
        mStart = new Minima();

        mHandler = new Handler(Looper.getMainLooper());

        // Create our notification channel here & add channel to Manager's list
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "Minima Node Foreground Service Channel",
                NotificationManager.IMPORTANCE_LOW
        );

        mNotificationManager = getSystemService(NotificationManager.class);
        mNotificationManager.createNotificationChannel(serviceChannel);

        Intent NotificationIntent = new Intent(getBaseContext(), MinimaActivity.class);
        mPendingIntent = PendingIntent.getActivity(getBaseContext(), 0
                , NotificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        //Set the Alarm..
        mAlarm = new Alarm();
        mAlarm.cancelAlarm(this);
        mAlarm.setAlarm(this);

        mService = this;

        //Add a Minima listener..
        Main.setMinimaListener(new MessageListener() {
            @Override
            public void processMessage(Message zMessage) {
                if(zMessage.getMessageType().equals(MinimaLogger.MINIMA_LOG)){
                    Console.writeLine(zMessage.getString("log"));

                }else if(zMessage.getMessageType().equals(NotifyManager.NOTIFY_POST)){
                    //Get the JSON..
                    JSONObject notify = (JSONObject) zMessage.getObject("notify");

                    //What is the Event..
                    String event    = (String) notify.get("event");
                    JSONObject data = (JSONObject) notify.get("data");

                    if(event.equals("NEWBLOCK")) {

                        //Get the TxPoW
                        mTxPowJSON = (JSONObject) data.get("txpow");

                        //Show a notification
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                //Set status Bar notification
                                setMinimaNotification();
                            }
                        });

                    }else if(event.equals("MAXIMA")){

                        //How big is the message
                        String datastr  = data.toString();
                        int datalen     = datastr.length();
                        int maxsize     = 50000;

                        if(datalen < maxsize) {

                            //Broadcast the Maxima Message ( Currently ONLY MaxChat / MaxWeb gets it.. can be configured )
                            Intent mchat= new Intent();
                            ComponentName cn = new ComponentName("com.minima.maxchat","com.minima.maxchat.MyBroadcastReceiver");
                            mchat.setComponent(cn);
                            mchat.setAction("com.minima.maxchat.MINIMA_MESSAGE");
                            mchat.putExtra("data", datastr);
                            sendBroadcast(mchat);

                            Intent i2 = new Intent();
                            ComponentName cn2 = new ComponentName("com.minima.webtest", "com.minima.webtest.MyBroadcastReceiver");
                            i2.setComponent(cn2);
                            i2.setAction("com.minima.webtest.MINIMA_MESSAGE");
                            i2.putExtra("data", datastr);
                            sendBroadcast(i2);

                        }else{
                            //Use a multi broadcast - otherwise Android plays up..:(
                            int numdivs = (int)Math.ceil ((double)datalen / (double)maxsize);
                            MinimaLogger.log("Multi Broadcast large message "+datalen+" "+numdivs);

                            for(int i=0;i<numdivs;i++){
                                Intent i2 = new Intent();
                                ComponentName cn2 = new ComponentName("com.minima.webtest", "com.minima.webtest.MyBroadcastReceiver");
                                i2.setComponent(cn2);
                                i2.setAction("com.minima.webtest.MINIMA_MESSAGE");
                                i2.putExtra("multi", true);
                                i2.putExtra("multitotal", numdivs);
                                i2.putExtra("multinum", i);

                                int start   = i*maxsize;
                                int end     = start + maxsize;
                                if(end>datalen){
                                    end = datalen;
                                }
                                String datadiv = datastr.substring(start,end);
                                i2.putExtra("data", datadiv);

                                sendBroadcast(i2);
                            }
                        }
                    }
                }
            }
        });

        //Start her up..
        ArrayList<String> vars = new ArrayList<>();

        vars.add("-daemon");

        vars.add("-data");
        vars.add(getFilesDir().getAbsolutePath());

        //Normal
        vars.add("-isclient");
        vars.add("-mobile");

        vars.add("-rpcenable");

////        //Test Values
//        vars.add("-test");
//        vars.add("-clean");
//        vars.add("-nop2p");

//        vars.add("-connect");
//        vars.add("10.0.2.2:9001");

//        vars.add("-genesis");
//        vars.add("-automine");

        mStart.mainStarter(vars.toArray(new String[0]));

        //Notify User service is now running!
        Toast.makeText(this, "Minima Service Started", Toast.LENGTH_SHORT).show();
    }

    public Minima getMinima(){
        return mStart;
    }

    public Notification createNotification(String zText){
        mNotification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(zText)
                .setContentText("Minima Status Channel")
                .setSmallIcon(R.drawable.ic_minima)
                .setContentIntent(mPendingIntent)
                .build();

        return mNotification;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        //Set status Bar notification
        setMinimaNotification();

        return START_STICKY;
    }

    private void setMinimaNotification(){
        //Set the default message
        if(mTxPowJSON == null){
            startForeground(1, createNotification("Starting up.. please wait.."));

        }else{
            JSONObject header = (JSONObject)mTxPowJSON.get("header");

            String block    = (String) header.get("block");
            String date     = (String) header.get("date");

            startForeground(1, createNotification("Block " + block + " @ " + date));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        //QUIT nicely..
        mStart.runMinimaCMD("quit");

        //Not listening anymore..
        Main.setMinimaListener(null);

        //Shut the channel..
        mNotificationManager.deleteNotificationChannel(CHANNEL_ID);

        //Mention..
        Toast.makeText(this, "Minima Service Stopped", Toast.LENGTH_SHORT).show();

        //Release the wakelocks..
        mWakeLock.release();
        mWifiLock.release();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public static boolean isPlugged(Context context) {
        boolean isPlugged= false;
        Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        isPlugged = plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB || plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS;

        return isPlugged;
    }

}

