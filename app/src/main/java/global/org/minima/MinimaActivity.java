package global.org.minima;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.minima.utils.MinimaLogger;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class MinimaActivity extends AppCompatActivity {

    //The Help Button
    Button btnMini;

    //The IP Text..
    TextView mTextIP;

    //The IP of this device
    String mIP;

    //The Service..
    MinimaService mMinima;

    boolean mSynced = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MinimaLogger.log("Activity : onCreate");

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
        mTextIP.setText("Synchronising network.. \n\nPlease wait.. 10 seconds");

        //start Minima node Foreground Service
        Intent intent = new Intent(getBaseContext(), MinimaService.class);
        startForegroundService(intent);

        //Start thread and wait 10 seconds..
        Thread updater = new Thread(new Runnable() {
            @Override
            public void run() {
                try {Thread.sleep(10000);} catch (InterruptedException e) {}

                setPostSyncDetails();
            }
        });
        updater.start();
    }

    @Override
    protected void onStart() {
        super.onStart();
        MinimaLogger.log("Activity : onStart");
    }

    @Override
    protected void onStop() {
        super.onStop();
        MinimaLogger.log("Activity : onStop");
    }

    @Override
    protected void onResume() {
        super.onResume();
        MinimaLogger.log("Activity : onResume");

        setIPText();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MinimaLogger.log("Activity : onDestroy");
    }

    public void setIPText() {
        //Set the IP - it may change..
        if (mSynced) {
            mIP = getIP();
            mTextIP.setText("\nBrowse locally or connect to Minima from your Desktop\n\nOpen a browser and go to\n\nhttp://" + mIP + ":21000/\n\nThe best experience - charge phone while using MiniDAPPs");
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

    public void setPostSyncDetails(){
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
    public void onBackPressed() {
        moveTaskToBack(true);
    }
}
