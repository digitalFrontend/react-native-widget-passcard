package ru.nasvyazi.widget.passcard.service;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import ru.nasvyazi.widget.passcard.constants.WIDGET_HIGHLIGHTS;
import ru.nasvyazi.widget.passcard.interfaces.IAdvertiseStateChangeCallback;
import ru.nasvyazi.widget.passcard.interfaces.IHighlightCallback;
import ru.nasvyazi.widget.passcard.server.GattServer;
import ru.nasvyazi.widget.passcard.server.entity.GattServerParams;
import ru.nasvyazi.widget.passcard.widget.PasscardWidget;
import ru.nasvyazi.widget.passcard.logger.LogsSender;

public class BackgroundService extends Service {
    private Looper serviceLooper;
    private ServiceHandler serviceHandler;
    private Looper serviceLooperForHighlight;
    private ServiceHandlerForHighlight serviceHandlerForHighlight;

    private GattServer gattServer;
//    private LogsSender logsSender = null;
    private final class ServiceHandler extends Handler {

        private Context mContext;
        public ServiceHandler(Looper looper, Context context) {
            super(looper);
            mContext = context;
        }
        @Override
        public void handleMessage(Message msg) {
            Log.i("PASSCARD_WIDGET", "Service started");
            SharedPreferences sharedPref = mContext.getSharedPreferences("PASSCARD_storage", Context.MODE_PRIVATE);

            GattServerParams serverParams = new GattServerParams();
            serverParams.USER_UUID = sharedPref.getString("USER_UUID", null);
            serverParams.SERVICE_UUID = sharedPref.getString("SERVICE_UUID", null);
            serverParams.CHAR_FOR_READ_UUID = sharedPref.getString("CHAR_FOR_READ_UUID", null);
            serverParams.CHAR_FOR_WRITE_UUID = sharedPref.getString("CHAR_FOR_WRITE_UUID", null);
            serverParams.CHAR_FOR_INDICATE_UUID = sharedPref.getString("CHAR_FOR_INDICATE_UUID", null);
            serverParams.CCC_DESCRIPTOR_UUID = sharedPref.getString("CCC_DESCRIPTOR_UUID", null);
            serverParams.WORK_TIME = sharedPref.getInt("WORK_TIME", 0);
            gattServer.start(serverParams);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                String CHANNEL_ID = "my_channel_01";
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                        "Channel human readable title",
                        NotificationManager.IMPORTANCE_DEFAULT);

                ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

                Notification notification = new NotificationCompat.Builder(mContext, CHANNEL_ID)
                        .setContentTitle("123")
                        .setContentText("321").build();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE);
                } else {
                    startForeground(1, notification);
                }
            }
        }
    }

    private final class ServiceHandlerForHighlight extends Handler {

        private Context mContext;
        private LogsSender logsSender = null;
        public ServiceHandlerForHighlight(Looper looper, Context context) {
            super(looper);
            mContext = context;
            logsSender = new LogsSender(context);
        }
        @Override
        public void handleMessage(Message msg) {
            try {
                SharedPreferences sharedPref = mContext.getSharedPreferences("PASSCARD_storage", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putInt("widgetHighlight", msg.arg1);
                editor.commit();

                Intent intent = new Intent(mContext, PasscardWidget.class);
                intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);

                int[] ids = AppWidgetManager.getInstance(mContext)
                        .getAppWidgetIds(new ComponentName(mContext, PasscardWidget.class));
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
                mContext.sendBroadcast(intent);
                Thread.sleep(2000);
                sharedPref = mContext.getSharedPreferences("PASSCARD_storage", Context.MODE_PRIVATE);
                editor = sharedPref.edit();
                editor.putInt("widgetHighlight", WIDGET_HIGHLIGHTS.NOTHING);
                editor.commit();

                intent = new Intent(mContext, PasscardWidget.class);
                intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);

                ids = AppWidgetManager.getInstance(mContext)
                        .getAppWidgetIds(new ComponentName(mContext, PasscardWidget.class));
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
                mContext.sendBroadcast(intent);
            } catch (InterruptedException e) {
                // Restore interrupt status.
                logsSender.appendLog("CATCH - handleMessage");
                Thread.currentThread().interrupt();
            }
        }
    }


    @Override
    public void onCreate() {
        HandlerThread thread = new HandlerThread("ServiceStartArguments",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
//        logsSender = new LogsSender(this);
        gattServer = new GattServer(this, new IAdvertiseStateChangeCallback() {
            @Override
            public void callback(int state) {
                handleGattServerStateChange(state);
            }
        }, new IHighlightCallback() {
            @Override
            public void callback(int highlight) {
                handleGattServerHighlight(highlight);
            }
        });

        serviceLooper = thread.getLooper();
        serviceHandler = new ServiceHandler(serviceLooper, this);

        HandlerThread threadForHighlight = new HandlerThread("ServiceHighlightThrottler",
                Process.THREAD_PRIORITY_BACKGROUND);
        threadForHighlight.start();

        serviceLooperForHighlight = threadForHighlight.getLooper();
        serviceHandlerForHighlight = new ServiceHandlerForHighlight(serviceLooperForHighlight, this);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("PASSCARD_WIDGET", "Service onStartCommand");

        Message msg = serviceHandler.obtainMessage();
        msg.arg1 = startId;
        serviceHandler.sendMessage(msg);

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        gattServer.stop();
        Log.i("PASSCARD_WIDGET", "Service destroyed");
    }

    private void handleGattServerStateChange(int state){
        SharedPreferences sharedPref = this.getSharedPreferences("PASSCARD_storage", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt("widgetState", state);
        editor.commit();

        Intent intent = new Intent(this, PasscardWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);

        int[] ids = AppWidgetManager.getInstance(this)
                .getAppWidgetIds(new ComponentName(this, PasscardWidget.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        this.sendBroadcast(intent);
    }

    private void handleGattServerHighlight(int highlight){
        Message msg = serviceHandlerForHighlight.obtainMessage();
        msg.arg1 = highlight;
        serviceHandlerForHighlight.sendMessage(msg);
    }
}