package global.org.minima;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import org.minima.Start;
import org.minima.objects.TxPOW;
import org.minima.objects.base.MiniData;
import org.minima.system.Main;
import org.minima.system.NativeListener;
import org.minima.system.brains.ConsensusHandler;
import org.minima.system.network.minidapps.DAPPManager;
import org.minima.utils.MinimaLogger;
import org.minima.utils.messages.Message;
import org.w3c.dom.Node;

import java.io.InputStream;

/** Foreground Service for the Minima Node
 *
 *  Elias Nemr
 *
 * 23 April 2020
 * */
public class NodeService extends Service {

    Handler mHandler;
    Start mStart;
    boolean mStarted = false;
    NotificationManager mNotificationManager, mNotificationManagerLow;
    android.app.Notification mNotificationBuilder;
    PendingIntent mPendingIntent;
    TxPOW txpow;
    public String mBLOCK_NUMBER;

    public static final String CHANNEL_ID = "MinimaNodeServiceChannel";

    public static NodeService mNodeService;

    PowerManager.WakeLock mWakeLock;
    WifiManager.WifiLock mWifiLock;

    @Override
    public void onCreate() {
        super.onCreate();

        mNodeService = this;

        //Power
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"Minima::MiniPower");
        if(!mWakeLock.isHeld()) {
            mWakeLock.acquire();
        }

        //WiFi..
        WifiManager wifi = (WifiManager) getSystemService(WIFI_SERVICE);
        mWifiLock = wifi.createWifiLock(WifiManager.WIFI_MODE_FULL, "Minima::MinimWiFi");
        if(!mWifiLock.isHeld()){
            mWifiLock.acquire();
        }

        //Start Minima
        mStart = new Start();
        mStart.fireStarter(getFilesDir().getAbsolutePath());

//        Log.d("Minima Call:", "" + "Minima is running");
        Toast.makeText(mNodeService, "Minima Service Started", Toast.LENGTH_LONG).show();

        mHandler = new Handler(Looper.getMainLooper());

        //And finally Install the Web Wallet as default..
        Runnable installer = new Runnable() {
            @Override
            public void run() {
                try{
                    //Wait for Minima to start..
                    Thread.sleep(1000);

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

                }catch(Exception exc){
                    Log.d("Minima","ERROR installing default Wallet.."+exc);
                }
            }
        };

        //Try it..
        Thread tt = new Thread(installer);
        tt.start();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Minima Node Foreground Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );

            mNotificationManager =
                    getSystemService(NotificationManager.class);
            mNotificationManager.createNotificationChannel(serviceChannel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Create our notification channel here & add channel to Manager's list
        createNotificationChannel();

        Intent NotificationIntent = new Intent(this, MainActivity.class);
        mPendingIntent = PendingIntent.getActivity(this, 0
                , NotificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Create our notification
        mNotificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Minima Node Status")
                .setContentText("Running...")
                .setSmallIcon(R.drawable.ic_minima)
                .setContentIntent(mPendingIntent)
                .build();

        startForeground(1, mNotificationBuilder);


        // do heavy work on background thread
        if (!mStarted) {
            mStarted = true;
            try {
                Thread.sleep(1000);
            } catch (Exception exc) {
            }
        }

        mStart.getServer().getConsensusHandler().addListener(new NativeListener() {
            @Override
            public void processMessage(final Message zMessage) {
                //THIS GETS CALLED!
                try {
                    if (zMessage.isMessageType(ConsensusHandler.CONSENSUS_NOTIFY_BALANCE)) {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {

                                mNotificationBuilder = new NotificationCompat.Builder(NodeService.this, CHANNEL_ID)
                                        .setContentTitle("Minima Node: ")
                                        .setContentText("You just received coins!")
                                        .setSmallIcon(R.drawable.ic_minima)
                                        .setContentIntent(mPendingIntent)
                                        .build();

                                startForeground(1, mNotificationBuilder);

                                Toast.makeText(NodeService.this,
                                        "You just received some coins!",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else if (zMessage.isMessageType(ConsensusHandler.CONSENSUS_NOTIFY_NEWBLOCK)) {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {

                                TxPOW txpow = (TxPOW) zMessage.getObject("txpow");

                                mBLOCK_NUMBER = txpow.getBlockNumber().toString();

                                mNotificationBuilder = new NotificationCompat.Builder(NodeService.this, CHANNEL_ID)
                                        .setContentTitle("Block "+mBLOCK_NUMBER)
                                        .setContentText("Minima Node Channel")
                                        .setSmallIcon(R.drawable.ic_minima)
                                        .setContentIntent(mPendingIntent)
                                        .build();

                                startForeground(1, mNotificationBuilder);

                            }
                        });

                    }
                } catch (Exception exc) {

                }
            }
        });

        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        Toast.makeText(this, "Minima stopped running.", Toast.LENGTH_LONG).show();
        super.onDestroy();

        mWakeLock.release();
        mWifiLock.release();
    }


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }


}

