package android.service;

import android.content.Context;
import android.content.Intent;

import androidx.core.app.JobIntentService;

public class ServiceStarterJobService extends JobIntentService {

    public static final int JOB_ID = 0x01;

    public static void enqueueWork(Context context, Intent work) {
        // your code
        //MinimaLogger.log("MINIMA ENQUEUE");

       enqueueWork(context, ServiceStarterJobService.class, JOB_ID, work);
    }

    @Override
    protected void onHandleWork(Intent intent) {
        // your code
        //MinimaLogger.log("MINIMA HANDLEWORK");

        //Start the Main Service..
        Intent serviceintent = new Intent(getBaseContext(), MinimaService.class);
        startForegroundService(serviceintent);
    }

}
