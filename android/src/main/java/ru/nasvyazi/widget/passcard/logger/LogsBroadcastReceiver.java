package ru.nasvyazi.widget.passcard.logger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import ru.nasvyazi.widget.passcard.interfaces.ILogReceive;

public class LogsBroadcastReceiver extends BroadcastReceiver {

    public static String ON_LOGS_INTENT = "ru.nasvyazi.widget.passcard.ON_LOGS_INTENT";
    public static String LOGS_PARAM = "ru.nasvyazi.widget.passcard.LOGS_PARAM";
    private ILogReceive mCompletion;

    public LogsBroadcastReceiver(Context context, ILogReceive completion){
        mCompletion = completion;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == ON_LOGS_INTENT){
            String log = intent.getExtras().getString(LOGS_PARAM, null);
            if (mCompletion != null){
                mCompletion.callback(log);
            }
        }
    }
}
