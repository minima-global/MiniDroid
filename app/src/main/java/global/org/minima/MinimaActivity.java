package global.org.minima;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Browser;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import org.minima.GlobalParams;
import org.minima.system.brains.ConsensusHandler;
import org.minima.utils.MiniFile;
import org.minima.utils.MiniFormat;
import org.minima.utils.MinimaLogger;
import org.minima.utils.messages.Message;
import org.minima.utils.messages.MessageListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;

import com.minima.service.MinimaService;

import global.org.minima.intro.IntroductionActivity;

public class MinimaActivity extends AppCompatActivity implements ServiceConnection, MessageListener {

    Button btnMini;

    //The IP Text..
    TextView mTextIP;

    //The IP of this device
    String mIP;

    //The Service..
    MinimaService mMinima = null;

    boolean mSynced = false;

    boolean mBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(global.org.minima.R.layout.activity_main);

        //The Button to open the local browser
        btnMini = findViewById(global.org.minima.R.id.btn_minidapp);
        btnMini.setTransformationMethod(null);
        btnMini.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://127.0.0.1:9004/"));
                intent.putExtra(Browser.EXTRA_APPLICATION_ID, MinimaActivity.this.getPackageName());
                startActivity(intent);
            }
        });
        btnMini.setVisibility(View.GONE);

        //The TEXT that shows the current IP
        mTextIP = findViewById(R.id.iptext_minidapp);
        mTextIP.setText("\nSynchronising.. please wait..");

        //start Minima node Foreground Service
        Intent minimaintent = new Intent(getBaseContext(), MinimaService.class);
        startForegroundService(minimaintent);
        bindService(minimaintent, this, Context.BIND_AUTO_CREATE);

        //Do we do the intro..
        SharedPreferences pref = getApplicationContext().getSharedPreferences("MinimaPref", 0); // 0 - for private mode

        //Introduction
        boolean doIntro = pref.getBoolean("intro",true);
        if(doIntro){
            SharedPreferences.Editor editor = pref.edit();
            editor.putBoolean("intro",false);
            editor.commit();

            //Start the intro activity..
            Intent intro = new Intent(this, IntroductionActivity.class);
            startActivity(intro);
        }

        //Make sure..
//        requestBatteryCheck();
    }

    /**
     * Show ameesgae requesting access to battery settings
     */
    public void requestBatteryCheck(){
        String packageName = getPackageName();
        PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            new AlertDialog.Builder(this)
                    .setTitle("Battery Optimise")
                    .setMessage("Minima needs to run in the background to validate and secure your coins.\n\n" +
                            "You can see this setting in your options menu in the top right.")
                    // Specifying a listener allows you to take an action before dismissing the dialog.
                    // The dialog is automatically dismissed when a dialog button is clicked.
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // Continue with delete operation
                            checkBatteryOptimisation();
                        }
                    })
                    // A null listener allows the button to dismiss the dialog and take no further action.
//                    .setNegativeButton(android.R.string.no, null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }
    }

    public void checkBatteryOptimisation(){
        Intent intent = new Intent();
        String packageName = getPackageName();
        PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        if (!pm.isIgnoringBatteryOptimizations(packageName)){
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + packageName));
            startActivity(intent);
        }
    }

    public void openBatteryOptimisation(){
        Intent intent = new Intent();
//        String packageName = getPackageName();
        PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        //if (pm.isIgnoringBatteryOptimizations(packageName))
            intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
//        else {
//            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
//            intent.setData(Uri.parse("package:" + packageName));
//        }

        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.titleoptions, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.minimahelp:
                Toast.makeText(this,"HELP! I NEED SOMEBODY!",Toast.LENGTH_SHORT).show();
                return true;

            case R.id.minimalogs:
                Intent intent = new Intent(MinimaActivity.this, MinimaLogs.class);
                startActivity(intent);
                return true;

            case R.id.backup:
                //First check we are connected..
                if(!mSynced){
                    Toast.makeText(this,"Waiting to connect to Minima..",Toast.LENGTH_LONG).show();
                    return true;
                }

                //Get a timeStamp..
                SimpleDateFormat s = new SimpleDateFormat("dd_MM_yyyy_hhmmss");
                String format = s.format(new Date());

                //Get the file location..
                File backup = new File(getFilesDir(),"backup-"+format+".minima");

                //Run a function..
                Toast.makeText(this,"Minima Backup to "+backup.getAbsolutePath(),Toast.LENGTH_LONG).show();
                backupMinima(backup.getAbsolutePath());
                return true;

            case R.id.restore:
                //First check we are connected..
                if(!mSynced){
                    Toast.makeText(this,"Waiting to connect to Minima..",Toast.LENGTH_LONG).show();
                    return true;
                }

                //Open a file chooser app
                Intent filechoose = new Intent().setType("*/*")
                        .setAction(Intent.ACTION_GET_CONTENT);

                startActivityForResult(Intent.createChooser(filechoose, "Select a file"), 123);

                return true;

//            case R.id.shareapp:
//                //Create a link and share that..
//                String link = "http://mifi.minima.global/apk/minima-latest.apk";
//
//                //Now share it
//                Intent sendIntent = new Intent();
//                sendIntent.setAction(Intent.ACTION_SEND);
//                sendIntent.putExtra(Intent.EXTRA_TEXT, link);
//                sendIntent.setType("text/plain");
//
//                Intent shareIntent = Intent.createChooser(sendIntent, null);
//                startActivity(shareIntent);
//
//                return true;
            case R.id.reset:
                //Reset the whole thing..
                new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle("Reset Minima").setMessage("Are you sure you want to RESET Minima ?\n\nThis will wipe all information..")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Toast.makeText(MinimaActivity.this, "Resetting.. Please wait",Toast.LENGTH_LONG).show();

                                //Get the shared prefs..
                                SharedPreferences pref = getApplicationContext().getSharedPreferences("MinimaPref", 0); // 0 - for private mode

                                //Permanent store
                                SharedPreferences.Editor editor = pref.edit();
                                editor.putBoolean("minidapps",false);
                                editor.commit();

                                //Now reset
                                resetMinima();

                            }
                        }).setNegativeButton("No", null).show();

                return true;

            case R.id.battery:
                openBatteryOptimisation();
                return true;

//            case R.id.shutdown:
//                shutdownMinima();
//                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 123 && resultCode == RESULT_OK) {
            Uri selectedfile = data.getData(); //The uri with the location of the file

            Toast.makeText(this,"Restoring from Backup!",Toast.LENGTH_LONG).show();

            restore(selectedfile);
        }
    }

    public void backupMinima(final String zFileBackup) {
        Thread backup = new Thread(new Runnable() {
            @Override
            public void run() {
                //First run the function
                String resp = mMinima.getMinima().runMinimaCMD("backup "+zFileBackup);

                //Get the FileURI
                Uri fileUri = FileProvider.getUriForFile(
                        getApplicationContext(),
                        "global.org.minima.fileprovider",
                        new File(zFileBackup));

                //System.out.println("BACKUP FILE : "+fileUri.toString());
                toastPopUp(fileUri.toString());

                //Now share it
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_STREAM,fileUri);
                sendIntent.setType("application/octet-stream");

                Intent shareIntent = Intent.createChooser(sendIntent, null);
                startActivity(shareIntent);
            }
        });
        backup.start();
    }

    public void restore(final Uri zFileBackup) {
        Thread restorer = new Thread(new Runnable() {
            @Override
            public void run() {
                //First copy the whole file..
                File restore = new File(getFilesDir(),"restore.minima");
                if(restore.exists()){
                    restore.delete();
                }

                //Now copy ..
                try{
                    //Get the Stream
                    InputStream is = getContentResolver().openInputStream(zFileBackup);

                    //Create the File.
                    restore.createNewFile();
                    FileOutputStream fos = new FileOutputStream(restore);

                    byte[] copy = new byte[4096];
                    int read=0;
                    while(read != -1){
                        read = is.read(copy);
                        if(read>=0){
                            //Write it out..
                            fos.write(copy,0,read);
                        }
                    }
                    fos.flush();
                    fos.close();

                    mSynced = false;
                    setPercentInitial("Restoring Minima.. please wait..");
                    MinimaLogger.log("Restore File : "+restore.getAbsolutePath()+" "+restore.length());

                    //Now do the restore..
                    String resp = mMinima.getMinima().runMinimaCMD("restore "+restore.getAbsolutePath());
                    MinimaLogger.log(resp);

                }catch(Exception exc){
                    MinimaLogger.log("ERROR Restore : "+exc);
                }
            }
        });
        restorer.start();
    }

    public void resetMinima(){
        Thread resetter = new Thread(new Runnable() {
            @Override
            public void run() {
                mSynced = false;

                setPercentInitial("Resetting Minima..");

                //RUn this.. can take some time..
                mMinima.getMinima().runMinimaCMD("reset");
            }
        });
        resetter.start();
    }

    public void shutdownMinima() {

        Thread shutdown = new Thread(new Runnable() {
            @Override
            public void run() {
                //Little message
                toastPopUp("Disconnecting from Minima..");

                //Update..
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        btnMini.setVisibility(View.GONE);
                        mTextIP.setText("\nShutting down.. Please wait..");
                    }
                });

                if(mMinima != null){
                    try{
                        //First run the function
                        String resp = mMinima.getMinima().runMinimaCMD("quit");
                        MinimaLogger.log(resp);

                        //Now stop listening
                        mMinima.getMinima().getServer().getConsensusHandler().removeListener(MinimaActivity.this);
                    }catch(Exception exc){}
                }

//                //Wait a few seconds..
//                try {Thread.sleep(5000);} catch (InterruptedException e) {}
//                toastPopUp("Stopping Service..");

                Intent minimaintent = new Intent(getBaseContext(), MinimaService.class);
                stopService(minimaintent);

                //Wait a few seconds..
                try {Thread.sleep(5000);} catch (InterruptedException e) {}

                toastPopUp("Minima Shutdown Complete..");

                try {Thread.sleep(2000);} catch (InterruptedException e) {}

                //Close it down..
                MinimaActivity.this.finish();
                System.exit(0);
            }
        });

        shutdown.start();

    }

//    public void restartMinima(){
//        //Hide the start Button..
//        btnMini.setVisibility(View.GONE);
//        mTextIP.setText("\nRestarting Minima.. please wait..");
//
//        Thread restart = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                mSynced = false;
//
//                toastPopUp("Disconnecting from Minima..");
//                //Disconnect..
//                disconnectFromService();
//
//                //Wait a few seconds..
//                try {Thread.sleep(10000);} catch (InterruptedException e) {}
//
//                toastPopUp("Stopping Service..");
//                //First stop the service,,
//                Intent minimaintent = new Intent(getBaseContext(), MinimaService.class);
//                stopService(minimaintent);
//
//                //Wait a few seconds..
//                try {Thread.sleep(10000);} catch (InterruptedException e) {}
//
//                //And then restart it..
//                toastPopUp("Re-Starting Service");
//                Intent minimaintentstarter = new Intent(getBaseContext(), MinimaService.class);
//                startForegroundService(minimaintentstarter);
//
//                //Wait a few seconds..
//                try {Thread.sleep(10000);} catch (InterruptedException e) {}
//
//                toastPopUp("Re-Binding to Minima..");
//                bindService(minimaintent, MinimaActivity.this, Context.BIND_AUTO_CREATE);
//            }
//        });
//        restart.start();
//    }

//    @Override
//    protected void onStart() {
//        super.onStart();
//    }
//
//    @Override
//    protected void onStop() {
//        super.onStop();
//        MinimaLogger.log("Activity : onStop");
//    }

    @Override
    protected void onResume() {
        super.onResume();
        //MinimaLogger.log("Activity : onResume");
        setIPText();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //Remove the message listener.. don;t want to clog it up..
        disconnectFromService();
    }

    private void disconnectFromService(){
        MinimaLogger.log("TRY DISCONNECT SERVICE");

        if(mMinima != null){
            try{
                mMinima.getMinima().getServer().getConsensusHandler().removeListener(this);
            }catch(Exception exc){}
        }

        if(mBound){
            unbindService(this);
        }
    }

    public void setIPText() {
        //Set the IP - it may change..
        if (mSynced) {
            mIP = getIP();
            mTextIP.setText("\nConnect to Minima v"+ GlobalParams.MINIMA_VERSION +" from your Desktop\n\n" +
                    "Open a browser and go to\n\n" +
                    "http://" + mIP + ":9004/");
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

    public void toastPopUp(final String zText){
        Runnable littletoast = new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MinimaActivity.this, zText, Toast.LENGTH_LONG).show();
            }
        };

        //Update..
        runOnUiThread(littletoast);
    }

    public void setPostSyncDetails(){
        Runnable uiupdate = new Runnable() {
            @Override
            public void run() {
                mSynced = true;
                btnMini.setVisibility(View.VISIBLE);
                setIPText();
                MinimaLogger.log("ACTIVITY : GET STARTED");
            }
        };

        //Update..
        runOnUiThread(uiupdate);
    }

    public void setPercentInitial(final String zMessage){
        if(!mSynced) {
            Runnable uiupdate = new Runnable() {
                @Override
                public void run() {
                    mTextIP.setText("\n" + zMessage);
                }
            };

            //Update..
            runOnUiThread(uiupdate);
        }
    }

//    @Override
//    public void onBackPressed() {
//        moveTaskToBack(true);
//    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        MinimaLogger.log("CONNECTED TO SERVICE");
        MinimaService.MyBinder binder = (MinimaService.MyBinder)iBinder;
        mMinima = binder.getService();

        mBound = true;

        Thread addlistener = new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    while(mMinima.getMinima() == null){Thread.sleep(250);}
                    while(mMinima.getMinima().getServer() == null){Thread.sleep(250);}
                    while(mMinima.getMinima().getServer().getConsensusHandler() == null){Thread.sleep(250);}

                    //ready..
                    if(mMinima.getMinima().getServer().getConsensusHandler().isInitialSyncComplete()){
                        MinimaLogger.log("ACTIVITY : INITIAL SYNC COMPLETE ON STARTUP..");
                        setPostSyncDetails();

                    }else{
                        //Listen for messages..
                        mMinima.getMinima().getServer().getConsensusHandler().addListener(MinimaActivity.this);

                        MinimaLogger.log("ACTIVITY : LISTENING FOR SYNC COMPLETE..");
                    }
                }catch(Exception exc){

                }
            }
        });
        addlistener.start();
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        MinimaLogger.log("DISCONNECTED TO SERVICE");
        mBound = false;
    }

    @Override
    public void processMessage(Message zMessage) {
        if (zMessage.isMessageType(ConsensusHandler.CONSENSUS_NOTIFY_INITIALSYNC)) {
            MinimaLogger.log("ACTIVITY SYNC COMPLETE : " + zMessage);

            //Set the correct view..
            setPostSyncDetails();

//            setPercentInitial("Almost Done.. Checking MiniDAPPs");

        }else if (zMessage.isMessageType(ConsensusHandler.CONSENSUS_NOTIFY_RECONNECT)) {
            //Reconnecting to network.. notify user..
            mSynced = false;
            setPercentInitial("Reconnecting in 30 seconds..");

        }else if (zMessage.isMessageType(ConsensusHandler.CONSENSUS_NOTIFY_INITIALPERC)) {
            setPercentInitial(zMessage.getString("info"));

        }else if (zMessage.isMessageType(ConsensusHandler.CONSENSUS_NOTIFY_DAPP_RELOAD)) {
            MinimaLogger.log("ACTIVITY : DAPPS LOADED");

            //Set the correct view..
            setPostSyncDetails();

        }else if (zMessage.isMessageType(ConsensusHandler.CONSENSUS_NOTIFY_DAPP_INSTALLED)) {
            String dapp = zMessage.getString("name");

            MinimaLogger.log("ACTIVITY : DAPP INSTALLED "+dapp);

            setPercentInitial("MiniDAPP installed : "+dapp);

            //Set the correct view..
            setPostSyncDetails();

        }else if (zMessage.isMessageType(ConsensusHandler.CONSENSUS_NOTIFY_LOG)) {
            String log = zMessage.getString("msg");
        }
    }
}
