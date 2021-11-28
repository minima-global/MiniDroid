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
import android.provider.Settings;
import android.text.Html;
import android.text.InputType;
import android.text.Spanned;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

//import org.minima.GlobalParams;
//import org.minima.system.brains.ConsensusHandler;

import com.google.android.material.tabs.TabLayout;
import com.jraska.console.Console;
import com.minima.service.MinimaService;

import org.minima.Minima;
import org.minima.utils.MinimaLogger;
import org.minima.utils.json.JSONObject;
import org.minima.utils.json.parser.JSONParser;
import org.minima.utils.json.parser.ParseException;
import org.minima.utils.messages.Message;
import org.minima.utils.messages.MessageListener;

import global.org.minima.intro.IntroductionActivity;

public class MinimaActivity extends AppCompatActivity implements ServiceConnection {

    //The Service..
    MinimaService mMinima = null;

    //The Main View pager
    ViewPager mPager;

    //The main pages
    MainViewAdapter mMainPages;

    //The IC User
//    String mICUser = "";

    boolean mShowPin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_viewpager);

        //Create a new ViewPager adapter
        mMainPages = new MainViewAdapter(this);

        //Get the viewpager
        mPager = (ViewPager)findViewById(R.id.intro_viewpager);
        mPager.setAdapter(mMainPages);
        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            @Override
            public void onPageSelected(int position) {
                switch (position){
                    case 0 :
                        MinimaActivity.this.setTitle("Minima");
                    break;
                    case 1 :
                        MinimaActivity.this.setTitle("Incentive Cash");
                        break;
                    case 2 :
                        MinimaActivity.this.setTitle("News");
                        break;
                    case 3 :
                        MinimaActivity.this.setTitle("Console");
                        break;
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {}
        });

        //Refresh the News feed
        mMainPages.refreshRSSFeed();

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabDots);
        tabLayout.setupWithViewPager(mPager, true);

        //Start Minima node Foreground Service
        Intent minimaintent = new Intent(getBaseContext(), MinimaService.class);
        startForegroundService(minimaintent);
        bindService(minimaintent, this, Context.BIND_AUTO_CREATE);

        //Do we do the intro..
        SharedPreferences pref = getApplicationContext().getSharedPreferences("MinimaPref", 0); // 0 - for private mode

//        //Get the IC USer
//        mICUser = pref.getString("icuser","");

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

        mShowPin = true;
    }

    /**
     * Show a messgae requesting access to battery settings
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

                Spanned text = Html.fromHtml("<br>Please visit <b>minima.global</b><br><br>Thank you",Html.FROM_HTML_MODE_LEGACY);

                new AlertDialog.Builder(this)
                        .setTitle("Help")
                        .setMessage(text)
                        .setIcon(R.drawable.ic_minima_new)
                        .show();

//                Toast.makeText(this,"HELP! I NEED SOMEBODY!",Toast.LENGTH_SHORT).show();
                return true;

           case R.id.incentive:

                //Start a web browser
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://incentivecash.minima.global/"));
                startActivity(browserIntent);

                return true;

            case R.id.refreshNews:

                //refresh the News..
                mMainPages.refreshRSSFeed();

                //Small notification
                Toast.makeText(this,"Refreshing News..",Toast.LENGTH_SHORT).show();

                return true;

            case R.id.clearconsole:
                Console.clear();

                //Small notification
                Toast.makeText(this,"Console cleared..",Toast.LENGTH_SHORT).show();

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

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if(requestCode == 123 && resultCode == RESULT_OK) {
//            Uri selectedfile = data.getData(); //The uri with the location of the file
//
//            Toast.makeText(this,"Restoring from Backup!",Toast.LENGTH_LONG).show();
//
//            restore(selectedfile);
//        }
//    }
//
//    public void backupMinima(final String zFileBackup) {
//        Thread backup = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                //First run the function
//                String resp = mMinima.getMinima().runMinimaCMD("backup "+zFileBackup);
//
//                //Get the FileURI
//                Uri fileUri = FileProvider.getUriForFile(
//                        getApplicationContext(),
//                        "global.org.minima.fileprovider",
//                        new File(zFileBackup));
//
//                //System.out.println("BACKUP FILE : "+fileUri.toString());
//                toastPopUp(fileUri.toString());
//
//                //Now share it
//                Intent sendIntent = new Intent();
//                sendIntent.setAction(Intent.ACTION_SEND);
//                sendIntent.putExtra(Intent.EXTRA_STREAM,fileUri);
//                sendIntent.setType("application/octet-stream");
//
//                Intent shareIntent = Intent.createChooser(sendIntent, null);
//                startActivity(shareIntent);
//            }
//        });
//        backup.start();
//    }
//
//    public void restore(final Uri zFileBackup) {
//        Thread restorer = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                //First copy the whole file..
//                File restore = new File(getFilesDir(),"restore.minima");
//                if(restore.exists()){
//                    restore.delete();
//                }
//
//                //Now copy ..
//                try{
//                    //Get the Stream
//                    InputStream is = getContentResolver().openInputStream(zFileBackup);
//
//                    //Create the File.
//                    restore.createNewFile();
//                    FileOutputStream fos = new FileOutputStream(restore);
//
//                    byte[] copy = new byte[4096];
//                    int read=0;
//                    while(read != -1){
//                        read = is.read(copy);
//                        if(read>=0){
//                            //Write it out..
//                            fos.write(copy,0,read);
//                        }
//                    }
//                    fos.flush();
//                    fos.close();
//
//                    mSynced = false;
//                    setPercentInitial("Restoring Minima.. please wait..");
//                    MinimaLogger.log("Restore File : "+restore.getAbsolutePath()+" "+restore.length());
//
//                    //Now do the restore..
//                    String resp = mMinima.getMinima().runMinimaCMD("restore "+restore.getAbsolutePath());
//                    MinimaLogger.log(resp);
//
//                }catch(Exception exc){
//                    MinimaLogger.log("ERROR Restore : "+exc);
//                }
//            }
//        });
//        restorer.start();
//    }
//
//    public void resetMinima(){
//        Thread resetter = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                mSynced = false;
//
//                setPercentInitial("Resetting Minima..");
//
//                //RUn this.. can take some time..
//                mMinima.getMinima().runMinimaCMD("reset");
//            }
//        });
//        resetter.start();
//    }
//
//    public void shutdownMinima() {
//
//        Thread shutdown = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                //Little message
//                toastPopUp("Disconnecting from Minima..");
//
//                //Update..
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        btnMini.setVisibility(View.GONE);
//                        mTextIP.setText("\nShutting down.. Please wait..");
//                    }
//                });
//
//                if(mMinima != null){
//                    try{
//                        //First run the function
//                        String resp = mMinima.getMinima().runMinimaCMD("quit");
//                        MinimaLogger.log(resp);
//
//                        //Now stop listening
//                        mMinima.getMinima().getServer().getConsensusHandler().removeListener(MinimaActivity.this);
//                    }catch(Exception exc){}
//                }
//
////                //Wait a few seconds..
////                try {Thread.sleep(5000);} catch (InterruptedException e) {}
////                toastPopUp("Stopping Service..");
//
//                Intent minimaintent = new Intent(getBaseContext(), MinimaService.class);
//                stopService(minimaintent);
//
//                //Wait a few seconds..
//                try {Thread.sleep(5000);} catch (InterruptedException e) {}
//
//                toastPopUp("Minima Shutdown Complete..");
//
//                try {Thread.sleep(2000);} catch (InterruptedException e) {}
//
//                //Close it down..
//                MinimaActivity.this.finish();
//                System.exit(0);
//            }
//        });
//
//        shutdown.start();
//
//    }

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

    @Override
    protected void onResume() {
        super.onResume();

        //Show the PIN activity..

    }

        @Override
    protected void onDestroy() {
        super.onDestroy();

        //Unbind from the service..
        if(mMinima != null){
            unbindService(this);
        }
    }

    public String runCommandSync(String zCommand){
        if(mMinima != null){
            return mMinima.getMinima().runMinimaCMD(zCommand);
        }

        return "Minima not connected..";
    }

    public void runMinimaCommand(final String zCommand){
        if(mMinima!=null){

            Runnable cmd = new Runnable() {
                @Override
                public void run() {

                    //Run a command..
                    String resp = mMinima.getMinima().runMinimaCMD(zCommand);

                    //Put it in the Console..
                    Console.writeLine(zCommand);
                    Console.writeLine(resp);
                }
            };

            Thread tt = new Thread(cmd);
            tt.start();
        }
    }

    public void runICCommand(String zCommand) {
        if(mMinima!=null){

            Runnable cmd = new Runnable() {
                @Override
                public void run() {

                    //Run a command..
                    String resp = mMinima.getMinima().runMinimaCMD(zCommand);
                    resp = resp.replaceAll("\n","");

                    try {
                        JSONObject json    = (JSONObject) new JSONParser().parse(resp);
                        JSONObject reply   = (JSONObject)json.get("response");
                        JSONObject details = (JSONObject)reply.get("details");
                        JSONObject rewards = (JSONObject)details.get("rewards");

                        long daily   = (long)rewards.get("dailyRewards");
                        double prev  = (double)rewards.get("previousRewards");
                        String lastping = (String)details.get("lastPing");

                        //Calculate the total
                        double totalrewards = daily+prev;

                        //Set the output in the IC Window..
//                        mMainPages.updateICData("Previous:"+prev+" Daily:"+daily,lastping);
                        mMainPages.updateICData(totalrewards+" Minima",lastping);

                    } catch (Exception e) {
                        mMainPages.updateICData("Error - No User found","..");
                       e.printStackTrace();
                    }

                }
            };

            Thread tt = new Thread(cmd);
            tt.start();
        }
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        //MinimaLogger.log("CONNECTED TO SERVICE");
        MinimaService.MyBinder binder = (MinimaService.MyBinder)iBinder;
        mMinima = binder.getService();
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        //MinimaLogger.log("DISCONNECTED TO SERVICE");
       mMinima = null;
    }
}
