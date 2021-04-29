package global.org.minima;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.IBinder;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.minima.system.brains.ConsensusHandler;
import org.minima.utils.MinimaLogger;
import org.minima.utils.messages.Message;
import org.minima.utils.messages.MessageListener;

import com.minima.service.MinimaService;

public class MinimaLogs extends AppCompatActivity implements ServiceConnection, MessageListener {

    //The Service..
    MinimaService mMinima = null;

    TextView mText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(global.org.minima.R.layout.activity_logs);

        setTitle("Minima Logs");

        mText = (TextView)findViewById(R.id.logstext);
        mText.setTypeface(Typeface.MONOSPACE);

        //Connect tp the service..
        Intent minimaintent = new Intent(getBaseContext(), MinimaService.class);
        bindService(minimaintent, this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.logoptions, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.share:
                //Get the text
                String text = mText.getText().toString();

                //Create an intent
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Minima Logs");
                sendIntent.putExtra(Intent.EXTRA_TEXT, text);
                sendIntent.setType("text/plain");

                //Fire it off..
                Intent shareIntent = Intent.createChooser(sendIntent, "Shareing Options");
                startActivity(shareIntent);

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //Remove the message listener.. don;t want to clog it up..
        if(mMinima != null){
            mMinima.getMinima().getServer().getConsensusHandler().removeListener(this);

            //Clean Unbind
            unbindService(this);
        }
    }

        @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        MinimaService.MyBinder binder = (MinimaService.MyBinder)iBinder;

        //Get Minima..
        mMinima = binder.getService();

        //Set the whole output
        addText("",true);

        //Add a listener..
        mMinima.getMinima().getServer().getConsensusHandler().addListener(this);
    }

    private void addText(final String zText, final boolean zFullOutput){
        Runnable adder = new Runnable() {
            @Override
            public void run() {
                String text = zText;
                if(zFullOutput){
                    text = MinimaLogger.getFullOutput();
                }

                //Add return
                if(!text.endsWith("\n")){
                    text = text+"\n";
                }

                //And add the new..
                mText.append(text);

                //How big is it..
                String current = mText.getText().toString();

                //Check the Length
                int len = current.length();
                if(len>150000){
                    //Resize in order
                    String tt = current.substring(len-140000,len);

                    //And now set it
                    mText.setText("...(cropped)\n"+tt);
                }
            }
        };

        runOnUiThread(adder);
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) { }

    @Override
    public void processMessage(Message zMessage) {
        //Get the LOG messages..
        if(zMessage.isMessageType(ConsensusHandler.CONSENSUS_NOTIFY_LOG)){
            //Add it to the text view..
            addText(zMessage.getString("msg"), false);
        }
    }
}
