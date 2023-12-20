package ru.nasvyazi.widget.passcard.service;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BackgroundServiceRunner {

    public static boolean isMyServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static void StartService(Context context) throws Exception {
        if (isMyServiceRunning(context,BackgroundService.class)){
            throw new Exception("Already running");
        } else {
            context.startService(new Intent(context, BackgroundService.class));
        }
    }

    public static void StopService(Context context) {
        context.stopService(new Intent(context, BackgroundService.class));
    }
}
