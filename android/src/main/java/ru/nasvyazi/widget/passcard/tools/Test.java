package ru.nasvyazi.widget.passcard.tools;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import ru.nasvyazi.widget.passcard.logger.LogsSender;

public class Test {

    private boolean isWork = false;
    private LogsSender logsSender;

    public void start(Context context) {

        isWork = true;
        logsSender = new LogsSender(context);
        recursive(context);

    }

    private void recursive(Context context){
        Handler handler = new Handler();

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Date currentTime = Calendar.getInstance().getTime();
                DateFormat dateFormat = new SimpleDateFormat("y-M-d H:m:s.S");
                String log = "Time: " + dateFormat.format(currentTime);
                logsSender.appendLog(log);

                Log.i("PASSCARD_WIDGET", log);
                if (isWork){
                    recursive(context);
                }
            }
        };

        // Post the Runnable with a delay
        handler.postDelayed(runnable, 3000);
    }

    public void stop(Context context){
        isWork = false;
    }
}
