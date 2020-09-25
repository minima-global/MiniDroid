package global.org.minima;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.IBinder;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;

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

        mText = (TextView)findViewById(R.id.logstext);
        mText.setTypeface(Typeface.MONOSPACE);
        mText.setMovementMethod(new ScrollingMovementMethod());

        //Connect tp the service..
        Intent minimaintent = new Intent(getBaseContext(), MinimaService.class);
        bindService(minimaintent, this, Context.BIND_AUTO_CREATE);
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

        //Get the Text..
        mText.setText(MinimaLogger.getFullOutput());

        //Add a listener..
        mMinima.getMinima().getServer().getConsensusHandler().addListener(this);
    }

    private void addText(final String zText){
        Runnable adder = new Runnable() {
            @Override
            public void run() {
                if(zText.endsWith("\n")){
                    mText.append(zText);
                }else{
                    mText.append(zText+"\n");
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
            addText(zMessage.getString("msg"));
        }
    }
}
