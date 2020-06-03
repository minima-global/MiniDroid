package global.org.minima;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Handler;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import org.minima.Start;
import org.minima.objects.TxPoW;
import org.minima.objects.base.MiniData;
import org.minima.system.NativeListener;
import org.minima.system.brains.ConsensusHandler;
import org.minima.system.input.InputMessage;
import org.minima.system.network.minidapps.DAPPManager;
import org.minima.utils.MinimaLogger;
import org.minima.utils.ResponseStream;
import org.minima.utils.messages.Message;

import java.io.InputStream;
import java.util.Date;

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

    @Override
    public void onCreate() {
        super.onCreate();

        MinimaLogger.log("Service : onCreate");

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
        //Cancel an old alarm..
        mAlarm.cancelAlarm(this);
        //Start a new one..
        mAlarm.setAlarm(this);

        mService = this;

        mListenerAdded = false;
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
        MinimaLogger.log("Service : OnStartCommand "+startId);

        //Only do this once..
        if(!mListenerAdded){
            mListenerAdded = true;

            //Set the default message
            startForeground(1, createNotification("Syncing.."));

            try {
                //Wait for Minima to start..
                while(mStart == null){ Thread.sleep(500);}
                while(mStart.getServer() == null){Thread.sleep(500);}
                while(mStart.getServer().getNetworkHandler() == null){Thread.sleep(500);}
                while(mStart.getServer().getNetworkHandler().getDAPPManager() == null){Thread.sleep(500);}

                Message msg = new Message(DAPPManager.DAPP_INSTALL);
                msg.addObject("overwrite", false);

                InputStream is=getAssets().open("wallet.minidapp");
                byte[] fileBytes=new byte[is.available()];
                is.read( fileBytes);
                is.close();

                //Post them to Minima..
                MiniData dapp = new MiniData(fileBytes);
                msg.addObject("minidapp", dapp);
                mStart.getServer().getNetworkHandler().getDAPPManager().PostMessage(msg);

                mStart.getServer().getConsensusHandler().addListener(new NativeListener() {
                    @Override
                    public void processMessage(Message zMessage) {
                        if (zMessage.isMessageType(ConsensusHandler.CONSENSUS_NOTIFY_NEWBLOCK)) {
                            //Gewt the TxPoW
                            mTxPow = (TxPoW) zMessage.getObject("txpow");

                            //Show a notification
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    startForeground(1, createNotification("Block "+mTxPow.getBlockNumber()+" @ "+new Date(mTxPow.getTimeMilli().getAsLong())));
                                }
                            });

                        }else if (zMessage.isMessageType(ConsensusHandler.CONSENSUS_NOTIFY_BALANCE)) {
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(MinimaService.this,"Minima : Your balance has changed!",Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                });

            } catch (Exception e) {
                MinimaLogger.log("Start Service Exception "+e);
            }
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        MinimaLogger.log("Service : onDestroy");

        //Post It..
        if(mStart != null){
            //Create a response stream
            ResponseStream resp = new ResponseStream();

            //Send a backup message!
            InputMessage backup = new InputMessage("backup",resp);

            MinimaLogger.log("Service : POST BACKUP MESSAGE");
            mStart.getServer().getInputHandler().PostMessage(backup);

            //Wait for it..
            resp.waitToFinish();
            MinimaLogger.log("Service : WAIT FINISHED");
        }

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

}

