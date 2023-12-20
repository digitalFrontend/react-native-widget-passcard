package ru.nasvyazi.widget.passcard.logger;

import android.content.Context;
import android.content.Intent;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class LogsSender {
    private Context mContext;
    public LogsSender(Context context){
        mContext = context;
    }

    public void appendLog(String log){
        String date = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());

        Intent intent = new Intent(LogsBroadcastReceiver.ON_LOGS_INTENT);
        intent.putExtra(LogsBroadcastReceiver.LOGS_PARAM, date + ": " + log);
        mContext.sendBroadcast(intent);
    }
}
