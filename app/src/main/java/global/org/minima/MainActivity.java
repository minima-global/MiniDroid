package global.org.minima;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.minima.system.NativeListener;
import org.minima.system.brains.ConsensusHandler;
import org.minima.utils.MinimaLogger;
import org.minima.utils.messages.Message;

import java.io.File;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.sql.SQLException;
import java.util.Enumeration;

public class MainActivity extends AppCompatActivity implements ServiceConnection {

    //The Help Button
    Button btnMini;

    //The IP Text..
    TextView mTextIP;

    //The IP of this device
    String mIP;

    //The Service..
    NodeService mMinima;

    boolean mSynced = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        //The Button to open the local browser
        btnMini = findViewById(R.id.btn_minidapp);

        // btn open 127.0.0.1:21000 `Minidapp`
        btnMini.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://127.0.0.1:21000/"));
                startActivity(intent);
            }
        });

        btnMini.setVisibility(View.INVISIBLE);

        //The TEXT that shows the current IP
        mTextIP = findViewById(R.id.iptext_minidapp);
        mTextIP.setText("Synchronising network.. Please wait.. ");

        //start Minima node Foreground Service
        Intent intent = new Intent(this, NodeService.class);
        startForegroundService(intent);

        Runnable servbind = new Runnable() {
            @Override
            public void run() {
                //Wait 5 seconds..
                try {Thread.sleep(2000);} catch (InterruptedException e) {}

                //Bind to the service
                bindService(new Intent(MainActivity.this, NodeService.class), MainActivity.this, Context.BIND_AUTO_CREATE);
            }
        };

        Thread binder = new Thread(servbind);
        binder.start();
    }

    @Override
    protected void onStart() {
        super.onStart();
        setIPText();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setIPText();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(this);
    }

    public void setIPText() {
        //Set the IP - it may change..
        if (mSynced) {
            mIP = getIP();
            mTextIP.setText("\nConnect to Minima from your Desktop\n\nOpen a browser and go to\n\nhttp://" + mIP + ":21000/");
        }
    }

    public String getIP(){
        String mHost = "127.0.0.1";
        boolean found = false;
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (!found && interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                // filters out 127.0.0.1 and inactive interfaces
                if (iface.isLoopback() || !iface.isUp())
                    continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while(!found && addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    String ip   = addr.getHostAddress();
                    String name = iface.getDisplayName();

                    //Only get the IPv4
                    if(!ip.contains(":")) {
                        mHost = ip;
                        if(name.startsWith("wl")) {
                            found = true;
                            break;
                        }
                    }
                }
            }
        } catch (SocketException e) {
            System.out.println("Minima Network IP : "+e);
        }

        return mHost;
    }

    public void setPostSyncDetails(boolean zShowToast){
        Runnable uiupdate = new Runnable() {
            @Override
            public void run() {
                mSynced = true;
                btnMini.setVisibility(View.VISIBLE);
                setIPText();
            }
        };

        //Update..
        runOnUiThread(uiupdate);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        NodeService.MyBinder binder = (NodeService.MyBinder) service;
        mMinima = binder.getService();

        MinimaLogger.log("BIND: Service Connected");

        Runnable notify =new Runnable() {
            @Override
            public void run() {
                //Check..
                if(mMinima.mStart.getServer().getConsensusHandler().isInitialSyncComplete()){
                    setPostSyncDetails(false);
                    return;
                }

                //Remove previous listeners if there are any..
                mMinima.mStart.getServer().getConsensusHandler().clearListeners();

                //Set a listener..
                mMinima.mStart.getServer().getConsensusHandler().addListener(new NativeListener() {
                    @Override
                    public void processMessage(final Message zMessage) {
                        MinimaLogger.log("Notify Message : "+zMessage);
                        try {
                            if (zMessage.isMessageType(ConsensusHandler.CONSENSUS_NOTIFY_INITIALSYNC)) {
                                setPostSyncDetails(true);
                            }
                        } catch (Exception exc) {}
                    }
                });
            }
        };

        Thread tt = new Thread(notify);
        tt.start();

    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }
        @Override
    public void onServiceDisconnected(ComponentName name) {
        mMinima = null;
    }
}
