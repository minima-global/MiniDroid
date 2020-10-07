package com.minima.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import org.minima.Start;
import org.minima.objects.TxPoW;
import org.minima.objects.base.MiniData;
import org.minima.system.brains.ConsensusHandler;
import org.minima.system.input.InputMessage;
import org.minima.system.network.minidapps.DAPPManager;
import org.minima.system.network.rpc.RPCClient;
import org.minima.utils.MinimaLogger;
import org.minima.utils.ResponseStream;
import org.minima.utils.messages.Message;
import org.minima.utils.messages.MessageListener;

import java.io.InputStream;
import java.util.Date;

import global.org.minima.MinimaActivity;
import global.org.minima.R;
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
    Start mStart;

    //Used to update the Notification
    Handler mHandler;

    NotificationManager mNotificationManager;
    android.app.Notification mNotification;

    //Start Minima When Notification is clicked..
    PendingIntent mPendingIntent;

    //Information for the Notification
    TxPoW mTxPow;

    public static final String CHANNEL_ID = "MinimaServiceChannel";

    PowerManager.WakeLock mWakeLock;
    WifiManager.WifiLock mWifiLock;

    boolean mListenerAdded;

    boolean mFirstRun = true;

    //The Last time some action happened..
    long mLastActionTime = 0;
    boolean mStopQuitter = false;

    @Override
    public void onCreate() {
        super.onCreate();

        MinimaLogger.log("Service : onCreate");
        mListenerAdded  = false;
        mLastActionTime = System.currentTimeMillis();
        mStopQuitter    = false;
        mFirstRun       = true;

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
        mStart = new Start();
        mStart.fireStarter(getFilesDir().getAbsolutePath());

        Toast.makeText(this, "Minima Service Started", Toast.LENGTH_SHORT).show();

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
    }

    public Start getMinima(){
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
        //MinimaLogger.log("Service : OnStartCommand "+startId+" "+mListenerAdded);

        //Only do this once..
        if(!mListenerAdded){
            mListenerAdded = true;

            //Set the default message
            startForeground(1, createNotification("Syncing.."));

            Thread installer = new Thread(new Runnable() {
                @Override
                public void run() {
                    MinimaLogger.log("Service : Initialise begin..");

                    try {
                        //Wait for Minima to start..
                        while(mStart == null){ Thread.sleep(100);}
                        while(mStart.getServer() == null){Thread.sleep(100);}
                        while(mStart.getServer().getConsensusHandler() == null){Thread.sleep(100);}
                        while(mStart.getServer().getNetworkHandler() == null){Thread.sleep(100);}
                        while(mStart.getServer().getNetworkHandler().getDAPPManager() == null){Thread.sleep(100);}

                        //Are we already up and running..
                        if(mStart.getServer().getConsensusHandler().isInitialSyncComplete()){
                            MinimaLogger.log("Service : Initial Sync complete at start..");
                            installDappSuite();
                        }else{
                            MinimaLogger.log("Service : Initial Sync listen for complete..");
                        }

                        mStart.getServer().getConsensusHandler().addListener(new MessageListener() {
                            @Override
                            public void processMessage(Message zMessage) {
                                if (zMessage.isMessageType(ConsensusHandler.CONSENSUS_NOTIFY_NEWBLOCK)) {
                                    //Get the TxPoW
                                    mTxPow = (TxPoW) zMessage.getObject("txpow");

                                    //Show a notification
                                    mHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            startForeground(1, createNotification("Block "+mTxPow.getBlockNumber()+" @ "+new Date(mTxPow.getTimeMilli().getAsLong())));
                                        }
                                    });

                                }else if (zMessage.isMessageType(ConsensusHandler.CONSENSUS_NOTIFY_INITIALSYNC)) {
                                    installDappSuite();

                                }else if (zMessage.isMessageType(ConsensusHandler.CONSENSUS_NOTIFY_BALANCE)) {
                                    mHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(MinimaService.this,"Minima : Your balance has changed!",Toast.LENGTH_SHORT).show();
                                        }
                                    });

                                }else if (zMessage.isMessageType(ConsensusHandler.CONSENSUS_NOTIFY_ACTION)) {
                                    //Something happening.. don't shut down for five monutes if not on power..
                                    mLastActionTime = System.currentTimeMillis();
                                }
                            }
                        });

                        MinimaLogger.log("Service : Initialise end.. ");

                    } catch (Exception e) {
                        MinimaLogger.log("Start Service Exception "+e);
                    }
                }
            });

            installer.start();
        }

        return START_STICKY;
    }

    public void installDappSuite(){
        if(mFirstRun) {
            MinimaLogger.log("Service : Install all DAPPS..");
            mFirstRun = false;

            Thread mini_install = new Thread(new Runnable() {
                @Override
                public void run() {

                    loadMiniDapp("block.minidapp");
                    loadMiniDapp("wallet.minidapp");
                    loadMiniDapp("coinflip.minidapp");
                    loadMiniDapp("dexxed.minidapp");
                    loadMiniDapp("terminal.minidapp");
                    loadMiniDapp("scriptide.minidapp");
                    loadMiniDapp("futurecash.minidapp");

                    //And reload the lot..
                    mStart.getServer().getNetworkHandler().getDAPPManager().PostMessage(DAPPManager.DAPP_RELOAD);
                }
            });

            //Run it..
            mini_install.start();
        }else{
            MinimaLogger.log("Service : Install DAPPS - not first run..");
        }
    }

    public void loadMiniDapp(String zMiniDapp){
        try{
            InputStream is=getAssets().open(zMiniDapp);
            byte[] fileBytes=new byte[is.available()];
            is.read( fileBytes);
            is.close();

            //Post them to Minima..
            MiniData dapp = new MiniData(fileBytes);

            //The Install message
            Message msg = new Message(DAPPManager.DAPP_INSTALL);
            msg.addObject("overwrite", false);
            msg.addBoolean("reload", false);
            msg.addObject("minidapp", dapp);
            msg.addString("filename", zMiniDapp);

            mStart.getServer().getNetworkHandler().getDAPPManager().PostMessage(msg);

        }catch(Exception Exc) {
            MinimaLogger.log("ERROR install MiniDAPP : "+zMiniDapp);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        MinimaLogger.log("Service : onDestroy");
        mListenerAdded = false;

        //Post It..
        if(mStart != null && mStart.getServer()!=null && mStart.getServer().isRunning()){
            MinimaLogger.log("Service : SAVE ONDESTROY");

            //Create a response stream
            ResponseStream resp = new ResponseStream();

            //Send a backup message!
            InputMessage backup = new InputMessage("quit",resp);

            mStart.getServer().getInputHandler().PostMessage(backup);

            //Wait for it..
            resp.waitToFinish();
            MinimaLogger.log("Service : "+resp.getResponse());
        }

        //Shut the channel..
        mNotificationManager.deleteNotificationChannel(CHANNEL_ID);

        //Stop the Quitter threa..
        mStopQuitter = true;

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

